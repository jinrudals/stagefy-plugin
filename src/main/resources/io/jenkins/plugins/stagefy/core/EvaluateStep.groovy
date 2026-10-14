package io.jenkins.plugins.stagefy.core

class EvaluateStep extends Step {
    EvaluateStep(Map raw, def jenkins, Stage stage) {
        super(raw, jenkins, stage)
    }
    void run() { jenkins.evaluate(raw.get("evaluate")) }
}
