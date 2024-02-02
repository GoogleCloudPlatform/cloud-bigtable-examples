# Copyright 2023 Google Inc.

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import json
import logging
import os
import random
import string
import struct
import threading
import time
from typing import Iterator

import pytest
from google.cloud import aiplatform_v1beta1, bigtable, workflows_v1
from google.cloud.bigtable import column_family
from google.cloud.workflows import executions_v1
from google.cloud.workflows.executions_v1 import Execution

# Configure Variables
BIGTABLE_TABLE_NAME = "test_bigtable_vertex_integration_" + str(
    random.randint(10000, 99999)
)
WORKFLOW_NAME = "test-bigtable-vvi-" + str(random.randint(10000, 99999))
WORKFLOW_LOCATION = "us-central1"
VERTEX_VECTOR_SEARCH_INDEX_ENDPOINT = (
    "1597640674.us-central1-818418350420.vdb.vertexai.goog"
)
VERTEX_VECTOR_SEARCH_INDEX = "5500764314786594816"

# Get the directory where this test file is located
THIS_FILE_DIRECTORY = os.path.dirname(os.path.abspath(__file__))
WORKFLOW_INPUT_FILE_PATH = THIS_FILE_DIRECTORY + "/workflow-input.json"
WORKFLOW_YAML_FILE_PATH = (
    os.path.dirname(os.path.dirname(THIS_FILE_DIRECTORY))
    + "/workflows/batch-export.yaml"
)


# Define a custom log format
log_format = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
log_datefmt = "%Y-%m-%d %H:%M:%S"

# Create a logger instance
logger = logging.getLogger(__name__)

# Set the log level to capture all log messages
logger.setLevel(level=logging.DEBUG)

# Create a handler for console output
console_handler = logging.StreamHandler()
console_handler.setFormatter(logging.Formatter(log_format, log_datefmt))

# Add the console handler and file handler to the logger
logger.addHandler(console_handler)


@pytest.fixture(scope="module")
def project_id() -> Iterator[str]:
    yield get_env_var("PROJECT_ID", "ID of the Cloud Bigtable project")


@pytest.fixture(scope="module")
def instance_id() -> Iterator[str]:
    yield get_env_var("INSTANCE_ID", "ID of the Cloud Bigtable instance")


@pytest.fixture(scope="module")
def setup_workflow(project_id: str):
    # Deploy Workflow
    deploy_workflow(project_id, WORKFLOW_LOCATION, WORKFLOW_NAME)

    yield

    workflow_client = workflows_v1.WorkflowsClient()

    workflow_full_path = (
        "projects/{project}/locations/{location}/workflows/{workflow_name}".format(
            project=project_id, location=WORKFLOW_LOCATION, workflow_name=WORKFLOW_NAME
        )
    )

    # Initialize request argument(s)
    request = workflows_v1.DeleteWorkflowRequest(
        name=workflow_full_path,
    )

    # Make the request
    operation = workflow_client.delete_workflow(request=request)

    logger.info("Delete Cloud Workflow with name: {}.".format(workflow_full_path))

    operation.result()


def generate_vector_data(number_of_rows, vector_dimension, table):
    """Generates vector data for Bigtable table.

    Args:
        number_of_rows: The number of rows to generate.
        vector_dimension: The dimension of the vectors.

    Returns:
        A list of rows, each of which is a tuple of (id, text, embeddings, restricts).
    """

    logger.info(
        "Generating {} vector embeddings each of dimension: {}.".format(
            number_of_rows, vector_dimension
        )
    )

    rows_dict = dict()
    rows = []

    for i in range(number_of_rows):
        # Generating random vector embeddings
        embeddings = [random.uniform(0, 1) for _ in range(vector_dimension)]

        rowEntry = {
            "row_key": f"row_key_{i}",
            "embeddings": embeddings,
            "allow": "thing 1",
            "deny": "thing 2",
            "int": 45000,
            "float": 3.14,
            "double": 2.71,
            "crowding_tag": "a" if i % 2 == 0 else "b",
        }

        row = table.direct_row(str.encode(rowEntry["row_key"]))
        row.set_cell(
            "cf",
            "embeddings",
            struct.pack(
                ">" + "f" * len(rowEntry["embeddings"]), *rowEntry["embeddings"]
            ),
        )

        # Restricts
        row.set_cell("cf", "allow", str.encode(rowEntry["allow"]))
        row.set_cell("cf", "deny", str.encode(rowEntry["deny"]))
        row.set_cell("cf", "int", rowEntry["int"].to_bytes(4, "big"))
        row.set_cell("cf", "float", struct.pack(">f", rowEntry["float"]))
        row.set_cell("cf", "double", struct.pack(">d", rowEntry["double"]))

        # Crowding tag
        row.set_cell("cf", "crowding_tag", str.encode(rowEntry["crowding_tag"]))

        rows_dict[rowEntry["row_key"]] = rowEntry
        rows.append(row)

    logger.info("Vector Embeddings generated.")

    return rows, rows_dict


