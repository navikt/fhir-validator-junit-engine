package no.nav

import org.hl7.fhir.r5.model.OperationOutcome
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestTag
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.reporting.ReportEntry
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.FileSource
import org.junit.platform.engine.support.hierarchical.Node
import org.opentest4j.AssertionFailedError
import org.opentest4j.MultipleFailuresError
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class TestCaseDescriptor(
    id: UniqueId,
    private val spec: Specification.TestCase,
    private val resource: Path,
    source: FileSource,
) : AbstractTestDescriptor(id, spec.title ?: resource.nameWithoutExtension, source),
    Node<FhirValidatorExecutionContext> {
    override fun getType() = TestDescriptor.Type.TEST
    override fun getTags() = spec.tags.map(TestTag::create).toSet()
    override fun execute(
        context: FhirValidatorExecutionContext,
        dynamicTestExecutor: Node.DynamicTestExecutor
    ): FhirValidatorExecutionContext {
        print(createHeader())

        val outcome = context.validator!!.validate(resource, spec.profile)
        val failures = UnexpectedIssue.test(spec, outcome) + MissingIssue.test(spec, outcome)
        println(createSummary(outcome, failures))

        if (failures.any()) {
            context.listener.reportingEntryPublished(this, createReportEntry())
            throw if (failures.count() == 1) failures.single() else MultipleFailuresError(null, failures)
        }

        return context
    }

    private fun createHeader() =
        StringBuilder().apply {
            appendLine(Color.TITLE.paint("> TEST: $displayName"))
            appendLine("  Location: ${(source.get() as FileSource).toUrl()}")
            if (tags.any()) { appendLine(Color.TAGS.paint("  Tags: ${tags.joinToString { it.name }}")) }
            toString()
        }

    private fun createReportEntry() =
        spec.run {
            val values = mapOf(
                Pair("resource", "${resource.toUri()}"),
                Pair("profile", profile ?: "core"),
                Pair("expectedIssueCount", "${expectedIssues.count()}")
            )

            ReportEntry.from(values)
        }
}

private fun FileSource.toUrl() = "${file.toPath().toUri()}:${position.get().line}:${position.get().column.get()}"

private fun createSummary(outcome: OperationOutcome, failedAssertions: List<AssertionFailedError>) =
    StringBuilder().run {
        val errors = outcome.issue.count { it.severity.failure() }
        val warnings = outcome.issue.count { it.severity == Severity.WARNING }
        val infos = outcome.issue.count { it.severity == Severity.INFORMATION }

        appendLine("  Finished: $errors errors, $warnings warnings, $infos notes")

        val unexpectedIssues = failedAssertions.mapNotNull { (it as? UnexpectedIssue)?.issueSpec }
        val missingIssues = failedAssertions.mapNotNull { (it as? MissingIssue)?.issueSpec }

        outcome.issue.forEachIndexed { i, it ->
            val issue = it.toData()
            val color =
                if (unexpectedIssues.any { mi -> mi.semanticallyEquals(issue) }) Color.FAILED
                else if (!issue.severity.failure()) Color.INFO
                else Color.SUCCESSFUL

            append("${i + 1}", issue, it.sourceUrl(), color)
        }

        missingIssues.forEach {
            append("X", it, "N/A", Color.FAILED)
        }

        toString()
    }

private fun StringBuilder.append(mark: String, issueSpec: Specification.Issue, source: String?, color: Color) {
    appendLine(color.paint("  $mark. Source: $source"))
    appendLine(color.paint("     Severity: ${issueSpec.severity}"))
    appendLine(color.paint("     Type: ${issueSpec.type}"))
    appendLine(color.paint("     Expression: ${issueSpec.expression}"))
    appendLine(color.paint("     Message: ${issueSpec.message}"))
}
