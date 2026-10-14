package io.jenkins.plugins.stagefy.core

/** Step for YAML key {@code setEnvFromFile}: exports the {@code env:} map of the named YAML file. */
class SetEnvFromFileStep extends Step {
    SetEnvFromFileStep(Map raw, def jenkins, Stage stage) {
        super(raw, jenkins, stage)
    }
    void run() { jenkins.setEnvFromFile((String) raw.get("setEnvFromFile")) }
}