def setup_bigtable(project_id, instance_id, table_name):
    """Sets up a Bigtable table with vector embeddings.

    Args:
        project_id: The project ID.
        instance_id: The instance ID.
        table_name: The table name.
    """

    logger.info("Setting up Bigtable Table...")

    NUMBER_OF_ROWS_IN_BIGTABLE = 1000
    VECTOR_DIMENSION = 128

    client = bigtable.Client(admin=True, project=project_id)
    instance = client.instance(instance_id)
    table = instance.table(table_name)
    table.create(
        column_families={
            "cf": column_family.MaxVersionsGCRule(1),
        }
    )

    logger.info("Created {} table on instance {}.".format(table_name, instance_id))

    rows, rows_dict = generate_vector_data(
        NUMBER_OF_ROWS_IN_BIGTABLE, VECTOR_DIMENSION, table
    )

    logger.info(
        "Inserting generated vector embeddings in Bigtable Table: {}.".format(
            table_name
        )
    )
    batcher = table.mutations_batcher()

    # In batcher, mutate will flush current batch if it
    # reaches the max_row_bytes
    batcher.mutate_rows(rows)
    batcher.flush()

    logger.info(
        "Inserted {} records in table {}.".format(
            NUMBER_OF_ROWS_IN_BIGTABLE, table_name
        )
    )

    return rows_dict


def deploy_workflow(project, location, workflow_name):
    """Deploys a workflow defined in file "https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/bigtable-ai/vertex-vector-search/workflows/batch-export.yaml" to Cloud Workflow.

    Args:
        project: The project ID.
        location: The location of the workflow.
        workflow_name: The name of the workflow.
    """
    logger.info(
        "Deploying workflow with name: {} on project: {} and location: {}.".format(
            workflow_name, project, location
        )
    )
    logger.info(
        "Picking workflow configuration from following path: {}.".format(
            WORKFLOW_YAML_FILE_PATH
        )
    )
    file_content = ""

    with open(WORKFLOW_YAML_FILE_PATH, "r") as file:
        # Read the entire file content
        file_content = file.read()

    # Create a client
    client = workflows_v1.WorkflowsClient()

    # Initialize request argument(s)
    workflow = workflows_v1.Workflow()
    workflow.source_contents = file_content

    request = workflows_v1.CreateWorkflowRequest(
        parent="projects/{project}/locations/{location}".format(
            project=project, location=location
        ),
        workflow=workflow,
        workflow_id=workflow_name,
    )

    # Make the request
    operation = client.create_workflow(request=request)

    logger.info("Waiting for deployment of workflow to complete...")

    response = operation.result()

    logger.info(
        "Workflow with name: {} deployed successfully on project: {}.".format(
            workflow_name, project
        )
    )


def execute_workflow(
    project, location, workflow_name, bigtable_arguments, vertex_index_id
):
    """Executes a workflow.

    Args:
        project: The project ID.
        location: The location of the workflow.
        workflow_name: The name of the workflow.
        bigtable_arguments: The dictionary with Bigtable Arguments.
        vertex_index_id: The Vertex Vector Search Index ID.
    """
    logger.info("Starting execution of workflow with name: {}.".format(workflow_name))

    client = executions_v1.ExecutionsClient()

    json_arguments = ""

    logger.info(
        "Reading workflow input template json from: {}.".format(
            WORKFLOW_INPUT_FILE_PATH
        )
    )

    with open(WORKFLOW_INPUT_FILE_PATH, "r") as file:
        # Read the entire file content
        json_arguments = json.load(file)

    json_arguments["project_id"] = project
    json_arguments["location"] = location
    json_arguments["bigtable"]["instance_id"] = bigtable_arguments["instance_id"]
    json_arguments["bigtable"]["table_name"] = bigtable_arguments["table_name"]
    json_arguments["vertex"]["vector_search_index_id"] = vertex_index_id

    workflow_execution_request = Execution()
    workflow_execution_request.argument = json.dumps(json_arguments, indent=4)

    # Initialize request argument(s)
    request = executions_v1.CreateExecutionRequest(
        parent="projects/{project}/locations/{location}/workflows/{workflow_name}".format(
            project=project, location=location, workflow_name=workflow_name
        ),
        execution=workflow_execution_request,
    )

    response = client.create_execution(request=request)

    logger.info(
        "Execution of workflow with name: {} triggered with following arguments: {}.".format(
            workflow_name, json_arguments
        )
    )

    return response


