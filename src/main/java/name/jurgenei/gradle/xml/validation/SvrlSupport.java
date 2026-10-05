package name.jurgenei.gradle.xml.validation;

import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import name.jurgenei.gradle.xml.SaxXmlWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Utility for generating and parsing SVRL and JUnit XML reports.
 */
public final class SvrlSupport {

    private static final String SVRL_NS = "http://purl.oclc.org/dsdl/svrl";

    private SvrlSupport() {
    }

    /**
     * Renders a normalized SVRL document from validation issues.
     *
     * @param document source document identifier
     * @param issues normalized validation findings
     * @return SVRL XML string
     */
    public static String renderSvrl(String document, List<ValidationIssue> issues) {
        try {
            SaxXmlWriter xml = SaxXmlWriter.createPretty();
            xml.startDocument();
            xml.startElement(
                    SVRL_NS,
                    "schematron-output",
                    "svrl:schematron-output",
                    List.of(SaxXmlWriter.attr("title", "XSD validation")),
                    java.util.Map.of("svrl", SVRL_NS));
            xml.emptyElement(
                    SVRL_NS,
                    "active-pattern",
                    "svrl:active-pattern",
                    List.of(SaxXmlWriter.attr("document", safe(document))),
                    java.util.Map.of());
            for (ValidationIssue issue : issues) {
                xml.startElement(
                        SVRL_NS,
                        "failed-assert",
                        "svrl:failed-assert",
                        List.of(
                                SaxXmlWriter.attr("test", "validation"),
                                SaxXmlWriter.attr("location", safe(issue.location()))),
                        java.util.Map.of());
                xml.startElement(SVRL_NS, "text", "svrl:text", List.of(), java.util.Map.of());
                xml.text(safe(issue.message()));
                xml.endElement(SVRL_NS, "text", "svrl:text");
                xml.endElement(SVRL_NS, "failed-assert", "svrl:failed-assert");
            }
            xml.endElement(SVRL_NS, "schematron-output", "svrl:schematron-output");
            xml.endDocument();
            return xml.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render SVRL document", e);
        }
    }

    /**
     * Parses failed assertions from an SVRL XML document.
     *
     * @param svrlXml SVRL XML input
     * @return extracted validation findings
     * @throws Exception when XML parsing fails
     */
    public static List<ValidationIssue> parseSvrlIssues(String svrlXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder()
            .parse(new java.io.ByteArrayInputStream(svrlXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        NodeList asserts = document.getElementsByTagNameNS(SVRL_NS, "failed-assert");
        List<ValidationIssue> issues = new ArrayList<>();
        for (int i = 0; i < asserts.getLength(); i++) {
            Element failedAssert = (Element) asserts.item(i);
            String location = failedAssert.getAttribute("location");
            NodeList texts = failedAssert.getElementsByTagNameNS(SVRL_NS, "text");
            String message = texts.getLength() > 0 ? texts.item(0).getTextContent() : "Schematron assertion failed";
            issues.add(ValidationIssue.error(message, location));
        }
        return issues;
    }

    /**
     * Renders a minimal JUnit XML report from normalized findings.
     *
     * @param suiteName JUnit testsuite name
     * @param testcaseName JUnit testcase name
     * @param issues normalized validation findings
     * @return JUnit XML string
     */
    public static String renderJunit(String suiteName, String testcaseName, List<ValidationIssue> issues) {
        try {
            SaxXmlWriter xml = SaxXmlWriter.createPretty();
            int failures = issues.size();
            xml.startDocument();
            xml.startElement(
                    "",
                    "testsuite",
                    "testsuite",
                    List.of(
                            SaxXmlWriter.attr("name", safe(suiteName)),
                            SaxXmlWriter.attr("tests", "1"),
                            SaxXmlWriter.attr("failures", failures > 0 ? "1" : "0"),
                            SaxXmlWriter.attr("errors", "0"),
                            SaxXmlWriter.attr("skipped", "0")),
                    java.util.Map.of());
            xml.startElement(
                    "",
                    "testcase",
                    "testcase",
                    List.of(
                            SaxXmlWriter.attr("classname", safe(suiteName)),
                            SaxXmlWriter.attr("name", safe(testcaseName))),
                    java.util.Map.of());
            if (failures > 0) {
                xml.startElement(
                        "",
                        "failure",
                        "failure",
                        List.of(
                                SaxXmlWriter.attr(
                                        "message",
                                        "Validation failed with " + failures + " issue(s)")),
                        java.util.Map.of());
                for (ValidationIssue issue : issues) {
                    xml.text(safe(issue.location()) + ": " + safe(issue.message()));
                    xml.text("\n");
                }
                xml.endElement("", "failure", "failure");
            }
            xml.endElement("", "testcase", "testcase");
            xml.endElement("", "testsuite", "testsuite");
            xml.endDocument();
            return xml.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render JUnit XML", e);
        }
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }
}
