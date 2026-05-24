import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class RegexReplaceRecursivelyInFilesTask : DefaultTask() {

    @get:LocalState
    abstract val directory: DirectoryProperty

    @get:Input
    abstract val regex: Property<String>

    @get:Input
    abstract val replace: Property<String>

    @TaskAction
    fun walkDirectory() {
        directory.asFileTree
            .forEach { file -> replaceInFile(file) }
    }

    fun replaceInFile(file: File) {
        if(file.isFile && (file.extension == "kt" || file.extension == "kts")) {
            file.writeText(file.readText().replace(regex.get().toRegex(), replace.get()))
        }
    }
}
