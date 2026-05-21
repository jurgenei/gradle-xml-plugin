package name.jurgenei.gradle.xml.validation;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
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

    public static String renderSvrl(String document, List<ValidationIssue> issues) {
        StringBuilder xml = new StringBuilder();
        xml.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <svrl:schematron-output xmlns:svrl="%s" title="XSD validation">
              <svrl:active-pattern document="%s"/>
            """.formatted(SVRL_NS, escape(document)));
        for (ValidationIssue issue : issues) {
            xml.append("""
                  <svrl:failed-assert test="validation" location="%s">
                    <svrl:text>%s</svrl:text>
                  </svrl:failed-assert>
                """.formatted(escape(issue.location()), escape(issue.message())));
        }
        xml.append("""
            </svrl:schematron-output>
            """);
        return xml.toString();
    }

    public static List<ValidationIssue> parseSvrlIssues(String svrlXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
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

    public static String renderJunit(String suiteName, String testcaseName, List<ValidationIssue> issues) {
        StringBuilder xml = new StringBuilder();
        int failures = issues.size();
        xml.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="%s" tests="1" failures="%s" errors="0" skipped="0">
              <testcase classname="%s" name="%s">
            """
            .formatted(escape(suiteName), failures > 0 ? "1" : "0", escape(suiteName), escape(testcaseName)));
        if (failures > 0) {
            xml.append("""
                <failure message="%s">
                """.formatted(escape("Validation failed with " + failures + " issue(s)")));
            for (ValidationIssue issue : issues) {
                xml.append("""
                    %s: %s
                    """.formatted(escape(issue.location()), escape(issue.message())));
            }
            xml.append("""
                </failure>
                """);
        }
        xml.append("""
              </testcase>
            </testsuite>
            """);
        return xml.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}

