package no.nav

import com.github.shyiko.klob.Glob
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.json.JsonParser
import com.sksamuel.hoplite.yaml.YamlParser
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.EngineDescriptor
import org.junit.platform.engine.support.descriptor.FilePosition
import org.junit.platform.engine.support.descriptor.FileSource
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.forEachLine
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

object EngineDescriptorFactory {
    fun create(engineId: UniqueId, specFiles: List<Path>): EngineDescriptor {
        val engineDesc = EngineDescriptor(engineId, "FHIR Validator")

        specFiles.forEachIndexed { tsIndex, path ->
            val testSuiteId = engineId.append<TestSuiteDescriptor>(tsIndex)
            val testSuiteSpec = loadConfig(path).run { copy(title = title ?: path.nameWithoutExtension) }
            val testSuiteSource = FileSource.from(path.toFile())
            val testSuiteDesc = TestSuiteDescriptor(testSuiteId, testSuiteSpec, testSuiteSource)
            engineDesc.addChild(testSuiteDesc)

            testSuiteSpec.tests.forEachIndexed { tcIndex, testCaseSpec ->
                val testCaseId = testSuiteId.append<TestCaseDescriptor>(tcIndex)
                val testCaseSource = createFileSource(path, "source", tcIndex)
                val testCaseDesc = TestCaseDescriptor(testCaseId, testCaseSpec, testCaseSource)
                testSuiteDesc.addChild(testCaseDesc)
            }

            if (testSuiteSpec.paths.any()) {
                val pathsTestSource = createFileSource(path, "paths", 0)

                testSuiteSpec.paths
                    .map { Specification.TestCase(Path(it)) }
                    .filterNot { testSuiteSpec.tests.any { existing -> existing.source == it.source } }
                    .forEachIndexed { tcIndex, testCaseSpec ->
                        val testCaseId = testSuiteId.append<TestCaseDescriptor>(testSuiteSpec.tests.count() + tcIndex)
                        val testCaseDesc = TestCaseDescriptor(testCaseId, testCaseSpec, pathsTestSource)
                        testSuiteDesc.addChild(testCaseDesc)
                    }
            }
        }

        return engineDesc
    }
}

private inline fun <reified T> UniqueId.append(id: Int) = append(T::class.simpleName, "$id")!!

/** Parsers needs to be explicitly mapped to file-extensions to work with ShadowJar. */
private val configLoader = ConfigLoader.Builder()
    .addFileExtensionMapping("json", JsonParser())
    .addFileExtensionMapping("yaml", YamlParser())
    .addFileExtensionMapping("yml", YamlParser())
    .build()

/** Loads the config/specification and resolves all relative paths. */
private fun loadConfig(specPath: Path): Specification {
    fun resolveAndNormalize(path: Path): Path {
        if (path.isAbsolute) return path
        val dir = if (specPath.isDirectory()) specPath else specPath.toAbsolutePath().parent
        return dir.resolve(path).normalize()
    }

    val config = configLoader.loadConfigOrThrow<Specification>(specPath)

    // Resolves .gitgnore-pattern based paths to absolute paths.
    val resolvedPaths = Glob
        .from(*config.paths.toTypedArray())
        .iterate(specPath.toAbsolutePath().parent)
        .asSequence()
        .map { it.toString() }
        .toList()

    // An IG can be specified as either package, file, folder or URL.
    // In case of file or folder we want the path to be resolved relative to the specification file.
    val resolvedIgs = config.validator.ig.map {
        try { resolveAndNormalize(Path(it)).toString() } catch (ex: Throwable) { it }
    }

    return config.copy(
        validator = config.validator.copy(ig = resolvedIgs),
        paths = resolvedPaths,
        tests = config.tests.map {
            it.copy(source = resolveAndNormalize(it.source))
        }
    )
}

/** Creates a FileSource with FilePosition (line, column) of the property within json or yaml file. */
private fun createFileSource(specPath: Path, property: String, index: Int): FileSource {
    fun findAllMatches(pattern: Regex) =
        sequence<FilePosition> {
            val commentLinePattern = Regex("^ *#")
            var lineNr = 1
            specPath.forEachLine { line ->
                if (!commentLinePattern.matches(line)) {
                    pattern.findAll(line).forEach {
                        yield(FilePosition.from(lineNr, it.range.first + 1))
                    }
                }
                lineNr++
            }
        }

    val pattern = if (specPath.name.endsWith(".json")) "\"$property\"" else "(^|[ {])$property:"
    val filePosition = findAllMatches(Regex(pattern)).elementAt(index)
    return FileSource.from(specPath.toFile(), filePosition)
}
