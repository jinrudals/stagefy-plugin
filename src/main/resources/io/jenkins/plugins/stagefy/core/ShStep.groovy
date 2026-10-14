package io.jenkins.plugins.stagefy.core

import io.jenkins.plugins.stagefy.util.EnvResolver

/** Step for YAML key {@code sh}: runs a shell command after resolving {@code ${env.X}} references. */
class ShStep extends Step {
    ShStep(Map raw, def jenkins, Stage stage) {
        super(raw, jenkins, stage)
    }

    void run() {
        String text = (String) raw.get("sh")
        def envKeys = EnvResolver.extractEnvKeys(text)
        Map envValues = [:]
        for (entry in envKeys) {
            envValues.put(entry.get("key"), jenkins.getEnv(entry.get("key")))
        }
        String cmd = EnvResolver.substitute(text, envKeys, envValues)
        jenkins.sh(cmd)
    }
}
