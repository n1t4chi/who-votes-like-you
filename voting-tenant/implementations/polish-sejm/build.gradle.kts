plugins {
    id("composite-module")
    id("org.openapi.generator")
}

val newClientFolder = file("new-client")
val clientFolder = file("client")

openApiGenerate {
    inputSpec.set("api.sejm.gov.pl.yaml")
    generatorName.set("kotlin")
    modelNamePrefix = "Sejm"
    modelNameSuffix = "Dto"
    setOutputDir(newClientFolder.path)
    configOptions.put("packageName", "vote.fetcher.polishsejm.client")
    configOptions.put("groupId", "vote.fetcher.polishsejm")
    configOptions.put("artifactId", "polishsejm-client")
    configOptions.put("library", "jvm-okhttp4")
    configOptions.put("omitGradlePluginVersions", "true")
    configOptions.put("omitGradleWrapper", "true")
    configOptions.put("useSettingsGradle", "false")
}

tasks.register<Copy>("copyNewSources") {
    group = "client-setup"
    from(newClientFolder.resolve("src/main"))
    into(clientFolder.resolve("src/main"))
}
tasks.register<Delete>("removeStaleSources") {
    group = "client-setup"
    delete(clientFolder.resolve("src/main"))
}
tasks.register<Delete>("removeGeneratedModule") {
    group = "client-setup"
    delete(newClientFolder)
}

tasks.register("regenerateClient") {
    group = "client-setup"
}

tasks.register<RegexReplaceRecursivelyInFilesTask>("replaceOffsetDateTimeInApi") {
    group = "client-setup"
    directory = newClientFolder.resolve("src/main/kotlin/vote/fetcher/polishsejm/client/apis")
    regex = "OffsetDateTime"
    replace = "LocalDateTime"
}
tasks.register<RegexReplaceRecursivelyInFilesTask>("replaceOffsetDateTimeInModel") {
    group = "client-setup"
    directory = newClientFolder.resolve("src/main/kotlin/vote/fetcher/polishsejm/client/models")
    regex = "OffsetDateTime"
    replace = "LocalDateTime"
}
tasks.register<RegexReplaceRecursivelyInFilesTask>("replaceDuplicatedEliInModel") {
    group = "client-setup"
    directory = newClientFolder.resolve("src/main/kotlin/vote/fetcher/polishsejm/client/models")
    regex = "val ELI"
    replace = "val _ELI"
}
tasks.register<RegexReplaceRecursivelyInFilesTask>("replaceDuplicatedMpInModel") {
    group = "client-setup"
    directory = newClientFolder.resolve("src/main/kotlin/vote/fetcher/polishsejm/client/models")
    regex = "val MP"
    replace = "val _MP"
}

tasks.getByName("replaceDuplicatedEliInModel").dependsOn("openApiGenerate")
tasks.getByName("replaceDuplicatedMpInModel").dependsOn("replaceDuplicatedEliInModel")
tasks.getByName("replaceOffsetDateTimeInModel").dependsOn("replaceDuplicatedMpInModel")
tasks.getByName("replaceOffsetDateTimeInApi").dependsOn("openApiGenerate")

tasks.getByName("copyNewSources").dependsOn("replaceOffsetDateTimeInModel")
tasks.getByName("copyNewSources").dependsOn("replaceOffsetDateTimeInApi")
tasks.getByName("copyNewSources").dependsOn("removeStaleSources")
tasks.getByName("copyNewSources").finalizedBy("client:runKtlintFormatOverMainSourceSet")

tasks.getByName("removeGeneratedModule").dependsOn("copyNewSources")

tasks.getByName("regenerateClient").dependsOn("replaceOffsetDateTimeInModel")
tasks.getByName("regenerateClient").dependsOn("replaceOffsetDateTimeInApi")
tasks.getByName("regenerateClient").dependsOn("removeGeneratedModule")
