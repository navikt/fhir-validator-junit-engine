package no.nav

import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.hierarchical.Node

class TestSuiteDescriptor(
    id: UniqueId,
    private val spec: Specification,
    source: TestSource
) : AbstractTestDescriptor(id, spec.title, source), Node<FhirValidatorExecutionContext> {
    override fun getType() = TestDescriptor.Type.CONTAINER
    override fun execute(
        context: FhirValidatorExecutionContext,
        dynamicTestExecutor: Node.DynamicTestExecutor
    ) = try {
        println(Color.TITLE.paint("# SUITE: $displayName"))
        context.copy(validator = FhirValidator.create(spec.validator))
    } catch (ex: Throwable) {
        context.listener.reportingEntryPublished(this, createReportEntry(spec.validator))
        println()
        throw ex
    } finally {
        println()
    }
}

private fun createReportEntry(spec: Specification.Validator) =
    spec.run {
        val values = mapOf(
            Pair("version", version),
            Pair("tx", tx),
            Pair("txLog", txLog),
            Pair("sct", sct),
            Pair("ig", ig.joinToString())
        ).filterValues { !it.isNullOrEmpty() }

        ReportEntry.from(values)
    }
