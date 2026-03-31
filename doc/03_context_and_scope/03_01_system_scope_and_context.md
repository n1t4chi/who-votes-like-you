# 3. System Scope and Context

## 3.1 Business Context

The who-votes-like-you system is designed for collecting the party/politician votes and allowing for an easy analysis of
those.
Main feature is filling your own votes for selected topics/votings and comparing the results across different parties
and politicians.
The intent is to see what parties/politicians align best based on their voting history.

### Key Business Processes

- Data collection from various voter databases
- Pattern recognition and predictive modeling
- Report generation

## 3.2 System Context

### External Systems

- **Vote Databases**: Primary source of voting data from government sites

### Interfaces

- REST API for vote data access
- Web page for data analytics

## 3.3 Stakeholder Interactions

### Primary Users

- Users interested in checking out voting patterns of parties/politicians

### Interaction Patterns

1. **Data providers**: Expose publicly available voting data to the system
2. **System Administrators**: Monitor and maintain platform infrastructure
3. **End Users**: Access data and perform analysis over web page

## 3.4 System Boundaries

The system boundary includes:

- Data ingestion and processing modules
- User interface components

External boundaries include:

- External data sources