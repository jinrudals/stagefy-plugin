# Installation Guide

[한국어](install.ko.md)

## Prerequisites

| Item | Version | Notes |
|------|---------|-------|
| Jenkins | 2.541.1+ | LTS recommended |
| Java | 17+ | Runtime for Jenkins |
| Maven | 3.9+ | Only needed to build from source |

### Required Jenkins plugins

The following plugins must be installed in Jenkins:

- `workflow-aggregator` (Pipeline)
- `pipeline-stage-step`
- `pipeline-utility-steps` (`readYaml`, etc.)
- `pipeline-model-definition` (Declarative Pipeline)

## Installing

### 1. Build the hpi

```bash
mvn clean package -DskipTests
ls target/stagefy.hpi
```

### 2. Install into Jenkins

#### Option A: Web UI

1. Manage Jenkins > Plugins > Advanced settings
2. In the **Deploy Plugin** section, upload `target/stagefy.hpi`
3. Restart Jenkins

#### Option B: CLI

```bash
java -jar jenkins-cli.jar -s http://JENKINS_URL/ install-plugin target/stagefy.hpi -restart
```

#### Option C: Copy the file

```bash
cp target/stagefy.hpi $JENKINS_HOME/plugins/stagefy.hpi
# Restart Jenkins afterwards
```

### 3. Docker

Build a Jenkins image that bundles the plugin:

```bash
docker build -t stagefy-jenkins .
docker run -p 8080:8080 stagefy-jenkins
```

`Dockerfile`:

```dockerfile
FROM jenkins/jenkins:lts
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
COPY target/stagefy.hpi /usr/share/jenkins/ref/plugins/stagefy.hpi
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
```

## Verifying the installation

Create a Pipeline job in Jenkins and run this script:

```groovy
pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                writeFile file: 'test.yml', text: 'hello:\n  steps:\n    - sh: "echo ok"'
                script {
                    stagefy.run('test.yml', 'hello')
                }
            }
        }
    }
}
```

If the console output shows `ok`, the plugin is installed correctly.
