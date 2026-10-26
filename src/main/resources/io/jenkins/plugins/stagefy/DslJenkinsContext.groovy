package io.jenkins.plugins.stagefy

import org.jenkinsci.plugins.workflow.cps.CpsScript
import java.util.logging.Level
import java.util.logging.Logger

/** Thin, serializable facade over the pipeline script (CpsScript) used by all stage/step classes. */
class DslJenkinsContext implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(DslJenkinsContext.class.name)
    private def script
    private Map yamlCache = new LinkedHashMap()

    DslJenkinsContext(CpsScript script) {
        this.script = script
    }

    def sh(String cmd) {
        script.sh(cmd)
    }

    def bat(String cmd) {
        script.bat(cmd)
    }

    def powershell(String cmd) {
        script.powershell(cmd)
    }

    def pwsh(String cmd) {
        script.pwsh(cmd)
    }

    def stage(String name, Closure body) {
        script.stage(name, body)
    }

    def parallel(Map branches) {
        script.parallel(branches)
    }

    def node(String label, Closure body) {
        script.node(label, body)
    }

    def withEnv(List envs, Closure body) {
        script.withEnv(envs, body)
    }

    /** Parses a YAML file once per run; later calls return the cached result (treat it as read-only). */
    def readYaml(String file) {
        if (!yamlCache.containsKey(file)) {
            yamlCache.put(file, script.readYaml(file: file))
        }
        return yamlCache.get(file)
    }

    def load(String path) {
        return script.load(path)
    }

    def evaluate(value) {
        return script.evaluate(value)
    }

    /** Logs at SEVERE and fails the build; never returns (the error step always throws AbortException). */
    def error(String msg) {
        LOGGER.log(Level.SEVERE, msg)
        script.error(msg)
    }

    def getEnv(String name) {
        return script.env[name]
    }

    /** Sets an env var; the value is stringified (null stays null). */
    def setEnvProperty(String key, Object value) {
        script.env.setProperty(key, value?.toString())
    }

    def log(String msg) {
        script.println(msg)
    }

    def debug(String msg) { LOGGER.log(Level.FINE, msg) }
    /** Prints to the build console only (no system log entry). */
    def info(String msg) {
        script.println(msg)
    }
    def warn(String msg) { LOGGER.log(Level.WARNING, msg) }

    /** Returns the top-level YAML entry named {@code stagename} from {@code filename} (null if absent). */
    def loadData(String filename, String stagename) {
        return readYaml(filename)[stagename]
    }

    /** Exports every entry of the YAML file's top-level {@code env:} map as an env var. */
    def setEnvFromFile(String filename) {
        def temp = readYaml(filename)
        if (temp.env != null) {
            temp.env.each { k, v -> setEnvProperty(k, v) }
        }
    }
}
