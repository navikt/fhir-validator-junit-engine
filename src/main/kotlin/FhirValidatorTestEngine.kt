package no.nav

import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.DirectorySelector
import org.junit.platform.engine.discovery.FileSelector
import org.junit.platform.engine.support.config.PrefixedConfigurationParameters
import org.junit.platform.engine.support.hierarchical.ForkJoinPoolHierarchicalTestExecutorService
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService
import java.nio.file.Files
import kotlin.io.path.name
import kotlin.streams.asSequence

private const val PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME = "no.nav.execution.parallel.enabled"
private const val PARALLEL_CONFIG_PREFIX = "no.nav.execution.parallel.config."

class FhirValidatorTestEngine : HierarchicalTestEngine<FhirValidatorExecutionContext>() {
    // See https://junit.org/junit5/docs/current/user-guide/#launcher-api-engines-custom
    override fun getId() = "fhir-validator-junit-engine"

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        val specFiles = discoveryRequest.run {
            Color.disableAnsiColors = configurationParameters.disableAnsiColors()
            val fileExt = listOf("json", "yml", "yaml").map { "${configurationParameters.testFilePostfix()}.$it" }

            val files = getSelectorsByType(DirectorySelector::class.java)
                .map { it.path }
                .flatMap { Files.walk(it).asSequence() }
                .filter { fileExt.any { ext -> it.name.endsWith(ext, ignoreCase = true) } }

            files + getSelectorsByType(FileSelector::class.java).map { it.path }
        }

        return EngineDescriptorFactory.create(uniqueId, specFiles)
    }

    override fun createExecutionContext(request: ExecutionRequest) =
        FhirValidatorExecutionContext(request.engineExecutionListener)

    override fun createExecutorService(request: ExecutionRequest): HierarchicalTestExecutorService =
        if (request.configurationParameters.getBoolean(PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME).orElse(false)) {
            val config = PrefixedConfigurationParameters(request.configurationParameters, PARALLEL_CONFIG_PREFIX)
            ForkJoinPoolHierarchicalTestExecutorService(config)
        } else super.createExecutorService(request)
}

// See https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params
private fun ConfigurationParameters.disableAnsiColors() = get("no.nav.disable-ansi-colors").orElse("false").toBoolean()
private fun ConfigurationParameters.testFilePostfix() = get("no.nav.postfix").orElse("test")
