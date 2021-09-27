package no.nav

import org.hl7.fhir.r5.model.OperationOutcome
import org.opentest4j.AssertionFailedError

class UnexpectedIssue(val issueSpec: Specification.Issue, val source: String?) : AssertionFailedError("Unexpected $issueSpec at $source") {
    companion object {
        fun test(testSpec: Specification.TestCase, outcome: OperationOutcome): List<UnexpectedIssue> {
            val unexpectedErrorFailures = outcome.issue
                .map { UnexpectedIssue(it.toData(), it.sourceUrl()) }
                .filter { it.issueSpec.severity.failure() }
                .filterNot { testSpec.expectedIssues.any { expected -> expected.semanticallyEquals(it.issueSpec) } }

            val color = if (unexpectedErrorFailures.isEmpty()) Color.SUCCESSFUL else Color.FAILED
            println(color.paint("  ${unexpectedErrorFailures.count()} unexpected errors!"))

            return unexpectedErrorFailures
        }
    }
}

class MissingIssue(val issueSpec: Specification.Issue) : AssertionFailedError("Expected issue was not found: $issueSpec.") {
    companion object {
        fun test(testSpec: Specification.TestCase, outcome: OperationOutcome): List<MissingIssue> {
            val issues = outcome.issue.map { it.toData() }

            val missingIssueFailures = testSpec.expectedIssues
                .filterNot { expected -> issues.any { expected.semanticallyEquals(it) } }
                .map { MissingIssue(it) }

            val foundCount = testSpec.expectedIssues.count() - missingIssueFailures.count()
            val color = if (foundCount == testSpec.expectedIssues.count()) Color.SUCCESSFUL else Color.FAILED
            println(color.paint("  Found $foundCount of ${testSpec.expectedIssues.count()} expected issues!"))

            return missingIssueFailures
        }
    }
}
