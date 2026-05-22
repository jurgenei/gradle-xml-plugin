package name.jurgenei.gradle.xml.validation;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * Ready-to-implement contract for SVRL-based validation tasks.
 */
public interface ValidationTaskSpec {
    /**
     * Returns the destination directory for generated SVRL files.
     *
     * @return destination directory for SVRL outputs
     */
    DirectoryProperty getOutputDir();

    /**
     * Returns the output extension used when writing SVRL files.
     *
     * @return output extension used for SVRL file mapping
     */
    Property<String> getOutputExtension();

    /**
     * Returns the number of workers used for concurrent validation.
     *
     * @return number of parallel workers for multi-file validation
     */
    Property<Integer> getWorkers();

    /**
     * Returns whether validation findings fail the build.
     *
     * @return whether validation findings should fail the build
     */
    Property<Boolean> getFailOnError();

    /**
     * Returns the maximum number of failures to aggregate.
     *
     * @return maximum number of failures to aggregate before stopping
     */
    Property<Integer> getMaxFailures();

    /**
     * Returns the configured report format.
     *
     * @return selected report format(s)
     */
    Property<ReportFormat> getReportFormat();

    /**
     * Returns the directory used for generated JUnit XML files.
     *
     * @return destination directory for generated JUnit XML reports
     */
    DirectoryProperty getJunitOutputDir();

    /**
     * Returns the testsuite name used in JUnit report generation.
     *
     * @return JUnit suite name used for generated reports
     */
    Property<String> getJunitSuiteName();

    /**
     * Returns task parameters passed to the active validation engine.
     *
     * @return task-level parameters consumed by validation engines
     */
    MapProperty<String, String> getParams();
}

