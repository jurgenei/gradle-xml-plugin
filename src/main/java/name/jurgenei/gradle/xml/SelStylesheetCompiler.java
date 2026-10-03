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

    static String render(
        List<SelRuleDescriptor> rules,
        Map<String, String> configuredGroupOutputs,
        SelOutputConfig outputConfig
    ) {
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
        xml.append("    xmlns:sx=\"urn:name.jurgenei.gradle.xml:sel-support\"\n");
        xml.append("    xmlns:c=\"http://jurgenei.name/canonical\"\n");
        if (!outputConfig.prefix().isBlank()) {
            xml.append("    xmlns:")
                .append(outputConfig.prefix())
                .append("=\"")
                .append(escape(outputConfig.namespaceUri()))
                .append("\"\n");
        }
        xml.append("    exclude-result-prefixes=\"xs c sx\" expand-text=\"yes\">\n\n");

        xml.append("  <xsl:output method=\"xml\" indent=\"yes\"/>\n");
        xml.append("  <xsl:mode on-no-match=\"shallow-skip\"/>\n");
        xml.append("  <xsl:param name=\"source-document\" as=\"xs:string\" select=\"''\"/>\n\n");
        xml.append("  <xsl:function name=\"sx:canonical-path\" as=\"xs:string\">\n");
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
            xml.append("      ").append(observationsStartTag(group, outputConfig)).append("\n");
            for (int i = 0; i < rules.size(); i++) {
                SelRuleDescriptor rule = rules.get(i);
                if (!group.equals(rule.group())) {
                    continue;
                }
                xml.append("        <xsl:apply-templates select=\"/\" mode=\"m-rule-")
                    .append(i)
                    .append("\"/>\n");
            }
            xml.append("      ").append(observationsEndTag(outputConfig)).append("\n");
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
            xml.append("      ").append(observationStartTag(rule, outputConfig)).append("\n");
            xml.append("        ").append(evidenceStartTag(outputConfig)).append("\n");
            xml.append("          <xsl:copy-of select=\"")
                .append(escape(rule.copy()))
                .append("\" copy-namespaces=\"no\"/>\n");
            xml.append("        ").append(evidenceEndTag(outputConfig)).append("\n");
            if (!rule.contextExpr().isBlank()) {
                xml.append("        ").append(contextStartTag(outputConfig)).append("\n");
                xml.append("          <xsl:copy-of select=\"")
                    .append(escape(rule.contextExpr()))
                    .append("\" copy-namespaces=\"no\"/>\n");
                xml.append("        ").append(contextEndTag(outputConfig)).append("\n");
            }
            if (!rule.templateFragment().isBlank()) {
                xml.append(indentFragment(rule.templateFragment(), 8)).append("\n");
            }
            xml.append("        ").append(sourceTag(outputConfig)).append("\n");
            xml.append("      ").append(observationEndTag(outputConfig)).append("\n");
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

    private static String observationsStartTag(String group, SelOutputConfig outputConfig) {
        if (outputConfig.prefix().isBlank()) {
            return "<Observations xmlns=\"" + escape(outputConfig.namespaceUri())
                + "\" group=\"" + escape(group) + "\" xmlns:c=\"http://jurgenei.name/canonical\">";
        }
        return "<" + outputConfig.prefix() + ":Observations group=\"" + escape(group)
            + "\" xmlns:c=\"http://jurgenei.name/canonical\">";
    }

    private static String observationsEndTag(SelOutputConfig outputConfig) {
        if (outputConfig.prefix().isBlank()) {
            return "</Observations>";
        }
        return "</" + outputConfig.prefix() + ":Observations>";
    }

    private static String observationStartTag(SelRuleDescriptor rule, SelOutputConfig outputConfig) {
        StringBuilder xml = new StringBuilder();
        xml.append("<").append(elementName(outputConfig, "Observation"))
            .append(" type=\"").append(escape(rule.type()))
            .append("\" group=\"").append(escape(rule.group()))
            .append("\" source=\"").append(escape(rule.sourceElement()))
            .append("\" ruleContext=\"").append(escape(rule.context()))
            .append("\">");
        return xml.toString();
    }

    private static String observationEndTag(SelOutputConfig outputConfig) {
        return "</" + elementName(outputConfig, "Observation") + ">";
    }

    private static String evidenceStartTag(SelOutputConfig outputConfig) {
        return "<" + elementName(outputConfig, "Evidence") + ">";
    }

    private static String evidenceEndTag(SelOutputConfig outputConfig) {
        return "</" + elementName(outputConfig, "Evidence") + ">";
    }

    private static String contextStartTag(SelOutputConfig outputConfig) {
        return "<" + elementName(outputConfig, "Context") + ">";
    }

    private static String contextEndTag(SelOutputConfig outputConfig) {
        return "</" + elementName(outputConfig, "Context") + ">";
    }

    private static String sourceTag(SelOutputConfig outputConfig) {
        return "<" + elementName(outputConfig, "Source")
            + " document=\"{$source-document}\" path=\"{sx:canonical-path(.)}\"/>";
    }

    private static String elementName(SelOutputConfig outputConfig, String localName) {
        if (outputConfig.prefix().isBlank()) {
            return localName;
        }
        return outputConfig.prefix() + ":" + localName;
    }

    private static String indentFragment(String fragment, int spaces) {
        String indent = " ".repeat(spaces);
        String[] lines = fragment.split("\\R", -1);
        StringBuilder xml = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                xml.append("\n");
            }
            xml.append(indent).append(lines[i]);
        }
        return xml.toString();
    }
}
