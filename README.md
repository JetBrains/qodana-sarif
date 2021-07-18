###Run Configurations

#Docker Image for Qodana IntelliJ Linter SARIF converter

### Options

```
Available options are:
 -o,--output <arg>        Output directory for report files
 -sp,--sarif-path <arg>   Path to SARIF file
 -vb,--verbose            Enable verbose logging
```

###Examples

```shell script
docker run -v <sarif-path>:/data/sarif.json -v <output>:/data/output docker-registry.labs.intellij.net/sarif-converter
```

```shell script
docker run -v <sarif-path>:/data/sarif.json\
              <output>:/data/output\
              docker-registry.labs.intellij.net/jetbrains-analytics-converter
              -sp /data/sarif.json
              -o /data/output
              -vb
``` 

#Qodana IntelliJ Linter SARIF converter (Jar archive)

* for UNIX/Mac OS use `gradlew`
* for Windows use `gradlew.bat`


```shell script
./gradlew :sarif-converter:fatJar
```

The built jar will lie: `sarif-converter/build/libs/sarifConverter.jar`

Help:
```
usage: java -jar jarName [-help] [-o <arg>] -sp <arg> [-vb]
Standard commands:
 -help,--help             Print program options
 -o,--output <arg>        Output directory for report files
 -sp,--sarif-path <arg>   Path to SARIF file
 -vb,--verbose            Enable verbose logging
Please report issues at https://youtrack.jetbrains.com/issues/QD
```

P.S. `-o` default current path 
