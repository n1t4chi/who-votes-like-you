# 5.1. Building Block View

## 5.1.1. Module Structure

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

## 5.1.2. Redesign (under development)
Current architecture could be a problem for further development. 
New target architecture was redesigned and can be checked out here:
- [Project big picture board](../01_introduction_and_goals/01_03_big_picture_board.png) - shows target solution with use-cases, architecture and domain boundaries. 
- [vote fetcher design board](05_02_vote_fetcher_design_board.png) - description of new fetcher that uses more ETL appropriate patterns.
- [vote viewer design board](05_03_vote_viewer_design_board.png) - description of general user interface.
- [vote analyzer design board](05_04_vote_analyzer_design_board.png) - description of the core of the project which is the analysis which parties/politicians vote the closest to user self-proclaimed preferences.
- [ADR for redesign](../09_architectural_decisions/09_01_redesign_from_initial_structure.md)

For more info why, see [section](../11_risks_and_tech_debt/11_01_risks_and_tech_debt.md#project-revival-and-architecture-review) in risks and tech debt chapter.