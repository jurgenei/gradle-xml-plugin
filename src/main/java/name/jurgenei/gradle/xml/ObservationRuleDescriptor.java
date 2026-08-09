package name.jurgenei.gradle.xml;

/**
 * Normalized observation rule metadata extracted from Schematron annotations.
 *
 * @param context rule context XPath.
 * @param test assertion/report test XPath.
 * @param type logical observation type.
 * @param group logical output group.
 * @param copy XPath selecting evidence payload.
 * @param contextExpr optional XPath selecting contextual payload.
 * @param sourceElement Schematron element type (`report` or `assert`).
 */
record ObservationRuleDescriptor(
    String context,
    String test,
    String type,
    String group,
    String copy,
    String contextExpr,
    String sourceElement
) {
}

