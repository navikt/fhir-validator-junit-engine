package no.nav

data class Specification(
    val title: String?,
    val validator: Validator = Validator(),
    val tests: List<TestCase> = emptyList(),
) {
    data class TestCase(
        val fileMatch: List<String> = emptyList(),
        val title: String? = null,
        val profile: String? = null,
        val expectedIssues: List<Issue> = emptyList(),
        val tags: List<String> = emptyList()
    )

    data class Issue(
        val severity: Severity,
        val type: IssueType?,
        val expression: String?,
        val message: String?
    ) {
        fun semanticallyEquals(other: Issue): Boolean {
            if (severity != other.severity) return false
            if (type != null && type != other.type) return false
            if (expression != null && !expression.contentEquals(other.expression, ignoreCase = true)) return false
            return (message == null || (other.message?.contains(message, ignoreCase = true) == true))
        }
    }

    data class Validator(
        val version: String? = null,
        val tx: String? = null,
        val txLog: String? = null,
        val sct: String? = null,
        val ig: List<String> = emptyList()
    )
}
