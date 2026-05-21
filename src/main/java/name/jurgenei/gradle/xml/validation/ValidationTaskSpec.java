package name.jurgenei.gradle.xml.validation;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * Ready-to-implement contract for SVRL-based validation tasks.
 */
public interface ValidationTaskSpec {
    DirectoryProperty getOutputDir();

    Property<String> getOutputExtension();

    Property<Integer> getWorkers();

    Property<Boolean> getFailOnError();

    Property<Integer> getMaxFailures();

    Property<ReportFormat> getReportFormat();

    DirectoryProperty getJunitOutputDir();

    Property<String> getJunitSuiteName();

    MapProperty<String, String> getParams();
}

