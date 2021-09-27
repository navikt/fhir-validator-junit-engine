package no.nav

import org.hl7.fhir.r5.model.ImplementationGuide
import org.hl7.fhir.r5.model.OperationOutcome
import org.hl7.fhir.r5.model.StringType
import org.hl7.fhir.r5.model.StructureDefinition
import org.hl7.fhir.r5.utils.ToolingExtensions
import org.hl7.fhir.utilities.TimeTracker
import org.hl7.fhir.utilities.VersionUtilities
import org.hl7.fhir.validation.ValidationEngine
import org.hl7.fhir.validation.cli.model.CliContext
import org.hl7.fhir.validation.cli.services.ValidationService
import org.hl7.fhir.validation.cli.utils.Params
import org.hl7.fhir.validation.cli.utils.QuestionnaireMode
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path

class FhirValidator(private val validationEngine: ValidationEngine) {
    fun validate(source: Path, profile: String?): OperationOutcome {
        val outcome = validationEngine.validate(source.toString(), listOf(profile).mapNotNull { it })
        return withPositionFileSources(outcome)
    }

    companion object {
        private val service = ValidationService()
        private val cache = ConcurrentHashMap<Specification.Validator, FhirValidator>()

        fun create(spec: Specification.Validator): FhirValidator {
            return cache.getOrPut(spec) {
                val ctx = spec.toCLIContext()
                if (ctx.sv == null) ctx.sv = service.determineVersion(ctx)

                val packageName = VersionUtilities.packageForVersion(ctx.sv)
                val version = VersionUtilities.getCurrentVersion(ctx.sv)

                val engine = service.initializeValidator(ctx, "$packageName#$version", TimeTracker())

                ctx.profiles.forEach {
                    if (!engine.context.hasResource(StructureDefinition::class.java, it) &&
                        !engine.context.hasResource(ImplementationGuide::class.java, it)
                    ) {
                        println("  Fetch Profile from $it")
                        engine.loadProfile(ctx.locations.getOrDefault(it, it))
                    }
                }

                FhirValidator(engine)
            }
        }
    }
}

private fun withPositionFileSources(outcome: OperationOutcome): OperationOutcome {
    val file = outcome.getExtensionByUrl(ToolingExtensions.EXT_OO_FILE)?.valueStringType?.value
    if (file != null) {
        outcome.issue.forEach {
            val line = it.getExtensionByUrl(ToolingExtensions.EXT_ISSUE_LINE)?.valueIntegerType?.value
            val column = it.getExtensionByUrl(ToolingExtensions.EXT_ISSUE_COL)?.valueIntegerType?.value

            var fileUrl = Path(file).toUri().toString()
            line?.let {
                fileUrl += ":$line"
                column?.let { fileUrl += ":$column" }
            }

            listOf(
                ToolingExtensions.EXT_ISSUE_LINE,
                ToolingExtensions.EXT_ISSUE_COL,
                ToolingExtensions.EXT_ISSUE_SOURCE
            ).forEach { extUrl -> it.removeExtension(extUrl) }

            it.addExtension(ToolingExtensions.EXT_ISSUE_SOURCE, StringType(fileUrl))
        }
    }

    return outcome
}

private fun Specification.Validator.toCLIContext(): CliContext {
    val args = mutableListOf<String>()

    args.add(Params.STRICT_EXTENSIONS)
    args.addAll(listOf(Params.QUESTIONNAIRE, QuestionnaireMode.REQUIRED.name))
    args.addAll(listOf(Params.TERMINOLOGY, tx ?: "n/a"))

    ig.forEach { args.addAll(listOf(Params.IMPLEMENTATION_GUIDE, it)) }
    version?.let { args.addAll(listOf(Params.VERSION, it)) }
    sct?.let { args.addAll(listOf(Params.SCT, it)) }
    txLog?.let { args.addAll(listOf(Params.TERMINOLOGY_LOG, it)) }

    return Params.loadCliContext(args.toTypedArray())
}
