package io.jenkins.plugins.stagefy.core

/** Step for YAML key {@code script}: loads the Groovy file and invokes its {@code main()}. */
class ScriptStep extends Step {
    ScriptStep(Map raw, def jenkins, Stage stage) {
        super(raw, jenkins, stage)
    }
    void run() { jenkins.load(raw.get("script")).main() }
}
