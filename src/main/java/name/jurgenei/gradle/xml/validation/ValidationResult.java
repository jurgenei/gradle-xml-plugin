package name.jurgenei.gradle.xml.validation;

import java.util.List;

/**
 * Validation output for a single input document.
 *
 * @param issues normalized validation findings
 * @param svrlXml optional native SVRL output; when empty, SVRL is generated from issues
 */
public record ValidationResult(List<ValidationIssue> issues, String svrlXml) {
    public ValidationResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
        svrlXml = svrlXml == null ? "" : svrlXml;
    }

    /**
     * Indicates whether the result contains any findings.
     *
     * @return true when one or more validation findings are present
     */
    public boolean hasErrors() {
        return !issues.isEmpty();
    }
}

