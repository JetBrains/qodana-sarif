###Run Configurations

#Docker Image for Qodana IntelliJ Linter SARIF converter

### Options

```
Available options are:
 -o,--output <arg>        Output directory for report files
 -s,--sarif-path <arg>   Path to SARIF file
 -v,--verbose            Enable verbose logging
```

###Examples

```shell script
docker run -v <sarif-path>:/data/sarif.json -v <output>:/data/output docker-registry.labs.intellij.net/sarif-converter
```

```shell script
docker run -v <sarif-path>:/data/sarif.json\
              <output>:/data/output\
              docker-registry.labs.intellij.net/jetbrains-analytics-converter
              -s /data/sarif.json
              -o /data/output
              -v
``` 
Would be generated both file: `result-allProblems.json` and `metaInformation.json`

#Qodana IntelliJ Linter SARIF converter (Jar archive)

* for UNIX/Mac OS use `gradlew`
* for Windows use `gradlew.bat`


```shell script
./gradlew :sarif-converter:fatJar
```

The built jar will lie: `sarif-converter/build/libs/sarifConverter.jar`

Help:
```
usage: java -jar jarName [-help] [-o <arg>] -s <arg> [-v]
Standard commands:
 -help,--help             Print program options
 -o,--output <arg>        Output directory for report files
 -s,--sarif-path <arg>   Path to SARIF file
 -v,--verbose            Enable verbose logging
Please report issues at https://youtrack.jetbrains.com/issues/QD
```

P.S. `-o` default current path 



# sarif results view to Qodana UI view dates mapping

For `result-allProblems.json`
```
Problem:
    var tool: String                       ->   runs[0].tool.driver.fullName
    var category: String                   ->   runs[0].results[0].ruleId find in extensions runs[0].tool.extensions[0].rules[0].id then runs[0].tool.extensions[0].rules[0].relationships[0].target.id by this find  name would be category
    var type: String                       ->   runs[0].tool.extensions[0].rules[0].shortDescription.text
    var tags: MutableList<String>?         ->   empty
    var comment: String                    ->   runs[0].results[0].message.text
    var severity: Severity                 ->   runs[0].results[0].properties.ideaSeverity or (runs[0].results[0].level)
    var detailsInfo: String                ->   runs[0].tool.extensions[0].rules[0].fullDescription.text
    var attributes: Map<String, String>?   ->   <inspectionName, runs[0].results[0].ruleId> + <module, runs[0].results[0].locations[0].logicalLocations[0].fullyQualifiedName>
    var sources: MutableList<Source>       ->   see down
    var hash: Long                         ->   hash Long by runs[0].results[0].partialFingerprints.equalIndicator/v1


Source:
    var type: String,                          ->  "where I can get type..."
    var path: String,                          ->  runs[0].results[0].locations[0].physicalLocation.artifactLocation.uri
    var language: String? = null,              ->  runs[0].results[0].locations[0].physicalLocation.region.sourceLanguage
    var line: Int? = null,                     ->  runs[0].results[0].locations[0].physicalLocation.region.startLine
    var offset: Int? = null,                   ->  runs[0].results[0].locations[0].physicalLocation.region.startColumn
    var length: Int? = null,                   ->  runs[0].results[0].locations[0].physicalLocation.region.charLength
    var code: Code? = null,                    ->  see down
    var attributes: Map<String, Any>? = null   ->  empty


Code:
    var startLine: Int,           ->   runs[0].results[0].locations[0].physicalLocation.contextRegion.startLine
    var length: Int,              ->   runs[0].results[0].locations[0].physicalLocation.contextRegion.charLength
    var offset: Int,              ->   runs[0].results[0].locations[0].physicalLocation.contextRegion.charOffset
    var surroundingCode: String   ->   runs[0].results[0].locations[0].physicalLocation.contextRegion.snippet.text

```

For `metaInformation.json`
```
{
    "total": 64,                                                            -> runs[0].results.size (without Unhandled problems)
    "tools inspection": {
        "Code Inspection": 64                                                    ->  <runs[0].tool.driver.fullName, similar total with> 
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
```
