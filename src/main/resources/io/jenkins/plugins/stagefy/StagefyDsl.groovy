package io.jenkins.plugins.stagefy

import org.jenkinsci.plugins.workflow.cps.CpsScript

/** Pipeline entry point: loads a stage from a YAML file and runs it inside a Jenkins stage. */
class StagefyDsl implements Serializable {
    private CpsScript script

    StagefyDsl(CpsScript script) {
        this.script = script
    }

    /** Runs {@code stage} defined in YAML {@code file}, wrapped in a Jenkins stage of the same name. */
    def run(String file, String stage) {
        def jenkins = new DslJenkinsContext(script)
        def data = jenkins.loadData(file, stage)
        def stageObj = io.jenkins.plugins.stagefy.core.StageFactory.create(file, stage, null, jenkins, data)
        script.stage(stage) {
            stageObj.run()
        }
    }

    /** Step-style entry point: {@code stagefy file: 'Jenkins.yml', stage: 'build'} (no {@code script} block needed). */
    def call(Map args) {
        if (!args.file || !args.stage) {
            script.error("stagefy requires 'file' and 'stage'")
        }
        run(args.file as String, args.stage as String)
    }
}
