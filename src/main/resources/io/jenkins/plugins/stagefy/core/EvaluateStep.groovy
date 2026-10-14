package io.jenkins.plugins.stagefy.core

/** Step for YAML key {@code evaluate}: evaluates the given Groovy expression. */
class EvaluateStep extends Step {
    EvaluateStep(Map raw, def jenkins, Stage stage) {
        super(raw, jenkins, stage)
    }
    void run() { jenkins.evaluate(raw.get("evaluate")) }
}
