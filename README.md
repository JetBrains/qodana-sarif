## Qodana Clone Finder Action for GitHub Actions

![Qodana EAP version alert](resources/eap-alert.png)

**Qodana Clone Finder** compares a queried project against a number of reference projects and lists all duplicate functions ranked by their importance. The tool is designed to prevent problems rather than face the consequences down the line. By supporting CI integration, Clone Finder makes clone detection a routine check and reports borrowed code before it can lead to trouble.

**Table of Contents**

<!-- toc -->

- [Usage](#usage)
- [Output Results](#output-results)
- [License Summary](#license-summary)
- [Contact](#contact)

<!-- tocstop -->


## Usage

Input parameters:
* `project-dir` - Project folder to inspect (default `${{ github.workspace }}/project`)
* `versus-dir` - Folder with projects to search clones from (default `${{ github.workspace }}/versus`)
* `results-dir` - Save results to folder (default `${{ github.workspace }}/qodana`)
* `languages` - Programming languages to search clones from (default `-l PHP -l Go -l JavaScript -l TypeScript -l Java -l Kotlin -l Python`)

```yaml
- name: Qodana - Clone Finder
  uses: JetBrains/qodana-clone-finder-action@v2.0-eap
```

All action's inputs are optional. 
```yaml
- name: Qodana - Code Inspection
  uses: JetBrains/qodana-clone-finder-action@v2.0-eap
  with:
      project-dir: ${{ github.workspace }}/project
      versus-dir: ${{ github.workspace }}/versus
      results-dir: ${{ github.workspace }}/qodana
      languages: "-l Java -l Kotlin"

```

Before you begin, view the list of [Supported Technologies](https://github.com/JetBrains/Qodana/blob/main/General/supported-technologies.md). For the full documentation of the action's inputs, see [action.yaml](action.yaml).

## Output Results

An example of the Qodana Clone Finder command-line summary output:
```
✨   Done!
┏━━━━━━━━━━━━┳━━━━━━━━┳━━━━━━━━━━━┓
┃ Repository ┃ Clones ┃ Functions ┃
┡━━━━━━━━━━━━╇━━━━━━━━╇━━━━━━━━━━━┩
│ buckwheat  │ 6      │ 72        │
└────────────┴────────┴───────────┘
```

Full Clone Finder results are available in the file `report.json` located in the `results-dir` folder.

## License Summary

By using Qodana, you agree to the [JetBrains EAP user agreement](https://www.jetbrains.com/legal/agreements/user_eap.html) and [JetBrains privacy policy](https://www.jetbrains.com/company/privacy.html).

## Contact

Contact us at [qodana-support@jetbrains.com](mailto:qodana-support@jetbrains.com) or via [our issue tracker](https://youtrack.jetbrains.com/newIssue?project=QD). We are eager to receive your feedback on the existing Qodana functionality and learn what other features you miss in it.
