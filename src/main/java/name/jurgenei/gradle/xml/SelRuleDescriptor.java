package name.jurgenei.gradle.xml;

/**
 * Normalized SEL rule metadata extracted from Schematron annotations.
 *
 * @param context rule context XPath.
 * @param test assertion/report test XPath.
 * @param type logical SEL type.
 * @param group logical output group.
 * @param copy XPath selecting evidence payload.
 * @param contextExpr optional XPath selecting contextual payload.
 * @param sourceElement Schematron element type (`report` or `assert`).
 */
record SelRuleDescriptor(
    String context,
    String test,
    String type,
    String group,
    String copy,
    String contextExpr,
    String sourceElement
) {
}
