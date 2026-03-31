# 4.1. Solution Strategy

## 4.1.1. Architecture Approach

The who-votes-like-you system employs a distributed software architecture with clear separation of concerns across
multiple Gradle modules.
This approach enables independent development, testing, and deployment of different components.

### Design Principles

- **Separation of Concerns**: Each module has dedicated responsibility in context of whole system (data collection,
  storage management, vote access)
- **Scalability**: Services that could require scalability like database access should not rely on patterns reserved for
  single-instance applications
- **Testability**: Each module can be tested independently but should be easy to integrate as a whole for E2E tests

## 4.1.2. Module Architecture

### Core Data Modules

- **model**: Contains all data models, entities, and value objects used throughout the system

### Data Processing Modules

- **vote-storage**: Responsible for accessing and writing the data to database
- **vote-fetcher**: Responsible for retrieving voting data from external systems
- **db-synchronizer**: Orchestrator for fetching and saving the data

### Utility Modules

- **utils**: Utility methods for backend
- **message-system**: Implements messaging infrastructure for internal system notifications and event handling

### Testing and Validation

- **acceptance-tests**: Acceptance tests validating system behavior using mocked data
- **online-acceptance-tests**: Acceptance tests validating system using real services
- **pet**: Performance tests using mocked data

## 4.1.3. Technology Selection

### Backend Technologies

- Kotlin with Spring Boot for server applications
- Neo4j for data storage

### Frontend Technologies

- Vue 3 with Pinia and Quasar for web page development

## 4.1.4. Quality Requirements

### Performance

- Sensible access for humans to voting data and analitics
- Non-intrusive collection of data from public sites (no DOS like behavior)

### Security

- No user data storage
- Collecting and storing publicly available data from government sites
