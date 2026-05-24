rootProject.name = "who-votes-like-you"
include(
    "jobs",
    "jobs:api",
    "jobs:service",

    "models",

    "storage",
    "storage:api",
    "storage:in-memory",

    "utilities",
    "utilities:test-utilities",

    "vote-fetcher",
    "vote-fetcher:service",

    "vote-viewer",
    "vote-viewer:frontend",
    "vote-viewer:service",

    "vote-analyzer",

    "voting-tenant",
    "voting-tenant:plugin-api",
    "voting-tenant:implementations",
    "voting-tenant:implementations:polish-sejm",
    "voting-tenant:implementations:polish-sejm:client",
    "voting-tenant:implementations:polish-sejm:plugin",
    "voting-tenant:service",
)

