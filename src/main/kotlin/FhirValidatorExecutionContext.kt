package no.nav

import org.junit.platform.engine.EngineExecutionListener
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext

data class FhirValidatorExecutionContext(
    val listener: EngineExecutionListener,
    val validator: FhirValidator? = null
) : EngineExecutionContext
