package io.jenkins.plugins.stagefy.core

class SetEnvFromFileStep extends Step {
    SetEnvFromFileStep(Map raw, def jenkins, Stage stage) {
        super(raw, jenkins, stage)
    }
    void run() { jenkins.setEnvFromFile((String) raw.get("setEnvFromFile")) }
}
