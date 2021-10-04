package com.jetbrains.qodana.sarif

import com.google.common.hash.Hashing
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jetbrains.qodana.sarif.TextUtil.Companion.sanitizeText
import com.jetbrains.qodana.sarif.model.*
import com.jetbrains.qodana.sarif.model.Level.*
import com.jetbrains.qodana.sarif.model.ResultAllProblems.Companion.emptySimpleProblem
import org.apache.logging.log4j.LogManager
import org.jetbrains.teamcity.qodana.json.Severity
import org.jetbrains.teamcity.qodana.json.version3.Code
import org.jetbrains.teamcity.qodana.json.version3.SimpleProblem
import org.jetbrains.teamcity.qodana.json.version3.Source
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.rmi.UnexpectedException
import kotlin.Exception
import kotlin.random.Random

@Suppress("UnstableApiUsage")
class SarifConverterImpl : SarifConverter {
    companion object {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        private val log = LogManager.getLogger(SarifConverterImpl::class.java)!!

        fun Result.severity(): Severity {
            return properties?.get("ideaSeverity")?.run {
                when (this) {
                    "ERROR" -> Severity.ERROR
                    "WARNING" -> Severity.WARNING
                    "WEAK WARNING" -> Severity.WEAK_WARNING
                    "TYPO" -> Severity.TYPO
                    "INFORMATION" -> Severity.INFORMATION
                    "INFO" -> Severity.INFORMATION
                    else -> Severity.INFORMATION.also {
                        log.info("Unexpected severity type: $this, used INFORMATION")
                    }
                }
            } ?: run {
                when (level) {
                    NONE -> Severity.INFORMATION
                    NOTE -> Severity.INFORMATION
                    WARNING -> Severity.WARNING
                    ERROR -> Severity.ERROR
                    else -> throw InvalidSarifException("Can't handle level of problem: $level")
                }
            }
        }
    }

    private val hasher: NullableHasher
        get() = NullableHasher(Hashing.sha256().newHasher())

    fun convert(sarifFile: File, output: Path): List<File> {
        val sarifReport = SarifUtil.readReport(sarifFile.toPath())
        log.info("sarif file version: ${sarifReport.version}")

        val (metaInformation, resultAllProblems) = convert(sarifReport)
        val writtenFiles = mutableListOf<File>()

        log.info("Amount problems: ${resultAllProblems.listProblem.size}")
        if (sarifReport.runs.firstOrNull()?.results?.size != resultAllProblems.listProblem.size) {
            val lostProblems = sarifReport.runs.firstOrNull()?.results?.size?.minus(resultAllProblems.listProblem.size)
            log.info("Unhandled problems: $lostProblems")
        }

        log.info("Writing result-allProblems.json")
        output.resolve("result-allProblems.json").toFile().let {
            it.writeResult(resultAllProblems)
            writtenFiles.add(it)
        }

        log.info("Writing metaInformation.json")
        output.resolve("metaInformation.json").toFile().let {
            it.writeResult(metaInformation)
            writtenFiles.add(it)
        }

        return writtenFiles
    }

    override fun convert(sarifFile: File): Pair<MetaInformation, ResultAllProblems> {
        val sarifReport = SarifUtil.readReport(sarifFile.toPath())
        return convert(sarifReport)
    }

    private fun File.writeResult(src: Any) {
        bufferedWriter(StandardCharsets.UTF_8).use { gson.toJson(src, it) }
    }

