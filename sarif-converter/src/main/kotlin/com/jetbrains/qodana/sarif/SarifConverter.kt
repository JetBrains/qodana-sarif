package com.jetbrains.qodana.sarif

import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import com.jetbrains.qodana.sarif.app.CLIProducer.gson
import com.jetbrains.qodana.sarif.model.Level.*
import com.jetbrains.qodana.sarif.model.ReportingDescriptor
import com.jetbrains.qodana.sarif.model.Result
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
import java.util.*

@Suppress("UnstableApiUsage")
class SarifConverter(private val sarifFile: File) {
    private val hasher: Hasher
        get() = Hashing.sha256().newHasher()

    fun convert(output: Path) {
        val sarif = SarifUtil.readReport(sarifFile.toPath())
        log.info("sarif file version: ${sarif.version}")

        val problems = mutableListOf<SimpleProblem>()
        sarif.runs.firstOrNull()?.run {
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
                    log.error(exception.message)
                    exception.stackTrace.forEach { log.error(it.toString()) }
                }
            }
        }

        log.info("Amount handle problems: ${problems.size}")
        log.info("Writing result-allProblems.json...")
        Files.newBufferedWriter(output.resolve("result-allProblems.json"), StandardCharsets.UTF_8).use { writer ->
            gson.toJson(mapOf("version" to "3", "listProblem" to problems), writer)
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
                else -> throw SarifQodanaException("Unexpected severity type: $this")
            }
        } ?: run {
            when (level) {
                NONE -> Severity.INFORMATION
                NOTE -> Severity.INFORMATION
                WARNING -> Severity.WARNING
                ERROR -> Severity.ERROR
                else -> throw SarifQodanaException("Can't handle level of problem: $level")
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

    private fun Result.sources(): MutableList<Source> {
        return mutableListOf<Source>().apply {
            locations.forEach { location ->
                add(Source(
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
                ))
            }
        }
    }

    companion object {
        private val log = LogManager.getLogger(SarifConverter::class.java)!!
    }
}

class SarifQodanaException(message: String) : InvalidPropertiesFormatException(message)

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