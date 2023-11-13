# Contributing

By participating in this project, you agree to abide our [Code of conduct](.github/CODE_OF_CONDUCT.md).

## Set Up Your Machine

`qodana-sarif` is a Java library. To set up your development environment you just need JDK installed on your machine and an IDE.

Consider using IntelliJ IDEA:

- [IntelliJ IDEA](https://www.jetbrains.com/idea/) (it's [free for open-source development](https://www.jetbrains.com/community/opensource/) and Community version should be enough)

Clone the project anywhere:

```sh
git clone git@github.com:JetBrains/qodana-cli.git
```

Build and run the project locally using your preferred Java development tools.

## Create a commit

Commit messages should be well formatted. Consider using a standardized format for clarity.

## Submit a pull request

Push your branch to your repository fork and open a pull request against the
main branch.

## Release a new version

The new version will be automatically released from any new changes merged to the default branch by [TeamCity BuildServer CI job](https://buildserver.labs.intellij.net/buildConfiguration/StaticAnalysis_Build_QodanaSarifLibraryBuildAndDeploy).
