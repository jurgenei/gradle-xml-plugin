package name.jurgenei.gradle.xml.validation;

import java.util.List;

/**
 * Validation output for a single input document.
 *
 * @param issues normalized validation findings
 * @param svrlXml optional native SVRL output; when empty, SVRL is generated from issues
 */
public record ValidationResult(List<ValidationIssue> issues, String svrlXml) {
    public boolean hasErrors() {
        return issues != null && !issues.isEmpty();
    }
}

