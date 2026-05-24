plugins {
    id(Deps.Kotest.plugin).version(Deps.Kotest.version).apply(false)
    id(Deps.Node.plugin).version(Deps.Node.version).apply(false)
    id(Deps.Ktlint.plugin).version(Deps.Ktlint.version).apply(false)
    id("org.openapi.generator").version("7.24.0").apply(false)
}