def get_worfklow_execution(arguments):
    """Gets a workflow execution.

    Args:
        arguments: A dictionary of arguments containg the `execution_id`.

    Returns:
        A workflow execution.
    """

    logger.info(
        "Fetching execution status of workflow with id: {}.".format(
            arguments["execution_id"]
        )
    )
    client = executions_v1.ExecutionsClient()

    # Initialize request argument(s)
    request = executions_v1.GetExecutionRequest(
        name=arguments["execution_id"],
    )

    # Make the request
    response = client.get_execution(request=request)

    # Handle the response
    return response


def workflow_execution_polling_predicate(workflow_execution_response):
    """A predicate that determines whether a workflow execution has finished.
    Checks whether the workflow state is `Active` or not.

    Args:
        workflow_execution_response: A workflow execution.

    Returns:
        True if the workflow execution has finished, False otherwise.
    """
    if workflow_execution_response.state != Execution.State.ACTIVE:
        return True

    return False


def polling(
    function_to_poll,
    arguments,
    function_poll_predicate,
    max_attempts=100,
    polling_interval=120,
):
    """A polling function that polls a function until a predicate is met.

    Args:
        function_to_poll: A function to poll.
        arguments: A dictionary of arguments to pass to the function to poll.
        function_poll_predicate: A predicate that determines whether the polling should stop.
        max_attempts: The maximum number of attempts to poll.
        polling_interval: The interval between polls.

    Returns:
        The result of the function to poll.
    """
    for attempt in range(max_attempts):
        response = function_to_poll(arguments)

        if function_poll_predicate(response):
            return response  # Desired condition met

        logger.info(
            "Attempt {}: Workflow execution in progress, waiting for workflow to finish...".format(
                attempt + 1
            )
        )
        time.sleep(polling_interval)

    raise TimeoutError("Polling timed out")


def sync_execute_workflow(
    project, location, workflow_name, bigtable_arguments, vertex_index_id
):
    """Synchronously executes a workflow.

    Args:
        project: The project ID.
        location: The location of the workflow.
        workflow_name: The name of the workflow.
        bigtable_arguments: The dictionary with Bigtable Arguments.
        vertex_index_id: The Vertex Vector Search Index ID.
    """
    execute_workflow_response = execute_workflow(
        project, location, workflow_name, bigtable_arguments, vertex_index_id
    )

    try:
        result = polling(
            get_worfklow_execution,
            {"execution_id": execute_workflow_response.name},
            workflow_execution_polling_predicate,
        )
        logger.info("Workflow exeuction finished with result: {}.".format(result))
    except TimeoutError:
        logger.error("Workflow exeuction polling timed out.")


def cleanup_bigtable_resources(project_id, instance_id, table_name):
    """Cleans up the Bigtable resources.

    Args:
        project_id: The project ID.
        instance_id: The instance ID.
        table_name: The table name.
    """
    client = bigtable.Client(admin=True, project=project_id)
    instance = client.instance(instance_id)
    instance.table(table_name).delete()

    logger.info("Dropped Bigtable table with name: {}.".format(table_name))


def read_index_datapoints(api_endpoint, keys):
    """Reads datapoints from a deployed Vertex Index.

    Args:
      api_endpoint: The AI Platform Index API endpoint.
      keys: A list of datapoint IDs to fetch.

    Returns:
      A ReadIndexDatapointsResponse.
    """
    # Create a client
    client_options = {"api_endpoint": api_endpoint}

    client = aiplatform_v1beta1.MatchServiceClient(client_options=client_options)

    # Initialize request argument(s)
    request = aiplatform_v1beta1.ReadIndexDatapointsRequest(
        deployed_index_id="bigtable_vector_batch_inte_1706731206928",
        ids=keys,
    )

    # Make the request
    response = client.read_index_datapoints(request=request)

    # Handle the response
    return response


