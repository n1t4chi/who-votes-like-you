# 11. Risks and Tech Debt

## Project revival and architecture review

Project was started long time ago and then abandoned due to lack of free time.
It was also developed as a training opportunity for Kotlin and Gradle.
After additional few years of experience it's a bit clear that the initial architecture could use some grand refactor.

### Topics to consider:

#### Data model

The data model is very tightly modeled after structure of the votes available fr the Polish Sejm.
This makes the current setup only consider Polish Sejm votes and has concepts that are not really that important (e.g.
voting day)
for the application.

#### Fetching refactored into more appriopriate ETL architecture

Fetching currently is a bit tightly coupled into whole system and uses messaging system to orchestrate everything.
This could be separated into more pipes and filter architecture.

#### Tenancy / Plugin like concepts for voting data sets.

Current setup only considers the Polish Sejm votes.
This could be abstracted with tenancy with plugin like vote fetching, allowing for integration with other voting
tenants.