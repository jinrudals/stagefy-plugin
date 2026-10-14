package io.jenkins.plugins.stagefy.core

/** Base class of all steps; {@code raw} is the step's YAML map and {@code stage} its owning stage. */
abstract class Step implements Serializable {
    Map raw
    def jenkins
    Stage stage

    Step(Map raw, def jenkins, Stage stage) {
        this.raw = raw
        this.jenkins = jenkins
        this.stage = stage
    }

    abstract void run()
}
