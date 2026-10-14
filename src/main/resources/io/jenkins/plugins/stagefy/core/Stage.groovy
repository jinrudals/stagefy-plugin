package io.jenkins.plugins.stagefy.core

/** Base class of stages: loads its YAML, evaluates {@code when}, guards against cycles, then delegates to execute(). */
abstract class Stage implements Serializable {
    String filename
    String stagename
    boolean flag = true
    Stage parent
    def jenkins

    Stage(String filename, String stagename, Stage parent, def jenkins) {
        this.filename = filename
        this.stagename = stagename
        this.parent = parent
        this.jenkins = jenkins
    }

    def load() { return jenkins.loadData(filename, stagename) }

    /** Walks up the parent chain and throws if the same file/stage appears again (circular reference). */
    void checkCircularLoop(Stage other) {
        if (parent == null) return
        if (parent.filename == other.filename && parent.stagename == other.stagename) {
            throw new RuntimeException("Circular Loop Execution ${stagename} from ${other.filename}")
        }
        parent.checkCircularLoop(other)
    }

    /** Rejects template stages, computes {@code flag} from {@code when} (ANDed with the parent's), then runs execute(). */
    void run() {
        Map data = load()
        if (data != null && data.containsKey("template")) {
            jenkins.error("Template stage '${stagename}' cannot be executed directly")
        }
        def whenData = data.get("when")
        flag = (whenData == null) ? true : jenkins.evaluate(whenData)
        if (parent != null) flag = parent.flag && flag
        jenkins.info("Stage ENTER: ${stagename}")
        execute(data)
        jenkins.info("Stage EXIT: ${stagename}")
    }

    /** Subclass hook: runs the stage body from its YAML {@code data}. */
    abstract void execute(Map data)

    // "name" or "name from file"; only whitespace-delimited "from" separates, so names containing "from" are safe
    protected List parseStageRef(String ref) {
        def parts = ref.trim().split(/\s+from\s+/, 2)
        if (parts.length == 2) return [parts[0].trim(), parts[1].trim()]
        return [ref.trim(), this.filename]
    }

    /** Creates the child stage {@code name} from {@code file}, with this stage as parent. */
    Stage constructChild(String file, String name) {
        def childData = jenkins.loadData(file, name)
        return StageFactory.create(file, name, this, jenkins, childData)
    }

    /** Runs already-resolved step maps in order via StepFactory. */
    void executeResolvedSteps(List resolvedSteps) {
        for (step in resolvedSteps) {
            jenkins.debug("Step: ${step.keySet()}")
            StepFactory.create(step, jenkins, this).run()
        }
    }
}
