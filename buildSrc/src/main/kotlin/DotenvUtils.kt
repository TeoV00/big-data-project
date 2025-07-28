import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.process.ProcessForkOptions
import java.io.File
import kotlin.reflect.KClass

object DotenvUtils {

    val Project.dotenv: DotEnv?
        get() = file(".env").takeIf { it.exists() }?.let { DotEnv.from(it) }

    fun <T> Project.injectInto(vararg taskTypes: KClass<out T>) where T : Task, T : ProcessForkOptions =
        ProcessForkOptionConfigurator(
            taskTypes.flatMap { tasks.withType(it.java) }
        )

    class ProcessForkOptionConfigurator<T>(private val taskTypes: List<T>) where T : Task, T : ProcessForkOptions {
        infix fun environmentsFrom(dotenv: DotEnv) = taskTypes.forEach {
            it.doFirst {
                dotenv.variables().forEach { (key, value) -> it.environment(key, value) }
            }
        }
    }

    interface DotEnv {

        /** The `*.env` file instance. */
        val file: File

        /** @return a map environment variables key-value pairs. */
        fun variables(): Map<String, String>

        companion object {
            fun from(file: File): DotEnv = DotEnvImpl(file)
        }
    }

    private class DotEnvImpl(override val file: File) : DotEnv {

        init {
            require(file.exists() && file.name.endsWith(EXTENSION_NAME)) {
                "File $file does not exist or has wrong extension. Only `*$EXTENSION_NAME` files are supported."
            }
        }

        override fun variables(): Map<String, String> = file
            .readLines()
            .filter { it.isNotBlank() && !it.startsWith(COMMENT_SYMBOL) }
            .associate { it.split(KEY_VALUE_SEPARATOR).let { (key, value) -> key to value } }

        private companion object {
            const val EXTENSION_NAME = ".env"
            const val COMMENT_SYMBOL = "#"
            const val KEY_VALUE_SEPARATOR = "="
        }
    }
}