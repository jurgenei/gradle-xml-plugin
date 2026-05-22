package name.jurgenei.gradle.xml.validation;

/**
 * Output report formats emitted by validation tasks.
 */
public enum ReportFormat {
    /** Writes only SVRL output files. */
    SVRL,
    /** Writes only JUnit XML reports. */
    JUNIT,
    /** Writes both SVRL and JUnit XML outputs. */
    SVRL_AND_JUNIT;

    /**
     * Determines whether SVRL output files should be written.
     *
     * @return true when SVRL output should be written
     */
    public boolean writesSvrl() {
        return this == SVRL || this == SVRL_AND_JUNIT;
    }

    /**
     * Determines whether JUnit XML reports should be written.
     *
     * @return true when JUnit XML output should be written
     */
    public boolean writesJunit() {
        return this == JUNIT || this == SVRL_AND_JUNIT;
    }
}

