# Deployment Guide

[한국어](deploy.ko.md)

## Build

```bash
mvn clean package -DskipTests
```

Output: `target/stagefy.hpi`

## Deployment options

### 1. Jenkinsfile (CI/CD)

The `Jenkinsfile` at the repository root builds and tests the plugin with `buildPlugin()`. Releases are handled by `.github/workflows/cd.yaml` (Jenkins continuous delivery).

### 2. Manual hpi update

How to update the plugin manually on a production Jenkins.

#### Preparation

- Jenkins administrator access
- A built `stagefy.hpi`
- A maintenance window in which Jenkins can be restarted

#### Steps

1. **Build the hpi**

   ```bash
   mvn clean package -DskipTests
   ```

2. **Back up the existing plugin**

   ```bash
   cp $JENKINS_HOME/plugins/stagefy.hpi $JENKINS_HOME/plugins/stagefy.hpi.bak
   cp -r $JENKINS_HOME/plugins/stagefy $JENKINS_HOME/plugins/stagefy.bak
   ```

3. **Replace the hpi**

   ```bash
   cp target/stagefy.hpi $JENKINS_HOME/plugins/stagefy.hpi
   # Remove the unpacked directory if present (Jenkins unpacks it again on restart)
   rm -rf $JENKINS_HOME/plugins/stagefy
   ```

4. **Restart Jenkins**

   ```bash
   # Safe restart (waits for running builds to finish)
   java -jar jenkins-cli.jar -s http://JENKINS_URL/ safe-restart

   # Or via the web UI: http://JENKINS_URL/safeRestart
   ```

5. **Verify the version**

   Check that the `Stagefy` version is updated under Manage Jenkins > Plugins > Installed plugins.

#### Rollback

If something goes wrong, restore from the backup:

```bash
cp $JENKINS_HOME/plugins/stagefy.hpi.bak $JENKINS_HOME/plugins/stagefy.hpi
rm -rf $JENKINS_HOME/plugins/stagefy
# Restart Jenkins
```

### 3. Docker

Ship the hpi inside a Docker image:

```bash
mvn clean package -DskipTests
docker build -t stagefy-jenkins .
```

Replace the running container:

```bash
docker stop jenkins
docker rm jenkins
docker run -d --name jenkins -p 8080:8080 \
  -v jenkins_home:/var/jenkins_home \
  stagefy-jenkins
```

## Notes

- Jenkins must be restarted after replacing the hpi for the new version to load.
- If you do not delete the unpacked `$JENKINS_HOME/plugins/stagefy/` directory, the old version may keep being used.
- A safe restart waits for running builds to finish.
- On production systems, perform the update during a maintenance window.
