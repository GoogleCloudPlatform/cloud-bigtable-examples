# Cloud Bigtable Hello World DotNet
 
 This is a simple application that demonstrates using the Ruby Google Cloud
 API to connect to and interact with Cloud Bigtable.
 
 **Table of Contents**
 
 <!-- START doctoc generated TOC please keep comment here to allow auto update -->
 <!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
 
 
 - [Downloading the sample](#downloading-the-sample)
 - [Costs](#costs)
 - [Before you begin](#before-you-begin)
   - [Installing Ruby dependencies](#installing-ruby-dependencies)
   - [Creating a Project in the Google Cloud Platform Console](#creating-a-project-in-the-google-cloud-platform-console)
   - [Enabling billing for your project.](#enabling-billing-for-your-project)
   - [Enable the Cloud Bigtable APIs.](#enable-the-cloud-bigtable-apis)
   - [Install the Google Cloud SDK.](#install-the-google-cloud-sdk)
   - [Setting Google Application Default Credentials](#setting-google-application-default-credentials)
 - [Provisioning an instance](#provisioning-an-instance)
 - [Running the application](#running-the-application)
 - [Cleaning up](#cleaning-up)
 
 <!-- END doctoc generated TOC please keep comment here to allow auto update -->
 
 
 ## Downloading the sample
 
 Download the sample app and navigate into the app directory:
 
 1.  Clone the [Cloud Bigtable examples repository][github-repo], to your local
     machine:
 
         git clone https://github.com/GoogleCloudPlatform/cloud-bigtable-examples.git
 
     Alternatively, you can [download the sample][github-zip] as a zip file and
     extract it.
 
 2.  Change to the Hello World code sample directory.
 
         cd cloud-bigtable-examples/dotnet/hello-world
 
 [github-repo]: https://github.com/GoogleCloudPlatform/cloud-bigtable-examples
 [github-zip]: https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/archive/master.zip
 
 
 ## Costs
 
 This sample uses billable components of Cloud Platform, including:
 
 +   Google Cloud Bigtable
 
 Use the [Pricing Calculator][bigtable-pricing] to generate a cost estimate
 based on your projected usage.  New Cloud Platform users might be eligible for
 a [free trial][free-trial].
 
 [bigtable-pricing]: https://cloud.google.com/products/calculator/#id=1eb47664-13a2-4be1-9d16-6722902a7572
 [free-trial]: https://cloud.google.com/free-trial
 
 
 ## Before you begin
 
 This sample assumes you have [.NET Core 2.0][.NET Core] 2.0 installed.
 
 [.NET Core]:https://docs.microsoft.com/en-us/dotnet/core/
 
 To install .NET Core please follow [this][this] tutorial.
 
 [this]:https://www.microsoft.com/net/learn/get-started/windows 
 
 ### Creating a Project in the Google Cloud Platform Console
 
 If you haven't already created a project, create one now. Projects enable you to
 manage all Google Cloud Platform resources for your app, including
 deployment,
 access control, billing, and services.
 
 1. Open the [Cloud Platform Console][cloud-console].
 1. In the drop-down menu at the top, select **Create a project**.
 1. Give your project a name.
 1. Make a note of the project ID, which might be different from the project
    name. The project ID is used in commands and in configurations.
 
 [cloud-console]: https://console.cloud.google.com/
 
 ### Enabling billing for your project.
 
 If you haven't already enabled billing for your project, [enable
 billing][enable-billing] now.  Enabling billing allows is required to use
 Cloud Bigtable and to create VM instances.
 
 [enable-billing]: https://console.cloud.google.com/project/_/settings
 
 ### Enable the Cloud Bigtable APIs.
 
 Make sure to [enable the Bigtable APIs][enable-bigtable-api].
 
 [enable-bigtable-api]: https://console.cloud.google.com/apis/library?q=bigtable
 
 ### Install the Google Cloud SDK.
 
 If you haven't already installed the Google Cloud SDK, [install the Google
 Cloud SDK][cloud-sdk] now. The SDK contains tools and libraries that enable you
 to create and manage resources on Google Cloud Platform.
 
 [cloud-sdk]: https://cloud.google.com/sdk/
 
 ### Setting Google Application Default Credentials
 
 Set your [Google Application Default
 Credentials][application-default-credentials] by [initializing the Google Cloud
 SDK][cloud-sdk-init] with the command:
 
     gcloud init
 
 Generate a credentials file by running the [application-default login](https://cloud.google.com/sdk/gcloud/reference/auth/application-default/login) command:
 
     gcloud auth application-default login
 
 [cloud-sdk-init]: https://cloud.google.com/sdk/docs/initializing
 [application-default-credentials]: https://developers.google.com/identity/protocols/application-default-credentials
 
 
 ## Provisioning an instance
 
 Follow the instructions in the [user
 documentation](https://cloud.google.com/bigtable/docs/creating-instance) to
 create a Google Cloud Platform project and Cloud Bigtable instance if necessary.
 You'll need to reference your project id and instance id to run the
 application.
 
 
 ## Running the application
 
 In appsettings.json replace `<GCP-project-id>` with Google Cloud Platform project name and `<Bigtable-instance-id>` with what you called it when provisioning the instance.
 
    "projectId": `[PROJECT_ID]`,
    "instanceId": `[BIGTABLE_INSTANCE]`,
 
 Run the sample using `dotnet run` CLI command:
    
    dotnet run -c Release
 
 You will see output resembling the following, interspersed with informational logging
 from the underlying libraries:
 
    Create new table: Hello-Bigtable with column family cf, Instance: helloworld
    Table Hello-Bigtable created succsessfully

    Write some greetings to the table Hello-Bigtable
            Greeting: --Hello World!-- written successfully
            Greeting: --Hellow Bigtable!-- written successfully
            Greeting: --Hellow C#!-- written successfully
    Read the first row
            Row key: greeting0, Value: Hello World!
    Read all rows using streaming
            Row key: greeting0, Value: Hello World!
            Row key: greeting1, Value: Hellow Bigtable!
            Row key: greeting2, Value: Hellow C#!
    Delete table: Hello-Bigtable
    Table: Hello-Bigtable deleted succsessfully
 
 ## Cleaning up
 
 To avoid incurring extra charges to your Google Cloud Platform account, remove
 the resources created for this sample.
 
 1.  Go to the [Instances page][Instances page] in the Cloud Cloud Platform
 
     [Instances page]:https://console.cloud.google.com/project/_/bigtable/instances
 
 2.  Click the isntance name.
 
 3.  Click **Delete**.
 
     ![Delete]( https://cloud.google.com/bigtable/img/delete-quickstart-instance.png)
 
 4. Type the instance ID, then click **Delete** to delete the instance.