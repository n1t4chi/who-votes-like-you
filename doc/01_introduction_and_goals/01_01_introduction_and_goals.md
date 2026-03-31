# 1.1. Introduction and Goals

## 1.1.1. Project Overview

The who-votes-like-you project is designed for collecting the party/politician votes and allowing for an easy analysis
of those.
Main feature is filling your own votes for selected topics/votings and comparing the results across different parties
and politicians.
The intent is to see what parties/politicians align best based on their voting history.

The project is in form of multi-module Kotlin and Vue application.
It collects votes and provides insights into political party/politicians behaviors.
The system consists of several interconnected modules that handle data fetching, storage, processing, and presentation.

### Big picture

Board which shows the big picture of this project can be found here: [here](01_03_big_picture_board.png).
This covers:
- major use-cases of this system
- user interactions
- architecture and boundaries

Currently this is more of a target architecture rather than current one which warrants redesign.

This is a result of big picture event storming and can be freely edited as new features/requirements arrive.

## 1.1.2. Goals and Objectives

### Primary Goals

- Collect votes in supported voting tenants (e.g. Polish Sejm voting sessions).
- Allow to browse and get insights from collected votes in user-friendly way
- Allow to check and compare your preferences with parties and politicians

### Success Metrics

- System scalability to handle multiple tenants and datasets
- Tenancy system for easy to extend and integrate different entities with publicly available voting results
- Reliable data processing pipeline with minimal downtime
- User-friendly interface for data visualization
- No or absolute legal minimum data collection from end-users

## 1.1.3. Stakeholders

| Stakeholder           | Role                       | Expectations                                       |
|-----------------------|----------------------------|----------------------------------------------------|
| Governments           | Data providers             | Legal, non intrusive and impartial data collection |
| System Administrators | Infrastructure maintainers | Stable, scalable platform                          |
| End Users             | Platform consumers         | Intuitive interface and data access                |
