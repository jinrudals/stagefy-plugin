package io.jenkins.plugins.stagefy.core

class ScriptStep extends Step {
    ScriptStep(Map raw, def jenkins, Stage stage) {
        super(raw, jenkins, stage)
    }
    void run() { jenkins.load(raw.get("script")).main() }
}
