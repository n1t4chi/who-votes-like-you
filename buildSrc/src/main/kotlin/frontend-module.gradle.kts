plugins.apply(Deps.Node.plugin)

tasks.register("test") {
    description = "run frontend tests, linter and TS check"
    group = "verification"
    dependsOn("npm_run_format-check")
    dependsOn("npm_run_ts-check")
    dependsOn("npm_run_test")
}
