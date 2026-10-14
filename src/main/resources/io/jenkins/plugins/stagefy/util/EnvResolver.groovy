package io.jenkins.plugins.stagefy.util

/** Finds {@code ${env.NAME}} references in {@code sh} text and substitutes them. */
class EnvResolver implements Serializable {
    /** Returns a list of [full: matched text, key: env name] for each {@code ${env.NAME}} occurrence. */
    static List extractEnvKeys(String text) {
        def pattern = /\$\{\s*env\.([a-zA-Z_.]+)\s*\}/
        def keys = []
        def matcher = (text =~ pattern)
        for (int i = 0; i < matcher.count; i++) {
            keys.add([full: matcher[i][0], key: matcher[i][1]])
        }
        return keys
    }

    // Plain identifiers become shell variable references so values are never re-parsed
    // as shell code (Jenkins exports env.* to sh steps); other keys keep literal substitution.
    static String substitute(String text, List envEntries, Map envValues) {
        def result = text
        for (entry in envEntries) {
            def val = envValues.get(entry.key)
            if (val == null) continue
            String key = (String) entry.key
            String replacement = (key ==~ /[a-zA-Z_][a-zA-Z0-9_]*/) ? "\${" + key + "}" : val.toString()
            result = result.replace(entry.full, replacement)
        }
        return result
    }
}
