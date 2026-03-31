# 5. Building Block View

## 5.1 Module Structure

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

