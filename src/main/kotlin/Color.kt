package no.nav

// Inspired by: https://github.com/junit-team/junit5/blob/main/junit-platform-console/src/main/java/org/junit/platform/console/tasks/Color.java
enum class Color(private val ansiCode: Int) {
    NONE(0), RED(31), GREEN(32), YELLOW(33), BLUE(34), CYAN(36);

    override fun toString() = "\u001B[${ansiCode}m"
    fun paint(text: String) = if (disableAnsiColors) text else "$this$text$NONE"

    // Trying to adhere to Junit TestExecutionSummary's coloring-scheme. */
    companion object {
        var disableAnsiColors: Boolean = false

        val TITLE = CYAN
        val TAGS = YELLOW
        val SUCCESSFUL = GREEN
        val FAILED = RED
        val INFO = BLUE
    }
}
