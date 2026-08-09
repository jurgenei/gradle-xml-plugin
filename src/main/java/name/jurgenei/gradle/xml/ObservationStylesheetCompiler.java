package name.jurgenei.gradle.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders an executable phase-2 extraction stylesheet from normalized observation rules.
 */
final class ObservationStylesheetCompiler {
    private ObservationStylesheetCompiler() {
    }

    static String render(List<ObservationRuleDescriptor> rules, Map<String, String> configuredGroupOutputs) {
        Map<String, String> groups = new LinkedHashMap<>();
        groups.putAll(configuredGroupOutputs);
        for (ObservationRuleDescriptor rule : rules) {
            groups.putIfAbsent(rule.group(), "observations/" + rule.group() + ".xml");
        }
        if (groups.isEmpty()) {
            groups.put("default", "observations/default.xml");
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<xsl:stylesheet version=\"3.0\"\n");
        xml.append("    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n");
        xml.append("    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n");
        xml.append("    xmlns:obs=\"http://jurgenei.name/observation\"\n");
        xml.append("    xmlns:c=\"http://jurgenei.name/canonical\"\n");
        xml.append("    exclude-result-prefixes=\"xs c\">\n\n");

        xml.append("  <xsl:output method=\"xml\" indent=\"yes\"/>\n");
        xml.append("  <xsl:mode on-no-match=\"shallow-skip\"/>\n");
        xml.append("  <xsl:param name=\"source-document\" as=\"xs:string\" select=\"''\"/>\n\n");

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
        xml.append("    <!-- Emit grouped observation payloads into independent output files. -->\n");
        for (String group : groups.keySet()) {
            xml.append("    <xsl:result-document href=\"{$output-")
                .append(escape(group))
                .append("}\">\n");
            xml.append("      <obs:Observations group=\"")
                .append(escape(group))
                .append("\">\n");
            for (int i = 0; i < rules.size(); i++) {
                ObservationRuleDescriptor rule = rules.get(i);
                if (!group.equals(rule.group())) {
                    continue;
                }
                xml.append("        <xsl:apply-templates select=\"/\" mode=\"m-rule-")
                    .append(i)
                    .append("\"/>\n");
            }
            xml.append("      </obs:Observations>\n");
            xml.append("    </xsl:result-document>\n");
        }
        xml.append("  </xsl:template>\n");

        for (int i = 0; i < rules.size(); i++) {
            ObservationRuleDescriptor rule = rules.get(i);
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
            xml.append("      <obs:Observation type=\"")
                .append(escape(rule.type()))
                .append("\" group=\"")
                .append(escape(rule.group()))
                .append("\" source=\"")
                .append(escape(rule.sourceElement()))
                .append("\" ruleContext=\"")
                .append(escape(rule.context()))
                .append("\">\n");
            xml.append("        <obs:Evidence>\n");
            xml.append("          <xsl:copy-of select=\"")
                .append(escape(rule.copy()))
                .append("\"/>\n");
            xml.append("        </obs:Evidence>\n");
            if (!rule.contextExpr().isBlank()) {
                xml.append("        <obs:Context>\n");
                xml.append("          <xsl:copy-of select=\"")
                    .append(escape(rule.contextExpr()))
                    .append("\"/>\n");
                xml.append("        </obs:Context>\n");
            }
            xml.append("        <obs:Source document=\"{$source-document}\" path=\"{path(.)}\"/>\n");
            xml.append("      </obs:Observation>\n");
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

