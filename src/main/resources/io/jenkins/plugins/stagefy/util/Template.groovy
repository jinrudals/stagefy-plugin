package io.jenkins.plugins.stagefy.util

class Template implements Serializable {
    String name
    String filename
    Map data
    List arguments
    List steps

    Template(String name, String filename, Map data) {
        this.name = name
        this.filename = filename
        this.data = data
        this.arguments = data?.template?.arguments
        this.steps = data?.steps
    }

    def validate(def jenkins) {
        if (data == null) jenkins.error("Template '${name}' not found in '${filename}'")
        if (data.template == null) jenkins.error("Stage '${name}' is not a template - missing 'template:' key in '${filename}'")
        if (arguments == null || arguments.isEmpty()) jenkins.error("Template '${name}' must declare at least one argument under 'template.arguments'")
        if (steps == null) jenkins.error("Template '${name}' has no 'steps' section")
        if (data.stages != null || data.parallels != null) jenkins.error("Template '${name}' must contain only steps")
        for (step in steps) {
            if (step instanceof Map && (step.containsKey('template') || step.containsKey('iterated'))) {
                jenkins.error("Template '${name}' must contain only steps - found 'template' or 'iterated' key inside template steps")
            }
        }
    }

    def validateBindings(Map bindings, def jenkins, String usedInStage) {
        for (arg in arguments) {
            if (!bindings.containsKey(arg)) {
                jenkins.error("Template '${name}' requires argument '${arg}' but it was not supplied (used in stage '${usedInStage}')")
            }
        }
        // Values are inlined into sh steps, so reject anything the shell could interpret.
        for (entry in bindings.entrySet()) {
            def bad = (entry.value.toString() =~ /[^A-Za-z0-9_.\/:@%+=,~ -]/)
            if (bad.find()) {
                jenkins.error("Template '${name}' argument '${entry.key}' (used in stage '${usedInStage}') contains unsafe character '${bad.group()}'; allowed: letters, digits and _ . / : @ % + = , ~ - and space")
            }
        }
    }

    List getArguments() { return arguments }
    List getSteps() { return steps }
}
