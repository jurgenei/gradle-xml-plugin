package name.jurgenei.gradle.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders an executable phase-2 extraction stylesheet from normalized SEL rules.
 */
final class SelStylesheetCompiler {
    private SelStylesheetCompiler() {
    }

    static String render(List<SelRuleDescriptor> rules, Map<String, String> configuredGroupOutputs) {
        Map<String, String> groups = new LinkedHashMap<>();
        groups.putAll(configuredGroupOutputs);
        for (SelRuleDescriptor rule : rules) {
            groups.putIfAbsent(rule.group(), "sel/" + rule.group() + ".xml");
        }
        if (groups.isEmpty()) {
            groups.put("default", "sel/default.xml");
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<xsl:stylesheet version=\"3.0\"\n");
        xml.append("    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n");
        xml.append("    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n");
        xml.append("    xmlns:sel=\"http://jurgenei.name/sel\"\n");
        xml.append("    xmlns:c=\"http://jurgenei.name/canonical\"\n");
        xml.append("    exclude-result-prefixes=\"xs c\">\n\n");

        xml.append("  <xsl:output method=\"xml\" indent=\"yes\"/>\n");
        xml.append("  <xsl:mode on-no-match=\"shallow-skip\"/>\n");
        xml.append("  <xsl:param name=\"source-document\" as=\"xs:string\" select=\"''\"/>\n\n");
        xml.append("  <xsl:function name=\"sel:canonical-path\" as=\"xs:string\">\n");
        xml.append("    <xsl:param name=\"n\" as=\"node()\"/>\n");
        xml.append("    <xsl:sequence select=\"replace(path($n), 'Q\\{http://jurgenei.name/canonical\\}', 'c:')\"/>\n");
        xml.append("  </xsl:function>\n\n");

        for (int i = 0; i < rules.size(); i++) {
            xml.append("  <xsl:mode name=\"m-rule-")
                .append(i)
                .append("\" on-no-match=\"shallow-skip\"/>\n");
        }
        if (!rules.isEmpty()) {
            xml.append("\n");
        }

        for (Map.Entry<String, String> entry : groups.entrySet()) {
            xml.append("  <xsl:param name=\"output-")
                .append(escape(entry.getKey()))
                .append("\" as=\"xs:string\" select=\"'")
                .append(escape(entry.getValue()))
                .append("'\"/>\n");
        }
        xml.append("\n");

        xml.append("  <xsl:template match=\"/\">\n");
        xml.append("    <!-- Emit grouped SEL payloads into independent output files. -->\n");
        for (String group : groups.keySet()) {
            xml.append("    <xsl:result-document href=\"{$output-")
                .append(escape(group))
                .append("}\">\n");
            xml.append("      <sel:Observations group=\"")
                .append(escape(group))
                .append("\" xmlns:c=\"http://jurgenei.name/canonical\">\n");
            for (int i = 0; i < rules.size(); i++) {
                SelRuleDescriptor rule = rules.get(i);
                if (!group.equals(rule.group())) {
                    continue;
                }
                xml.append("        <xsl:apply-templates select=\"/\" mode=\"m-rule-")
                    .append(i)
                    .append("\"/>\n");
            }
            xml.append("      </sel:Observations>\n");
            xml.append("    </xsl:result-document>\n");
        }
        xml.append("  </xsl:template>\n");

        for (int i = 0; i < rules.size(); i++) {
            SelRuleDescriptor rule = rules.get(i);
            String condition = "assert".equals(rule.sourceElement())
                ? "not(" + rule.test() + ")"
                : "(" + rule.test() + ")";

            xml.append("\n  <xsl:template match=\"")
                .append(escape(rule.context()))
                .append("\" mode=\"m-rule-")
                .append(i)
                .append("\">\n");
            xml.append("    <xsl:if test=\"")
                .append(escape(condition))
                .append("\">\n");
            xml.append("      <sel:Observation type=\"")
                .append(escape(rule.type()))
                .append("\" group=\"")
                .append(escape(rule.group()))
                .append("\" source=\"")
                .append(escape(rule.sourceElement()))
                .append("\" ruleContext=\"")
                .append(escape(rule.context()))
                .append("\">\n");
            xml.append("        <sel:Evidence>\n");
            xml.append("          <xsl:copy-of select=\"")
                .append(escape(rule.copy()))
                .append("\" copy-namespaces=\"no\"/>\n");
            xml.append("        </sel:Evidence>\n");
            if (!rule.contextExpr().isBlank()) {
                xml.append("        <sel:Context>\n");
                xml.append("          <xsl:copy-of select=\"")
                    .append(escape(rule.contextExpr()))
                    .append("\" copy-namespaces=\"no\"/>\n");
                xml.append("        </sel:Context>\n");
            }
            xml.append("        <sel:Source document=\"{$source-document}\" path=\"{sel:canonical-path(.)}\"/>\n");
            xml.append("      </sel:Observation>\n");
            xml.append("    </xsl:if>\n");
            xml.append("  </xsl:template>\n");
        }

        xml.append("</xsl:stylesheet>\n");

        return xml.toString();
    }

    private static String escape(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
