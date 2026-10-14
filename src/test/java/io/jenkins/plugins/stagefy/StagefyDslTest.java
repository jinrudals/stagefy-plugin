package io.jenkins.plugins.stagefy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import hudson.Functions;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class StagefyDslTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
        assumeFalse(Functions.isWindows(), "tests rely on a POSIX shell");
    }

    private WorkflowRun run(String yaml, String stage) throws Exception {
        return run(yaml, stage, "");
    }

    private WorkflowRun run(String yaml, String stage, String prelude) throws Exception {
        WorkflowJob job = j.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition(
                "node {\n" + prelude + "writeFile file: 'Jenkins.yml', text: '''" + yaml + "'''\n"
                        + "stagefy.run('Jenkins.yml', '" + stage + "')\n}",
                true));
        return job.scheduleBuild2(0).get();
    }

    private void assertLog(WorkflowRun run, String text) throws Exception {
        assertTrue(JenkinsRule.getLog(run).contains(text), "log should contain: " + text);
    }

    @Test
    void stepsStageRunsShell() throws Exception {
        WorkflowRun run = run("build:\n  steps:\n    - sh: \"echo hello-stagefy\"\n", "build");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "hello-stagefy");
    }

    @Test
    void envBlockIsApplied() throws Exception {
        WorkflowRun run = run("build:\n  env:\n    FOO: bar\n  steps:\n    - sh: \"echo foo=$FOO\"\n", "build");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "foo=bar");
    }

    @Test
    void whenFalseSkipsSteps() throws Exception {
        WorkflowRun run = run("build:\n  when: \"false\"\n  steps:\n    - sh: \"echo should-not-run\"\n", "build");
        j.assertBuildStatusSuccess(run);
        assertFalse(JenkinsRule.getLog(run).contains("should-not-run"));
    }

    @Test
    void templateStepSubstitutesArguments() throws Exception {
        String yaml = "greet:\n  template:\n    arguments: [name]\n  steps:\n    - sh: \"echo hi-{name}\"\n"
                + "build:\n  steps:\n    - template: greet\n      name: world\n";
        WorkflowRun run = run(yaml, "build");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "hi-world");
    }

    @Test
    void iteratedStagesOverList() throws Exception {
        String yaml = "tpl:\n  template:\n    arguments: [target]\n  steps:\n    - sh: \"echo target-{target}\"\n"
                + "all:\n  stages:\n    - iterated:\n        template: tpl\n        over: [a, b]\n";
        WorkflowRun run = run(yaml, "all");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "target-a");
        assertLog(run, "target-b");
        assertLog(run, "tpl_a");
        assertLog(run, "tpl_b");
    }

    @Test
    void iteratedOverEnvVariable() throws Exception {
        String yaml = "tpl:\n  template:\n    arguments: [target]\n  steps:\n    - sh: \"echo target-{target}\"\n"
                + "all:\n  stages:\n    - iterated:\n        template: tpl\n        over: env.TARGETS\n";
        WorkflowRun run = run(yaml, "all", "env.TARGETS = 'x, y'\n");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "target-x");
        assertLog(run, "target-y");
    }

    @Test
    void parallelIteratedStages() throws Exception {
        String yaml = "tpl:\n  template:\n    arguments: [target]\n  steps:\n    - sh: \"echo par-{target}\"\n"
                + "all:\n  parallels:\n    - iterated:\n        template: tpl\n        over: [a, b]\n";
        WorkflowRun run = run(yaml, "all");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "par-a");
        assertLog(run, "par-b");
    }

    @Test
    void sequentialStagesReferenceOtherStages() throws Exception {
        String yaml = "one:\n  steps:\n    - sh: \"echo step-one\"\n"
                + "two:\n  steps:\n    - sh: \"echo step-two\"\n"
                + "all:\n  stages:\n    - one\n    - two\n";
        WorkflowRun run = run(yaml, "all");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "step-one");
        assertLog(run, "step-two");
    }

    @Test
    void useDirectiveRunsReferencedStage() throws Exception {
        String yaml = "lib:\n  steps:\n    - sh: \"echo from-lib\"\n"
                + "build:\n  steps:\n    - use: lib from Jenkins.yml\n";
        WorkflowRun run = run(yaml, "build");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "from-lib");
    }

    @Test
    void missingStageFails() throws Exception {
        WorkflowRun run = run("build:\n  steps:\n    - sh: \"true\"\n", "nope");
        j.assertBuildStatus(Result.FAILURE, run);
        assertLog(run, "Stage 'nope' not found");
    }

    @Test
    void templateCannotRunDirectly() throws Exception {
        String yaml = "tpl:\n  template:\n    arguments: [a]\n  steps:\n    - sh: \"true\"\n";
        WorkflowRun run = run(yaml, "tpl");
        j.assertBuildStatus(Result.FAILURE, run);
        assertLog(run, "cannot be executed directly");
    }

    @Test
    void missingTemplateArgumentFails() throws Exception {
        String yaml = "tpl:\n  template:\n    arguments: [name]\n  steps:\n    - sh: \"true\"\n"
                + "build:\n  steps:\n    - template: tpl\n";
        WorkflowRun run = run(yaml, "build");
        j.assertBuildStatus(Result.FAILURE, run);
        assertLog(run, "requires argument 'name'");
    }

    @Test
    void circularStageReferenceFails() throws Exception {
        String yaml = "a:\n  stages:\n    - b\nb:\n  stages:\n    - a\n";
        WorkflowRun run = run(yaml, "a");
        j.assertBuildStatus(Result.FAILURE, run);
        assertLog(run, "Circular Loop");
    }

    @Test
    void unknownStepDirectiveFails() throws Exception {
        WorkflowRun run = run("build:\n  steps:\n    - bogus: x\n", "build");
        j.assertBuildStatus(Result.FAILURE, run);
        assertLog(run, "Unknown step directive 'bogus'");
    }

    @Test
    void stageWithoutKindFails() throws Exception {
        WorkflowRun run = run("build:\n  env:\n    A: b\n", "build");
        j.assertBuildStatus(Result.FAILURE, run);
        assertLog(run, "has no 'stages', 'parallels', or 'steps' key");
    }

    @Test
    void envReferenceIsSubstituted() throws Exception {
        WorkflowRun run = run("build:\n  steps:\n    - sh: \"echo val=${env.MYVAR}\"\n", "build", "env.MYVAR = 'hello'\n");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "val=hello");
    }

    @Test
    void envValueIsNotExecutedAsShell() throws Exception {
        WorkflowRun run = run("build:\n  steps:\n    - sh: 'echo val=\"${env.MYVAR}\"'\n", "build",
                "env.MYVAR = 'x\"; echo INJECTED-MARKER; echo \"'\n");
        j.assertBuildStatusSuccess(run);
        assertFalse(JenkinsRule.getLog(run).contains("\nINJECTED-MARKER"), "value must not run as shell");
    }

    @Test
    void stageEnterIsLoggedToConsole() throws Exception {
        WorkflowRun run = run("build:\n  steps:\n    - sh: \"true\"\n", "build");
        j.assertBuildStatusSuccess(run);
        assertLog(run, "Stage ENTER: build");
    }
}