@pytest.fixture
def bigtable_vertex_vector_search_data(project_id: str, instance_id: str):
    """
    Setting up Bigtable Table with vector embeddings to test the workflow.
    The function does following operations:
    1. Creation of Bigtable table.
    2. Inserting randomly generated vector embeddings data into bigtable table.
    3. Invoke the test to execute workflow and comapre vector embeddings.
    4. Tear down Bigtable Resources.
    """
    # Setup code, e.g., initialize resources
    logger.info("Setting up resources for Integration Tests")

    # 1 Setting up Bigtable
    try:
        rows = setup_bigtable(project_id, instance_id, BIGTABLE_TABLE_NAME)
    except Exception as e:
        logger.error(
            "An exception occurred while setting up Bigtable table: %s",
            str(e),
            exc_info=True,
        )
        pytest.fail(
            "Test failed due to unhandled exception while setting up bigtable table."
        )

    try:
        yield rows  # This is where the test runs
    except Exception as e:
        logger.error(
            "An exception occurred while executing workflow: %s",
            str(e),
            exc_info=True,
        )

    cleanup_bigtable_resources(project_id, instance_id, BIGTABLE_TABLE_NAME)


def compare_float_lists(list1, list2, tolerance=1e-5):
    """
    Compare two lists of floating-point numbers with a specified tolerance.

    This function compares two lists of floating-point numbers element-wise, allowing for a certain tolerance
    to account for small differences due to floating-point precision. It returns True if all corresponding elements
    in the two lists are within the specified tolerance, indicating that the lists are considered equal.
    If the lists have different lengths, they are not considered equal.

    Parameters:
        list1 (list of float): The first list of floating-point numbers to be compared.
        list2 (list of float): The second list of floating-point numbers to be compared.
        tolerance (float, optional): The allowable absolute difference between corresponding elements
            in the two lists. Default is 1e-5.

    Returns:
        bool: True if the lists are equal within the specified tolerance, False otherwise.

    Example:
        ```python
        list1 = [1.0, 2.00001, 3.00002]
        list2 = [1.00001, 2.0, 3.00003]

        are_equal = compare_float_lists(list1, list2)

        if are_equal:
            print("The lists are equal within the specified tolerance.")
        else:
            print("The lists are not equal within the specified tolerance.")
        ```
    """
    if len(list1) != len(list2):
        return False  # The lists have different lengths, so they can't be equal.

    for elem1, elem2 in zip(list1, list2):
        if abs(elem1 - elem2) > tolerance:
            return False  # The elements are not within the allowable error.

    return True


def read_and_compare_vertex_data(
    bigtable_vertex_vector_search_data, vertex_index_end_point_url
):
    """
    Reads and compares vertex data from a @code{bigtable_vertex_vector_search_data}

    Args:
        bigtable_vertex_vector_search_data (list of tuples): A list of tuples representing vector data
            retrieved from a Bigtable database, where each tuple contains an ID and vector embeddings.
        vertex_index_end_point_url (str): The URL of the Vertex Index endpoint for data retrieval.

    Raises:
        AssertionError: If the actual data retrieved from the Bigtable database is not found in
            the data fetched from the Vertex Index, or if the vector embeddings do not match.

    Returns:
        None: This function does not return a value but raises assertions if comparisons fail.
    """
    data_point_id_list = list(bigtable_vertex_vector_search_data.keys())
    data_point_id_list = [
        # Convert keys to strings
        key
        for key in data_point_id_list
    ]
    # Fetching data from Vertex Index
    vertex_vector_search_data = read_index_datapoints(
        vertex_index_end_point_url, data_point_id_list
    )

    for data_point in vertex_vector_search_data.datapoints:
        actual_data = bigtable_vertex_vector_search_data.get(
            data_point.datapoint_id, None
        )

        assert actual_data is not None

        actual_vector_embeddings = actual_data["embeddings"]
        vertex_index_vector_embeddings = list(data_point.feature_vector)

        assert compare_float_lists(
            actual_vector_embeddings, vertex_index_vector_embeddings
        )

        for restrict in data_point.restricts:
            if restrict.namespace == "allow":
                assert restrict.allow_list[0] == actual_data["allow"]
                assert not restrict.deny_list
            if restrict.namespace == "deny":
                assert restrict.deny_list[0] == actual_data["deny"]
                assert not restrict.allow_list
        for restrict in data_point.numeric_restricts:
            if restrict.namespace == "int":
                assert restrict.value_int == actual_data["int"]
            if restrict.namespace == "float":
                assert compare_float_lists(
                    [restrict.value_float], [actual_data["float"]]
                )
            if restrict.namespace == "double":
                assert restrict.value_double == actual_data["double"]


