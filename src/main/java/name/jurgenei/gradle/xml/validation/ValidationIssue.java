package name.jurgenei.gradle.xml.validation;

/**
 * A single validation finding mapped to SVRL/JUnit output.
 *
 * @param severity finding severity
 * @param message human-readable message
 * @param location source location hint (XPath, line/column, or URI)
 */
public record ValidationIssue(String severity, String message, String location) {
    public static ValidationIssue error(String message, String location) {
        return new ValidationIssue("error", message, location == null ? "" : location);
    }
}

