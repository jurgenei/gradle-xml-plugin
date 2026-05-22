package name.jurgenei.gradle.xml.validation;

/**
 * Validation engine options for XSD validation.
 */
public enum XsdEngine {
    /** Automatically choose Saxon schema-aware when available, otherwise JAXP. */
    AUTO,
    /** Force Saxon schema-aware validation (requires PE/EE). */
    SAXON,
    /** Force JAXP/Xerces validation. */
    JAXP
}

