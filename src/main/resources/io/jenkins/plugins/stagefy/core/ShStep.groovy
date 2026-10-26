package io.jenkins.plugins.stagefy.core

import io.jenkins.plugins.stagefy.util.EnvResolver

/** Step for YAML keys {@code sh}, {@code bat}, {@code powershell}, {@code pwsh}: runs a command after resolving {@code ${env.X}} references. */
class ShStep extends Step {
    private String kind

    ShStep(Map raw, def jenkins, Stage stage, String kind = "sh") {
        super(raw, jenkins, stage)
        this.kind = kind
    }

    void run() {
        String text = (String) raw.get(kind)
        def envKeys = EnvResolver.extractEnvKeys(text)
        Map envValues = [:]
        for (entry in envKeys) {
            envValues.put(entry.get("key"), jenkins.getEnv(entry.get("key")))
        }
        String cmd = EnvResolver.substitute(text, envKeys, envValues, kind)
        jenkins."${kind}"(cmd)
    }
}
