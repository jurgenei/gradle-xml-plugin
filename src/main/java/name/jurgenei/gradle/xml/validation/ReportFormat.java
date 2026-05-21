package name.jurgenei.gradle.xml.validation;

/**
 * Output report formats emitted by validation tasks.
 */
public enum ReportFormat {
    SVRL,
    JUNIT,
    SVRL_AND_JUNIT;

    /**
     * @return true when SVRL output should be written
     */
    public boolean writesSvrl() {
        return this == SVRL || this == SVRL_AND_JUNIT;
    }

    /**
     * @return true when JUnit XML output should be written
     */
    public boolean writesJunit() {
        return this == JUNIT || this == SVRL_AND_JUNIT;
    }
}

