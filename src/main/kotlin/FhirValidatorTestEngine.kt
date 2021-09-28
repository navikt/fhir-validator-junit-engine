package no.nav

import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.EngineDiscoveryRequest
import org.junit.platform.engine.ExecutionRequest
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.discovery.FileSelector
import org.junit.platform.engine.support.config.PrefixedConfigurationParameters
import org.junit.platform.engine.support.hierarchical.ForkJoinPoolHierarchicalTestExecutorService
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine
import org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutorService
import java.nio.file.Paths

private const val PARALLEL_EXECUTION_ENABLED_PROPERTY_NAME = "no.nav.execution.parallel.enabled"
private const val PARALLEL_CONFIG_PREFIX = "no.nav.execution.parallel.config."

class FhirValidatorTestEngine : HierarchicalTestEngine<FhirValidatorExecutionContext>() {
    // See https://junit.org/junit5/docs/current/user-guide/#launcher-api-engines-custom
    override fun getId() = "fhir-validator-junit-engine"

    override fun discover(discoveryRequest: EngineDiscoveryRequest, uniqueId: UniqueId): TestDescriptor {
        Color.disableAnsiColors = discoveryRequest.configurationParameters.disableAnsiColors()

        val patterns = discoveryRequest.getSelectorsByType(FileSelector::class.java).map { it.rawPath }
        val specFiles = Paths.get("").globFileWalk(patterns).toList()

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
