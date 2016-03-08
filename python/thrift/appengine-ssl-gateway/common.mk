# Copyright 2016 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
################################################################################

VERB = @
ifeq ($(VERBOSE),1)
	VERB =
endif

default:
	$(VERB) echo "Follow instructions in README.md"

# Ensure common env vars are set up properly.
# TODO: automatically pick up PROJECT_ID, ZONE if running on GCE.

env_project_id:
	$(VERB) if [ -z "$${PROJECT_ID}" ]; then echo '$$PROJECT_ID not set'; exit 1; fi

env_zone:
	$(VERB) if [ -z "$${ZONE}" ]; then echo '$$ZONE not set'; exit 1; fi

env_cluster_id:
	$(VERB) if [ -z "$${CLUSTER_ID}" ]; then echo '$$CLUSTER_ID not set'; exit 1; fi

env_bucket:
	$(VERB) if [ -z "$${BUCKET}" ]; then echo '$$BUCKET not set'; exit 1; fi

env_docker_project_id:
	$(VERB) if [ -z "$${DOCKER_PROJECT_ID}" ]; then echo '$$DOCKER_PROJECT_ID not set'; exit 1; fi