    private fun convert(sarifReport: SarifReport): Pair<MetaInformation, ResultAllProblems> {
        val problems = mutableListOf<SimpleProblem>()
        val metaInformation = MetaInformation()
        var lostProblems = 0

        sarifReport.runs.firstOrNull()?.run {
            val toolName = tool.driver.fullName ?: tool.driver.name

            results.forEach { result ->
                try {
                    val rule = tool.findRuleById(result.ruleId)
                    val driver = tool.driver

                    problems.add(emptySimpleProblem().apply {
                        tool = toolName
                        comment = sanitizeText(result.message?.text ?: "")
                        hash = result?.hash() ?: Random.nextLong().toString()
                        type = rule.shortDescription?.text ?: ""
                        detailsInfo = rule.fullDescription?.text ?: ""
                        category = rule.relationships?.first()?.target?.id
                                ?.let { id -> driver.taxa.first { it.id == id }.name }
                                ?: ""
                        severity = result.severity()
                        attributes = result.attributes()
                        sources = result.sources()
                    })
                } catch (exception: Exception) {
                    lostProblems++
                    log.error(exception)
                    exception.stackTrace.forEach { log.error(it.toString()) }
                }
            }

            metaInformation.run {
                versionControlProvenance?.firstOrNull()?.run {
                    attributes = mapOf(
                        "vcs" to mapOf(
                            "sarifIdea" to mapOf(
                                "repositoryUri" to repositoryUri,
                                "revisionId" to revisionId,
                                "branch" to branch
                            )
                        )
                    )
                }
                totalProblem = problems.size
                toolsInspection = mapOf(toolName to problems.size)
            }

        } ?: throw UnexpectedException("sarif have to be contain no less one runs object")

        if (lostProblems > 0 && problems.size == 0) throw InvalidSarifException("all sarif problems have invalid data")

        return metaInformation to ResultAllProblems(listProblem = problems)
    }


    private fun Result.sources(): MutableList<Source> {
        fun type(location: Location): String {
            return location.physicalLocation?.artifactLocation?.uri?.let {
                "file"
            } ?: let {
                location.logicalLocations?.firstOrNull()?.let {
                    "module"
                } ?: let {
                    "no specific module"
                }
            }
        }

        return mutableListOf<Source>().apply {
            locations.forEach { location ->
                add(source(location, type(location)))
            }
        }
    }

    private fun source(location: Location, sourceType: String): Source {
        val physicalLocation = location.physicalLocation
        val contextRegion = physicalLocation?.contextRegion
        val region = physicalLocation?.region

        return Source(
            sourceType,
            physicalLocation?.artifactLocation?.uri ?: "",
            region?.sourceLanguage ?: contextRegion?.sourceLanguage ?: "",
            region?.startLine ?: contextRegion?.startLine ?: 0,
            (region?.startColumn ?: contextRegion?.startColumn?: 0).minus(1).coerceAtLeast(0),
            region?.charLength ?: contextRegion?.charLength ?: 0,
            code(contextRegion, region),
            null
        )
    }


    private fun code(contextRegion: Region?, region: Region?): Code {
        val offset = if (contextRegion?.charOffset != null && region?.charOffset != null) {
            region.charOffset - contextRegion.charOffset
        } else {
            contextRegion?.charOffset ?: region?.charOffset ?: 0
        }

        return Code(
            contextRegion?.startLine ?: region?.startLine ?: 0,
            region?.charLength ?: contextRegion?.charLength ?: 0,
            offset,
            contextRegion?.snippet?.text ?: region?.snippet?.text ?: ""
        )
    }


    private fun Result.attributes(): MutableMap<String, String> {
        return mutableMapOf<String, String>().apply {
            put("module", locations?.firstOrNull()?.logicalLocations?.first()?.fullyQualifiedName ?: "")
            put("inspectionName", ruleId)
            baselineState?.let { if (it.isTechDebt()) put("isTechDebt", "true") }
            baselineState?.let { put("baselineState", it.toString()) }
        }
    }

    private fun Result.BaselineState.isTechDebt() = this == Result.BaselineState.UNCHANGED || this == Result.BaselineState.ABSENT

    private fun Tool.findRuleById(ruleId: String): ReportingDescriptor {
        val driverRules = driver.rules?.asSequence() ?: emptySequence()
        val extensionRules =
            extensions?.asSequence()?.flatMap { it?.rules?.asSequence() ?: emptySequence() } ?: emptySequence()
        try {
            return driverRules.firstOrNull { it.id == ruleId } ?: extensionRules.first { it.id == ruleId }
        } catch (e: NoSuchElementException) {
            log.error("rules sections (driver rules or extension rules) doesn't contain rule with id $ruleId")
            throw e
        }
    }

    private fun Result.hash(): String? {
        val (key, version) = "equalIndicator" to 1
        val partialFingerprint = partialFingerprints?.get(key, version)
        if (!partialFingerprint.isNullOrBlank()) {
            return partialFingerprint
        }
        val fingerprint = fingerprints?.get(key, version)
        val h = hasher
        fingerprint?.let { h.putUnencodedChars(it) }
        partialFingerprint?.let { h.putUnencodedChars(it) }
        return h.hash()?.asBytes()?.toString()
    }
}
