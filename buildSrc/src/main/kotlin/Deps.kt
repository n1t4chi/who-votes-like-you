object Deps {

    object Kotest {
        val version = "6.1.11"
        val assertionsCode = "io.kotest:kotest-assertions-core:${version}"
        val property = "io.kotest:kotest-property:$version"
        val runner = "io.kotest:kotest-runner-junit6-jvm:$version"
        val plugin = "io.kotest"
    }
    object Ktlint {
        val version = "14.2.0"
        val plugin = "org.jlleitschuh.gradle.ktlint"
    }
    object Node {
        val version = "7.1.0"
        val plugin = "com.github.node-gradle.node"
    }
}
