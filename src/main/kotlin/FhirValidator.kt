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
    fun validate(source: Path, profile: String?) =
        validationEngine
            .validate(source.toString(), listOf(profile).mapNotNull { it })
            .apply { issue.removeIf { it.details?.text == "All OK" } }
            .withPositionFileSources()

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

/** Rewrites the issue-source extension values to format: file:///{filePath}:{line}:{column} */
private fun OperationOutcome.withPositionFileSources(): OperationOutcome {
    val file = getExtensionByUrl(ToolingExtensions.EXT_OO_FILE)?.valueStringType?.value
    if (file != null) {
        issue.forEach {
            val line = it.getExtensionByUrl(ToolingExtensions.EXT_ISSUE_LINE)?.valueIntegerType?.value
            val column = it.getExtensionByUrl(ToolingExtensions.EXT_ISSUE_COL)?.valueIntegerType?.value

            var fileUrl = Path(file).toUri().toString()

            if (line != null) {
                fileUrl += ":$line"
                if (column != null) fileUrl += ":$column"
            }

            it.removeExtension(ToolingExtensions.EXT_ISSUE_SOURCE)
            it.addExtension(ToolingExtensions.EXT_ISSUE_SOURCE, StringType(fileUrl))
        }
    }

    return this
}

private fun Specification.Validator.toCLIContext(): CliContext {
    val args = mutableListOf<String>()

    fun addArg(key: String, value: String?) = value?.let { args.addAll(listOf(key, value)) }

    ig.forEach { addArg(Params.IMPLEMENTATION_GUIDE, it) }
    args.add(Params.STRICT_EXTENSIONS)
    addArg(Params.VERSION, version)
    addArg(Params.TERMINOLOGY, tx ?: "n/a")
    addArg(Params.TERMINOLOGY_LOG, txLog)
    addArg(Params.SCT, sct)
    addArg(Params.QUESTIONNAIRE, QuestionnaireMode.REQUIRED.name)

    return Params.loadCliContext(args.toTypedArray())
}