def test_bigtable_vertex_vector_search_integration(
    project_id, instance_id, setup_workflow, bigtable_vertex_vector_search_data
):
    """
    Tests integration between Bigtable and Vertex Vector Search.
    1. Execute the workflow synchronously.
    2. Fetch Vector Embeddings from Vertex Index.
    3. Compare generated embeddings from the embeddings in Vertex Index.
    """
    # Execute Workflow
    sync_execute_workflow(
        project_id,
        WORKFLOW_LOCATION,
        WORKFLOW_NAME,
        {
            "instance_id": instance_id,
            "table_name": BIGTABLE_TABLE_NAME,
        },
        VERTEX_VECTOR_SEARCH_INDEX,
    )

    read_and_compare_vertex_data(
        bigtable_vertex_vector_search_data, VERTEX_VECTOR_SEARCH_INDEX_ENDPOINT
    )


def setup_and_execute_workflow(
    project, location, workflow_name, bigtable_arguments, vertex_index_id, result_list
):
    """
    Sets up a Bigtable database and executes a workflow, then appends the result to a list.

    Args:
        project (str): The project ID for GCP.
        location (str): The location where the workflow will be executed.
        workflow_name (str): The name of the workflow to be executed.
        bigtable_arguments (dict): A dictionary containing Bigtable setup parameters, including
            instance_id, and table_name.
        vertex_index_id (str): The ID of the Vertex Index.
        result_list (list): A list to which the result will be appended.

    Returns:
        None: This function does not return a value directly but appends the result rows
        to the result_list.

    Raises:
        Any exceptions raised by the functions called within this function may be propagated.

    Note:
        This function sets up a Bigtable database, executes a workflow, and appends the result rows
        to the provided result_list.
    """

    # 1 Setting up Bigtable
    rows = setup_bigtable(
        project,
        bigtable_arguments["instance_id"],
        bigtable_arguments["table_name"],
    )

    # 2 Execute Workflow
    sync_execute_workflow(
        project, location, workflow_name, bigtable_arguments, vertex_index_id
    )

    result_list.append(rows)

    cleanup_bigtable_resources(
        project,
        bigtable_arguments["instance_id"],
        bigtable_arguments["table_name"],
    )


def test_concurrent_workflow_execution(project_id, instance_id, setup_workflow):
    """
    Test the concurrent execution of workflow in separate threads.

    Args:
        setup_workflow (fixture): A fixture that sets up the necessary environment for testing.

    Raises:
        AssertionError: If the concurrent workflow execution does not behave as expected.

    Note:
        This test function verifies the behavior of concurrent execution of the
        'setup_and_execute_workflow' function in separate threads. It creates two threads
        to run the function with different parameters and verifies the results.
        The final vertex state should be consistent with the latest bigtable data.
    """

    # Create a thread for the async function without blocking
    result_list1 = []
    result_list2 = []

    thread_1 = threading.Thread(
        target=setup_and_execute_workflow,
        args=(
            project_id,
            WORKFLOW_LOCATION,
            WORKFLOW_NAME,
            {
                "instance_id": instance_id,
                "table_name": BIGTABLE_TABLE_NAME + "_first",
            },
            VERTEX_VECTOR_SEARCH_INDEX,
            result_list1,
        ),
    )
    thread_1.start()

    # Wait for 5 minutes (300 seconds)
    time.sleep(60)

    # Create another thread for the async function
    thread_2 = threading.Thread(
        target=setup_and_execute_workflow,
        args=(
            project_id,
            WORKFLOW_LOCATION,
            WORKFLOW_NAME,
            {
                "instance_id": instance_id,
                "table_name": BIGTABLE_TABLE_NAME + "_second",
            },
            VERTEX_VECTOR_SEARCH_INDEX,
            result_list2,
        ),
    )
    thread_2.start()

    thread_1.join()
    thread_2.join()

    # Vertex should have data points which were latest
    read_and_compare_vertex_data(result_list2[0], VERTEX_VECTOR_SEARCH_INDEX_ENDPOINT)


def get_env_var(key: str, desc: str) -> str:
    v = os.environ.get(key)
    if v is None:
        raise ValueError(f"Must set env var {key} to: {desc}")
    return v
