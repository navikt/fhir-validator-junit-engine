package no.nav

import com.github.shyiko.klob.Glob
import org.hl7.fhir.r5.model.OperationOutcome
import org.hl7.fhir.r5.utils.ToolingExtensions
import java.nio.file.Path
import kotlin.io.path.isDirectory

typealias Severity = OperationOutcome.IssueSeverity
typealias IssueType = OperationOutcome.IssueType
typealias IssueComponent = OperationOutcome.OperationOutcomeIssueComponent

fun IssueComponent.toData() = Specification.Issue(severity, code, expression.firstOrNull()?.asStringValue(), details.text)
fun IssueComponent.sourceUrl() = getExtensionByUrl(ToolingExtensions.EXT_ISSUE_SOURCE)?.valueStringType?.value
fun Severity.failure() = this in listOf(Severity.FATAL, Severity.ERROR)

/** Uses .gitignore glob-pattern to recursively iterate and select file-paths. */
fun Path.globFileWalk(patterns: List<String>): Sequence<Path> {
    val baseDir = toAbsolutePath().run { if (isDirectory()) this else parent }
    return Glob.from(*patterns.toTypedArray()).iterate(baseDir).asSequence()
}
