package io.jenkins.plugins.stagefy.core

/** Step for YAML key {@code use} ("Stage from file"); optional {@code makeStage: false} runs it without a new Jenkins stage. */
class UseStageStep extends Step {
    UseStageStep(Map raw, def jenkins, Stage stage) {
        super(raw, jenkins, stage)
    }
    void run() {
        String useValue = (String) raw.get("use")
        boolean makeStage = raw.containsKey("makeStage") ? Boolean.parseBoolean(raw.get("makeStage").toString()) : true
        if (!useValue.contains(" from ")) {
            jenkins.error("use directive must follow 'StageName from filepath' format: '${useValue}'")
        }
        def parts = useValue.split(" from ", 2)
        String targetStage = parts[0].trim()
        String targetFile = parts[1].trim()
        Stage child = stage.constructChild(targetFile, targetStage)
        child.checkCircularLoop(child)
        if (makeStage) {
            jenkins.stage(targetStage) { child.run() }
        } else {
            child.run()
        }
    }
}
