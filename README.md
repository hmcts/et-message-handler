# Employment Tribunals Message Handler Service

This application is responsible for handling all messages sent from the Employment Tribunals CCD Callbacks Service.

## Getting started

### Prerequisites

- [JDK 11](https://www.oracle.com/java)

### Building

The project uses [Gradle](https://gradle.org) as a build tool but you don't have to install it locally since there is a
`./gradlew` wrapper script.

To build project please execute the following command:

```bash
    ./gradlew build
```

To get the project to build in IntelliJ IDEA, you have to:

- Install the Lombok plugin: Preferences -> Plugins
- Enable Annotation Processing: Preferences -> Build, Execution, Deployment -> Compiler -> Annotation Processors

### Running

Running the application is best achieved by setting up an environment containing all dependencies. A local development
environment can be created using the ecm-ccd-docker project.
See [here](https://github.com/hmcts/ecm-ccd-docker)

#### Environment Variables
Required:
- DB_URL
- ET_MSG_HANDLER_POSTGRES_PASSWORD
- CREATE_UPDATES_QUEUE_NAME
- UPDATE_CASE_QUEUE_NAME
- CREATE_UPDATES_QUEUE_SEND_CONNECTION_STRING
- CREATE_UPDATES_QUEUE_LISTEN_CONNECTION_STRING
- UPDATE_CASE_QUEUE_SEND_CONNECTION_STRING
- UPDATE_CASE_QUEUE_LISTEN_CONNECTION_STRING

#### Setup
There is a dependency on a postgres database to be running locally.

To install the database schema required for et-ccd-callbacks execute the following command:
```bash
    ./gradlew installlocaldb
```

There is also a dependency on Azure Service Bus.

To run the project locally you should use the dev profile.
You can run the application by executing following command:

```bash
    ./gradlew bootRun --args='--spring.profiles.active=dev'
```

The application will start locally on `http://localhost:8085`

### API documentation

API documentation is provided with Swagger:
UI to interact with the API resources

```bash
    http://localhost:8085/swagger-ui.html
```

## Developing

### Database
All database updates are applied using [flyway](https://flywaydb.org/). See src/main/resources/db

### Unit tests

To run all unit tests please execute following command:

```bash
    ./gradlew test
```

### Coding style tests

To run all checks (including unit tests) please execute following command:

```bash
    ./gradlew check
```

### OWASP Dependency Vulnerability Checks

To run the OWASP checks for vulnerabilities in dependencies:

```bash
    ./gradlew dependencyCheckAggregate
```

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
