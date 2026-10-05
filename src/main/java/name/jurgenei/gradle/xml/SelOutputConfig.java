/* (C)2026 */
package name.jurgenei.gradle.xml;

import java.util.regex.Pattern;

/**
 * Effective SEL output naming configuration for generated extraction payloads.
 *
 * @param namespaceUri namespace URI used for SEL output elements.
 * @param prefix namespace prefix used for SEL output elements; empty means default namespace.
 */
record SelOutputConfig(String namespaceUri, String prefix) {
    static final String DEFAULT_NAMESPACE_URI = "http://jurgenei.name/sel";
    static final String DEFAULT_PREFIX = "sel";
    private static final Pattern PREFIX_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9._-]*");

    static SelOutputConfig defaults() {
        return new SelOutputConfig(DEFAULT_NAMESPACE_URI, DEFAULT_PREFIX);
    }

    static SelOutputConfig resolve(
            SelOutputConfig schemaDefaults, String overrideNamespaceUri, String overridePrefix) {
        String namespaceUri = trimToNull(overrideNamespaceUri);
        if (namespaceUri == null) {
            namespaceUri = schemaDefaults.namespaceUri();
        }
        namespaceUri = trimToNull(namespaceUri);
        if (namespaceUri == null) {
            throw new IllegalArgumentException("SEL output namespace URI must not be blank");
        }

        String prefix;
        if (overridePrefix != null) {
            prefix = overridePrefix.trim();
        } else {
            prefix = schemaDefaults.prefix() == null ? "" : schemaDefaults.prefix().trim();
        }

        if (!prefix.isEmpty()) {
            if (!PREFIX_PATTERN.matcher(prefix).matches()) {
                throw new IllegalArgumentException(
                        "SEL output prefix is not a valid XML NCName: '" + prefix + "'");
            }
            if ("xml".equalsIgnoreCase(prefix) || "xmlns".equalsIgnoreCase(prefix)) {
                throw new IllegalArgumentException(
                        "SEL output prefix cannot be reserved XML token: '" + prefix + "'");
            }
        }

        return new SelOutputConfig(namespaceUri, prefix);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
