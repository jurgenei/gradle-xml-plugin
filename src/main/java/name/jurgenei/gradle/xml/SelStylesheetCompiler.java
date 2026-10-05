/* (C)2026 */
package name.jurgenei.gradle.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.SAXException;

/**
 * Renders an executable phase-2 extraction stylesheet from normalized SEL rules.
 */
final class SelStylesheetCompiler {
    private static final String XSL_NS = "http://www.w3.org/1999/XSL/Transform";
    private static final String XS_NS = "http://www.w3.org/2001/XMLSchema";
    private static final String SX_NS = "urn:name.jurgenei.gradle.xml:sel-support";
    private static final String C_NS = "http://jurgenei.name/canonical";

    private SelStylesheetCompiler() {}

    static String render(
            List<SelRuleDescriptor> rules,
            Map<String, String> configuredGroupOutputs,
            SelOutputConfig outputConfig) {
        Map<String, String> groups = new LinkedHashMap<>();
        groups.putAll(configuredGroupOutputs);
        for (SelRuleDescriptor rule : rules) {
            groups.putIfAbsent(rule.group(), "sel/" + rule.group() + ".xml");
        }
        if (groups.isEmpty()) {
            groups.put("default", "sel/default.xml");
        }

        try {
            SaxXmlWriter xml = SaxXmlWriter.createPretty();
            xml.startDocument();

            Map<String, String> stylesheetNamespaces = new LinkedHashMap<>();
            stylesheetNamespaces.put("xsl", XSL_NS);
            stylesheetNamespaces.put("xs", XS_NS);
            stylesheetNamespaces.put("sx", SX_NS);
            stylesheetNamespaces.put("c", C_NS);
            if (!outputConfig.prefix().isBlank()) {
                stylesheetNamespaces.put(outputConfig.prefix(), outputConfig.namespaceUri());
            }

            xml.startElement(
                    XSL_NS,
                    "stylesheet",
                    "xsl:stylesheet",
                    List.of(
                            SaxXmlWriter.attr("version", "3.0"),
                            SaxXmlWriter.attr("exclude-result-prefixes", "xs c sx"),
                            SaxXmlWriter.attr("expand-text", "yes")),
                    stylesheetNamespaces);

            xml.emptyElement(
                    XSL_NS,
                    "output",
                    "xsl:output",
                    List.of(
                            SaxXmlWriter.attr("method", "xml"),
                            SaxXmlWriter.attr("indent", "yes")),
                    Map.of());
            xml.emptyElement(
                    XSL_NS,
                    "mode",
                    "xsl:mode",
                    List.of(SaxXmlWriter.attr("on-no-match", "shallow-skip")),
                    Map.of());
            xml.emptyElement(
                    XSL_NS,
                    "param",
                    "xsl:param",
                    List.of(
                            SaxXmlWriter.attr("name", "source-document"),
                            SaxXmlWriter.attr("as", "xs:string"),
                            SaxXmlWriter.attr("select", "''")),
                    Map.of());

            xml.startElement(
                    XSL_NS,
                    "function",
                    "xsl:function",
                    List.of(
                            SaxXmlWriter.attr("name", "sx:canonical-path"),
                            SaxXmlWriter.attr("as", "xs:string")),
                    Map.of());
            xml.emptyElement(
                    XSL_NS,
                    "param",
                    "xsl:param",
                    List.of(
                            SaxXmlWriter.attr("name", "n"),
                            SaxXmlWriter.attr("as", "node()")),
                    Map.of());
            xml.emptyElement(
                    XSL_NS,
                    "sequence",
                    "xsl:sequence",
                    List.of(
                            SaxXmlWriter.attr(
                                    "select",
                                    "replace(path($n), 'Q\\{http://jurgenei.name/canonical\\}', 'c:')")),
                    Map.of());
            xml.endElement(XSL_NS, "function", "xsl:function");

            for (int i = 0; i < rules.size(); i++) {
                xml.emptyElement(
                        XSL_NS,
                        "mode",
                        "xsl:mode",
                        List.of(
                                SaxXmlWriter.attr("name", "m-rule-" + i),
                                SaxXmlWriter.attr("on-no-match", "shallow-skip")),
                        Map.of());
            }

            for (Map.Entry<String, String> entry : groups.entrySet()) {
                xml.emptyElement(
                        XSL_NS,
                        "param",
                        "xsl:param",
                        List.of(
                                SaxXmlWriter.attr("name", "output-" + entry.getKey()),
                                SaxXmlWriter.attr("as", "xs:string"),
                                SaxXmlWriter.attr("select", "'" + entry.getValue() + "'")),
                        Map.of());
            }

            xml.startElement(
                    XSL_NS, "template", "xsl:template", List.of(SaxXmlWriter.attr("match", "/")), Map.of());
            xml.comment(" Emit grouped SEL payloads into independent output files. ");
            for (String group : groups.keySet()) {
                xml.startElement(
                        XSL_NS,
                        "result-document",
                        "xsl:result-document",
                        List.of(SaxXmlWriter.attr("href", "{$output-" + group + "}")),
                        Map.of());

                xml.startElement(
                        outputConfig.namespaceUri(),
                        "Observations",
                        elementName(outputConfig, "Observations"),
                        List.of(SaxXmlWriter.attr("group", group)),
                        observationsNamespaces(outputConfig));

                for (int i = 0; i < rules.size(); i++) {
                    SelRuleDescriptor rule = rules.get(i);
                    if (!group.equals(rule.group())) {
                        continue;
                    }
                    xml.emptyElement(
                            XSL_NS,
                            "apply-templates",
                            "xsl:apply-templates",
                            List.of(
                                    SaxXmlWriter.attr("select", "/"),
                                    SaxXmlWriter.attr("mode", "m-rule-" + i)),
                            Map.of());
                }
                xml.endElement(
                        outputConfig.namespaceUri(),
                        "Observations",
                        elementName(outputConfig, "Observations"));
                xml.endElement(XSL_NS, "result-document", "xsl:result-document");
            }
            xml.endElement(XSL_NS, "template", "xsl:template");

            for (int i = 0; i < rules.size(); i++) {
                SelRuleDescriptor rule = rules.get(i);
                String condition =
                        "assert".equals(rule.sourceElement())
                                ? "not(" + rule.test() + ")"
                                : "(" + rule.test() + ")";

                xml.startElement(
                        XSL_NS,
                        "template",
                        "xsl:template",
                        List.of(
                                SaxXmlWriter.attr("match", rule.context()),
                                SaxXmlWriter.attr("mode", "m-rule-" + i)),
                        Map.of());
                xml.startElement(
                        XSL_NS,
                        "if",
                        "xsl:if",
                        List.of(SaxXmlWriter.attr("test", condition)),
                        Map.of());

                xml.startElement(
                        outputConfig.namespaceUri(),
                        "Observation",
                        elementName(outputConfig, "Observation"),
                        List.of(
                                SaxXmlWriter.attr("type", rule.type()),
                                SaxXmlWriter.attr("group", rule.group()),
                                SaxXmlWriter.attr("source", rule.sourceElement()),
                                SaxXmlWriter.attr("ruleContext", rule.context())),
                        Map.of());

                xml.startElement(
                        outputConfig.namespaceUri(),
                        "Evidence",
                        elementName(outputConfig, "Evidence"),
                        List.of(),
                        Map.of());
                xml.emptyElement(
                        XSL_NS,
                        "copy-of",
                        "xsl:copy-of",
                        List.of(
                                SaxXmlWriter.attr("select", rule.copy()),
                                SaxXmlWriter.attr("copy-namespaces", "no")),
                        Map.of());
                xml.endElement(
                        outputConfig.namespaceUri(),
                        "Evidence",
                        elementName(outputConfig, "Evidence"));

                if (!rule.contextExpr().isBlank()) {
                    xml.startElement(
                            outputConfig.namespaceUri(),
                            "Context",
                            elementName(outputConfig, "Context"),
                            List.of(),
                            Map.of());
                    xml.emptyElement(
                            XSL_NS,
                            "copy-of",
                            "xsl:copy-of",
                            List.of(
                                    SaxXmlWriter.attr("select", rule.contextExpr()),
                                    SaxXmlWriter.attr("copy-namespaces", "no")),
                            Map.of());
                    xml.endElement(
                            outputConfig.namespaceUri(),
                            "Context",
                            elementName(outputConfig, "Context"));
                }

                if (!rule.templateFragment().isBlank()) {
                    xml.appendFragment(rule.templateFragment(), fragmentNamespaces(outputConfig));
                }

                xml.emptyElement(
                        outputConfig.namespaceUri(),
                        "Source",
                        elementName(outputConfig, "Source"),
                        List.of(
                                SaxXmlWriter.attr("document", "{$source-document}"),
                                SaxXmlWriter.attr("path", "{sx:canonical-path(.)}")),
                        Map.of());

                xml.endElement(
                        outputConfig.namespaceUri(),
                        "Observation",
                        elementName(outputConfig, "Observation"));
                xml.endElement(XSL_NS, "if", "xsl:if");
                xml.endElement(XSL_NS, "template", "xsl:template");
            }

            xml.endElement(XSL_NS, "stylesheet", "xsl:stylesheet");
            xml.endDocument();
            return xml.build();
        } catch (SAXException e) {
            throw new IllegalStateException("Failed to render SEL stylesheet", e);
        }
    }

    private static String elementName(SelOutputConfig outputConfig, String localName) {
        if (outputConfig.prefix().isBlank()) {
            return localName;
        }
        return outputConfig.prefix() + ":" + localName;
    }

    private static Map<String, String> observationsNamespaces(SelOutputConfig outputConfig) {
        Map<String, String> namespaces = new LinkedHashMap<>();
        if (outputConfig.prefix().isBlank()) {
            namespaces.put("", outputConfig.namespaceUri());
        } else {
            namespaces.put(outputConfig.prefix(), outputConfig.namespaceUri());
        }
        namespaces.put("c", C_NS);
        return namespaces;
    }

    private static Map<String, String> fragmentNamespaces(SelOutputConfig outputConfig) {
        Map<String, String> namespaces = new LinkedHashMap<>();
        namespaces.put("xsl", XSL_NS);
        namespaces.put("xs", XS_NS);
        namespaces.put("sx", SX_NS);
        namespaces.put("c", C_NS);
        if (!outputConfig.prefix().isBlank()) {
            namespaces.put(outputConfig.prefix(), outputConfig.namespaceUri());
        } else {
            namespaces.put("", outputConfig.namespaceUri());
        }
        return namespaces;
    }
}
