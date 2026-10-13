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

Optional keys on a `steps` stage: `when` (Groovy expression, the stage is skipped when false), `env` (map of environment variables), `node` (label to run on) and `modules` (environment modules to load before each `sh` step).

### Steps

| Step | Description |
|------|-------------|
| `sh: "cmd"` | Run a shell command. `${env.VAR}` is substituted from the build environment. |
| `script: path` | Load a Groovy file and call its `main()` |
| `evaluate: expr` | Evaluate a Groovy expression |
| `setEnvFromFile: file` | Read the `env:` map of a YAML file into the build environment |
| `use: Stage from file.yml` | Run a stage defined in another file; add `makeStage: false` to skip the wrapping `stage(...)` |
| `template: name` | Expand a template, passing arguments as sibling keys |
| `iterated:` | Expand a template once per item (see below) |

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

## Contributing

Issues and pull requests are welcome. Please run `mvn verify` before submitting a change.

## License

Licensed under the [MIT License](LICENSE).
