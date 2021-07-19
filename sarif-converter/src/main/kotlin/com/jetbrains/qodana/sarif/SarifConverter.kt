package com.jetbrains.qodana.sarif

import org.apache.logging.log4j.LogManager
import org.jetbrains.teamcity.qodana.json.Severity
import org.jetbrains.teamcity.qodana.json.version3.SimpleProblem
import java.io.File
import java.nio.file.Path

class SarifConverter(private val sarifFile: File) {
    fun convert(output: Path) {
        val sarif = SarifUtil.readReport(sarifFile.toPath())

//        sarif.runs.get(0).results.forEach {  }
        val tool = sarif.runs[0].tool.driver.fullName
        val res1 = sarif.runs.get(0).results.get(4)
        var expected = SimpleProblem(
            tool = tool,
            category = "",
            type = "",
            tags = emptyList<String>().toMutableList(),
            severity = Severity.ERROR,
            detailsInfo = "string",
            comment = "as",
            sources = mutableListOf(),
            attributes = mutableMapOf(),
            hash = 123

        )
        log.info("sarif file version: ${sarif.version}")
//        sarif.runs
    }

    companion object {
        private val log = LogManager.getLogger(SarifConverter::class.java)!!
    }
}




/*
need results-allProblems.json and metaInformation.json
*/

/*
    var tool: String                        ->   runs[0].tool.driver.fullName
    var category: String                    ->   runs[0].results[0].ruleId find in extensions runs[0].tool.extensions[0].rules[0].id then runs[0].tool.extensions[0].rules[0].relationships[0].target.id by this find  name would be category
    var type: String                        ->   runs[0].tool.extensions[0].rules[0].shortDescription.text
    var tags: MutableList<String>?          ->   empty
    var comment: String                     ->   runs[0].results[0].message.text
    var severity: Severity                  ->   runs[0].results[0].properties.ideaSeverity or (runs[0].results[0].level)
    var detailsInfo: String                 ->   runs[0].tool.extensions[0].rules[0].fullDescription.text
    var attributes: Map<String, String>?    ->   <inspectionName, runs[0].results[0].ruleId> + <module, runs[0].results[0].locations[0].logicalLocations[0].fullyQualifiedName>
    var sources: MutableList<Source>        ->   see down
    var hash: Long                          ->   hash Long by runs[0].results[0].partialFingerprints.equalIndicator/v1
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