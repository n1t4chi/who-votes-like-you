plugins {
    id("kotlin-module")
}

dependencies {
    implementation(Deps.Kotest.assertionsCode)
    implementation(Deps.Kotest.property)
    implementation(Deps.Kotest.runner)
}
