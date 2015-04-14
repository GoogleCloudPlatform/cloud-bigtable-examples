# Cloud Bigtable MapReduce Example

The Map/Reduce code for Cloud Bigtable should look identical to HBase Map/Reduce jobs. The main issue of running against specific HBase and Hadoop versions. Take note of the dependencies in pom.xml. The HBase codebase has gone through quite a bit of churn related to its API. Due to the API churn, jobs that require the Google BigTable Cloud HBase compatibility layer require very specific versions of HBase in order to execute correctly. Compiling Map/Reduce code against different HBase versions may have problems executing relating to class incompatibility issues.

## Project setup, installation, and configuration

How do I, as a developer, start working on the project?

1. What dependencies does it have (where are they expressed) and how do I install them?
1. Can I see the project working before I change anything?


## Testing

How do I run the project's automated tests?

* Unit Tests

* Integration Tests


## Deploying

### How to setup the deployment environment

* Addons, packages, or other dependencies required for deployment.
* Required environment variables or credentials not included in git.
* Monitoring services and logging.

### How to deploy


## Troubleshooting & useful tools

### Examples of common tasks

e.g.
* How to make curl requests while authenticated via oauth.
* How to monitor background jobs.
* How to run the app through a proxy.


## Contributing changes

* See [CONTRIBUTING.md](../../CONTRIBUTING.md)


## Licensing

* See [LICENSE](../../LICENSE)
