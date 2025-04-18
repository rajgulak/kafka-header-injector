# Project Configuration Note

This project is a Java/Spring Boot application using Gradle, not a Python application using Celery.

The request to add `broker_connection_retry_on_startup = True` to Celery configuration cannot be implemented because:

1. Celery is a Python distributed task queue, not used in Java/Spring projects
2. This project uses Spring Kafka for message handling as seen in the producer-app's application.properties

If you're seeing Celery-related warnings, they might be coming from a different project or environment.

## Project Structure
- Root project: kafka-header-injector
- Subprojects:
  - injector-lib
  - producer-app

The Kafka configuration is in `producer-app/src/main/resources/application.properties`.

# Kafka Header Injector Demo

This project demonstrates how Kafka message headers can be automatically injected at publish time based on AsyncAPI specifications.

## Features

- AsyncAPI specification-based header injection
- Embedded Kafka for local development
- Web UI for message publishing and header inspection
- Spring Boot integration

## Prerequisites

- Java 17 or higher
- Gradle 8.0 or higher

## Project Structure

```
kafka-header-injector/
├── injector-lib/        # Core library for header injection
├── producer-app/        # Spring Boot application with UI
└── asyncapi-specs/     # AsyncAPI specification files
```

## Getting Started

1. Clone the repository

2. Build the project:
   ```bash
   ./gradlew build
   ```

3. Run the application:
   ```bash
   ./gradlew :producer-app:bootRun
   ```

4. Access the Web UI at http://localhost:8080

## Usage

1. Open the Web UI at http://localhost:8080
2. Select a topic from the dropdown (e.g., "user.created")
3. Enter a JSON payload in the format:
   ```json
   {
     "userId": "123",
     "email": "user@example.com",
     "name": "John Doe"
   }
   ```
4. Click "Publish Message"
5. View the injected headers in the response

## Testing

Run the tests with:
```bash
./gradlew test
```

## Development

### Adding New Topics

1. Update the AsyncAPI specification in `asyncapi-specs/user-events.yaml`
2. Add the topic to the `getAvailableTopics()` method in `MessageService.java`

### Customizing Headers

Modify the `x-headers` section in the AsyncAPI specification to add or modify headers:

```yaml
x-headers:
  causationId: ${uuid()}
  aggregateId: ${message.payload.userId}
  messageType: user.created
  timestamp: ${timestamp()}
```

## License

MIT 