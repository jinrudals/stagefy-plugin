# Stagefy Jenkins Plugin

[한국어](README.ko.md)

Stagefy lets you define pipeline **stages** in YAML and reuse them across pipelines. It adds a global `stagefy` variable that you call from a Jenkinsfile, so stage definitions live in plain YAML files and are shared through `use`, `template` and `iterated` directives.

Because the stage logic ships inside the plugin, Jenkinsfiles only call `stagefy.run(...)`; no custom shared-library code needs Script Security approval.

## Relationship to pipeline-as-yaml

[pipeline-as-yaml](https://plugins.jenkins.io/pipeline-as-yaml/) defines a whole pipeline in YAML. Stagefy does not replace it, it complements it: Stagefy focuses on reusing YAML at the **stage level** (`use`, `template`, `iterated`).

## Usage

```groovy
// Jenkinsfile
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                script {
                    stagefy.run('Jenkins.yml', 'build')
                }
            }
        }
    }
}
```

```yaml
# Jenkins.yml
build:
  steps:
    - sh: "make build"
    - sh: "make test"
```

`stagefy.run(file, stage)` reads the YAML file from the workspace and runs the named stage inside a `stage(...)` block.

## YAML reference

A stage is a top-level key in the YAML file. Its kind is decided by one of these keys:

| Key | Meaning |
|-----|---------|
| `steps` | Run a list of steps in order |
| `stages` | Run other stages sequentially |
| `parallels` | Run other stages in parallel |
| `template` | Declares a reusable template (cannot be run directly) |

`when` (Groovy expression, the stage is skipped when false) is accepted on every stage kind. Optional keys on a `steps` stage only: `env` (map of environment variables) and `node` (label to run on).

### Steps

| Step | Description |
|------|-------------|
| `sh: "cmd"` | Run a shell command. `${env.VAR}` is substituted from the build environment; plain names are passed as shell variable references (`${VAR}`), so values are never interpreted as shell code. |
| `script: path` | Load a Groovy file and call its `main()` |
| `evaluate: expr` | Evaluate a Groovy expression |
| `setEnvFromFile: file` | Read the `env:` map of a YAML file into the build environment |
| `use: Stage from file.yml` | Run a stage defined in another file; add `makeStage: false` to skip the wrapping `stage(...)` |
| `template: name` | Expand a template, passing arguments as sibling keys |
| `iterated:` | Expand a template once per item (see below) |

> **Security:** `script`, `evaluate` and `when` run arbitrary Groovy outside the Script Security sandbox. Only load YAML files and scripts from sources you trust.
>
> Template arguments (`{name}`) are inlined as text into `sh` steps, just like `${...}` interpolation in a Jenkinsfile. When a value comes from outside (branch names, build parameters, `over: env.VAR`), quote it in the step (`sh: "deploy.sh '{name}'"`) and make sure it cannot contain quotes or shell syntax. `${env.VAR}` is different: it is passed as a shell variable and is never interpreted as code.

### Stage composition

```yaml
all:
  stages:
    - build
    - test from common.yml        # stage from another file
    - template: notify            # template used as a stage
      channel: ci
```

Use `parallels:` instead of `stages:` to run the entries in parallel. Stage names must be unique within one block, and circular references are rejected.

### Templates and iteration

```yaml
deploy:
  template:
    arguments: [target]
  steps:
    - sh: "./deploy.sh {target}"

release:
  stages:
    - iterated:
        template: deploy
        over: [dev, staging]      # or: env.TARGETS  (comma-separated)
```

Arguments are referenced as `{name}` inside template steps. `over` may be a list of scalars, a list of maps (each key becomes an argument; an optional `_name` key names the generated stage) or `env.VAR_NAME` holding a comma-separated list. Each item produces a stage named `<template>_<item>`. Templates may only contain steps; they cannot contain `template` or `iterated` themselves.

## Editor validation

A JSON Schema for the stage YAML is provided at [`schema/stagefy.schema.json`](schema/stagefy.schema.json) (draft-07). It gives completion and flags unknown keys in editors that use the YAML language server.

Reference it with a modeline at the top of a stage file:

```yaml
# yaml-language-server: $schema=https://raw.githubusercontent.com/jenkinsci/stagefy-plugin/main/schema/stagefy.schema.json
build:
  steps:
    - sh: "make build"
```

or map it in VS Code `settings.json` (Red Hat YAML extension):

```json
"yaml.schemas": {
  "./schema/stagefy.schema.json": ["Jenkins.yml", "stages/*.yml"]
}
```

The schema checks structure only; Groovy in `when`, `evaluate` and `script` and cross-file references are not validated.

## Troubleshooting

| Message | Cause |
|---------|-------|
| `Stage 'X' not found in 'file'` | The stage name is not a top-level key of the YAML file, or the file is not in the workspace (checkout first). |
| `Stage 'X' has no 'stages', 'parallels', or 'steps' key` | The stage defines none of the three body keys. |
| `Unknown step directive 'x' in stage 'X'` | A step uses a key other than `sh`, `script`, `setEnvFromFile`, `evaluate`, `use`, `template`, `iterated`. |
| `Template 'T' requires argument 'a' ...` | A `template` step or `iterated` item does not supply every declared argument. |
| `Circular Loop Execution ...` | Stages reference each other in a cycle. |
| `Template stage 'T' cannot be executed directly` | Templates can only be used through `template` or `iterated`. |

## Requirements

| Item | Version |
|------|---------|
| Jenkins | 2.541.1+ |
| Java | 17+ |
| Plugins | `workflow-cps`, `workflow-step-api`, `pipeline-stage-step`, `pipeline-utility-steps` |

## Building

```bash
mvn clean verify          # build, run tests and static analysis
mvn hpi:run               # start a development Jenkins with the plugin
```

The built plugin is `target/stagefy.hpi`.

## Documentation

- [Installation guide](install.md)
- [Deployment guide](deploy.md)
- [Editor validation (JSON Schema)](#editor-validation)
- [Changelog](CHANGELOG.md)

## Contributing

Issues and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) and record notable changes in [CHANGELOG.md](CHANGELOG.md).

## License

Licensed under the [MIT License](LICENSE).
