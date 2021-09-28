import no.nav.FhirValidatorTestEngine
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectFile
import org.junit.platform.launcher.TagFilter
import org.junit.platform.testkit.engine.EngineTestKit

class FhirValidatorTestEngineTest {
    @Test
    fun `Given a directory with a json test file, tests should be discovered and executed`() {
        EngineTestKit
            .engine(FhirValidatorTestEngine())
            .selectors(selectFile("src/test/resources/subdir/**/*.json"))
            .filters(TagFilter.excludeTags("with-profile"))
            .execute()
            .testEvents()
            .assertStatistics {
                it.started(3).succeeded(1).failed(2).aborted(0).skipped(0)
            }
    }

    @Test
    fun `Filtered by tag, test should validate using meta-profile`() {
        EngineTestKit
            .engine(FhirValidatorTestEngine())
            .selectors(selectFile("src/test/resources/subdir/second-subdir/**"))
            .filters(TagFilter.includeTags("with-profile"))
            .execute()
            .testEvents()
            .assertStatistics {
                it.started(1).succeeded(1).failed(0).aborted(0).skipped(0)
            }
    }

    @Test
    fun `Given a yaml test file, tests should be discovered and executed`() {
        EngineTestKit
            .engine(FhirValidatorTestEngine())
            .selectors(selectFile("src/test/resources/simple.test.yaml"))
            .execute()
            .testEvents()
            .assertStatistics {
                it.started(1).succeeded(1).failed(0).aborted(0).skipped(0)
            }
    }

    @Test
    fun `Given a test with questionnaire-response as source, it should be validated against its questionnaire`() {
        EngineTestKit
            .engine(FhirValidatorTestEngine())
            .selectors(selectFile("src/test/resources/questionnaire.test.yaml"))
            .execute()
            .testEvents()
            .assertStatistics {
                it.started(2).succeeded(1).failed(1).aborted(0).skipped(0)
            }
    }

    @Test
    fun `Given a test with glob paths, should generate tests`() {
        EngineTestKit
            .engine(FhirValidatorTestEngine())
            .selectors(selectFile("src/test/resources/glob-pattern.test.yaml"))
            .configurationParameter("no.nav.execution.parallel.enabled", "true")
            .configurationParameter("no.nav.disable-ansi-colors", "true")
            .execute()
            .testEvents()
            .assertStatistics {
                it.started(1).succeeded(1).failed(0).aborted(0).skipped(0)
            }
    }

    @Test
    fun `Given a test invalid validator config, test-suite should fail`() {
        EngineTestKit
            .engine(FhirValidatorTestEngine())
            .selectors(selectFile("src/test/resources/invalid.test.yaml"))
            .execute()
            .testEvents()
            .assertStatistics {
                it.started(0)
            }
    }
}
