package com.jetbrains.qodana.sarif

import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jetbrains.qodana.sarif.model.Level.*
import com.jetbrains.qodana.sarif.model.ReportingDescriptor
import com.jetbrains.qodana.sarif.model.Result
import com.jetbrains.qodana.sarif.model.SarifReport
import com.jetbrains.qodana.sarif.model.Tool
import org.apache.logging.log4j.LogManager
import org.jetbrains.teamcity.qodana.json.Severity
import org.jetbrains.teamcity.qodana.json.version3.Code
import org.jetbrains.teamcity.qodana.json.version3.SimpleProblem
import org.jetbrains.teamcity.qodana.json.version3.Source
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.rmi.UnexpectedException

@Suppress("UnstableApiUsage")
class SarifConverterImpl : SarifConverter {
    private val hasher: Hasher
        get() = Hashing.sha256().newHasher()

    override fun convert(sarifReport: SarifReport, output: Path) {
        log.info("sarif file version: ${sarifReport.version}")

        val problems = mutableListOf<SimpleProblem>()
        val metaInformation = MetaInformation()
        var lostProblems = 0

        sarifReport.runs.firstOrNull()?.run {
            val toolName = tool.driver.fullName

            results.forEach { result ->
                try {
                    val rule = tool.findRuleById(result.ruleId)

                    problems.add(emptySimpleProblem().apply {
                        tool = toolName
                        comment = result.message.text
                        hash = result.hash()
                        type = rule.shortDescription.text
                        detailsInfo = rule.fullDescription.text
                        category = rule.relationships.first().target.id
                        severity = result.severity()
                        attributes = result.attributes()
                        sources = result.sources()
                    })
                } catch (exception: Exception) {
                    lostProblems++
                    log.error(exception.message)
                    exception.stackTrace.forEach { log.error(it.toString()) }
                }
            }

            metaInformation.run {
                versionControlProvenance.firstOrNull()?.run {
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

        log.info("Amount problems: ${problems.size}")
        if (lostProblems != 0) run { log.info("Unhandled problems: $lostProblems") }
        log.info("Writing result-allProblems.json")
        Files.newBufferedWriter(output.resolve("result-allProblems.json"), StandardCharsets.UTF_8).use { writer ->
            gson.toJson(mapOf("version" to "3", "listProblem" to problems), writer)
        }


        log.info("Writing metaInformation.json")
        Files.newBufferedWriter(output.resolve("metaInformation.json"), StandardCharsets.UTF_8).use { writer ->
            gson.toJson(metaInformation, writer)
        }
    }

    override fun convert(sarifFile: File, output: Path) {
        val sarifReport = SarifUtil.readReport(sarifFile.toPath())
        convert(sarifReport, output)
    }


    private fun Result.sources(): MutableList<Source> {
        return mutableListOf<Source>().apply {
            locations.forEach { location ->
                add(
                    Source(
                        "none",
                        location.physicalLocation.artifactLocation.uri,
                        location.physicalLocation.region.sourceLanguage,
                        location.physicalLocation.region.startLine,
                        location.physicalLocation.region.startColumn,
                        location.physicalLocation.region.charLength,
                        Code(
                            location.physicalLocation.contextRegion.startLine,
                            location.physicalLocation.contextRegion.charLength,
                            location.physicalLocation.contextRegion.charOffset,
                            location.physicalLocation.contextRegion.snippet.text
                        ),
                        null
                    )
                )
            }
        }
    }

    private fun Result.attributes(): MutableMap<String, String> {
        return mutableMapOf<String, String>().apply {
            put("inspectionName", ruleId)
            put("module", locations.first().logicalLocations.first().fullyQualifiedName)
        }
    }


    private fun Result.severity(): Severity {
        return properties["ideaSeverity"]?.run {
            when (this) {
                "ERROR" -> Severity.ERROR
                "WARNING" -> Severity.WARNING
                "WEAK WARNING" -> Severity.WEAK_WARNING
                "TYPO" -> Severity.TYPO
                "INFORMATION" -> Severity.INFORMATION
                else -> throw InvalidSarifException("Unexpected severity type: $this")
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


    private fun Tool.findRuleById(ruleId: String): ReportingDescriptor {
        return extensions.first { toolComponent ->
            toolComponent.rules.any { reportingDescriptor ->
                reportingDescriptor.id == ruleId
            }
        }.run {
            rules.first { it.id == ruleId }
        }
    }

    private fun Result.hash(): Long {
        val (key, index) = "equalIndicator" to 1
        return hasher.putUnencodedChars(partialFingerprints.get(key, index)).hash().asLong()
    }

    private fun emptySimpleProblem() = SimpleProblem("", "", "", null, Severity.TYPO, "", "", mutableListOf(), null, 0)

    companion object {
        private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        private val log = LogManager.getLogger(SarifConverterImpl::class.java)!!
    }
}

/*
need results-allProblems.json and metaInformation.json
*/

/*
    var type: String,                                   ->   "where I can get type..."
    var path: String,                                   ->   runs[0].results[0].locations[0].physicalLocation.artifactLocation.uri
    var language: String? = null,                       ->   runs[0].results[0].locations[0].physicalLocation.region.sourceLanguage
    var line: Int? = null,                              ->   runs[0].results[0].locations[0].physicalLocation.region.startLine
    var offset: Int? = null,                            ->   runs[0].results[0].locations[0].physicalLocation.region.startColumn
    var length: Int? = null,                            ->   runs[0].results[0].locations[0].physicalLocation.region.charLength
    var code: Code? = null,                             ->   see down
    var attributes: Map<String, Any>? = null            ->   empty
*/


/*
!   var tool: String                        ->   runs[0].tool.driver.fullName
!   var category: String                    ->   runs[0].results[0].ruleId find in extensions runs[0].tool.extensions[0].rules[0].id then runs[0].tool.extensions[0].rules[0].relationships[0].target.id by this find  name would be category
!   var type: String                        ->   runs[0].tool.extensions[0].rules[0].shortDescription.text
!   var tags: MutableList<String>?          ->   empty
!   var comment: String                     ->   runs[0].results[0].message.text
!   var severity: Severity                  ->   runs[0].results[0].properties.ideaSeverity or (runs[0].results[0].level)
!   var detailsInfo: String                 ->   runs[0].tool.extensions[0].rules[0].fullDescription.text
!   var attributes: Map<String, String>?    ->   <inspectionName, runs[0].results[0].ruleId> + <module, runs[0].results[0].locations[0].logicalLocations[0].fullyQualifiedName>
    var sources: MutableList<Source>        ->   see down
!   var hash: Long                          ->   hash Long by runs[0].results[0].partialFingerprints.equalIndicator/v1
*/


/*

    var startLine: Int,                    ->    runs[0].results[0].locations[0].physicalLocation.contextRegion.startLine
    var length: Int,                       ->    runs[0].results[0].locations[0].physicalLocation.contextRegion.charLength
    var offset: Int,                       ->    runs[0].results[0].locations[0].physicalLocation.contextRegion.charOffset
    var surroundingCode: String            ->    runs[0].results[0].locations[0].physicalLocation.contextRegion.snippet.text
*/

//result-allProblems.json


//metaInformation.json

/*
{
    "total": 64,                                                                 ->   total is total =)
    "tools inspection": {
        "Code Inspection": 64                                                    ->   tool name
    },
    "attributes": {
        "vcs": {
            "sarifIdea": {
                "repositoryUri": "https://github.com/niki999922/Test.git",       ->    runs[0].versionControlProvenance[0].repositoryUri
                "revisionId": "1029b30d570577b23197e12be883fdfc1c1cb86a",        ->    runs[0].versionControlProvenance[0].revisionId
                "branch": "master"                                               ->    runs[0].versionControlProvenance[0].branch
            }
        }
    }
}
*/