package io.jenkins.plugins.stagefy.core

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
