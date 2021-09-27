package no.nav

import org.hl7.fhir.r5.model.OperationOutcome
import org.hl7.fhir.r5.utils.ToolingExtensions

typealias Severity = OperationOutcome.IssueSeverity
typealias IssueType = OperationOutcome.IssueType
typealias IssueComponent = OperationOutcome.OperationOutcomeIssueComponent

fun IssueComponent.toData() = Specification.Issue(severity, code, expression.firstOrNull()?.asStringValue(), details.text)
fun IssueComponent.sourceUrl() = getExtensionByUrl(ToolingExtensions.EXT_ISSUE_SOURCE)?.valueStringType?.value
fun Severity.failure() = this in listOf(Severity.FATAL, Severity.ERROR)
