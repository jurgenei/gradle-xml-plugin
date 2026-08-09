package name.jurgenei.gradle.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts `obs:*` annotated rule metadata from a Schematron document.
 */
final class ObservationRuleCollector {
    static final String SCH_NS = "http://purl.oclc.org/dsdl/schematron";
    static final String OBS_NS = "http://jurgenei.name/observation";

    private ObservationRuleCollector() {
    }

    static List<ObservationRuleDescriptor> collect(Document schematron) {
        List<ObservationRuleDescriptor> descriptors = new ArrayList<>();
        descriptors.addAll(collectFromElements(schematron, "report"));
        descriptors.addAll(collectFromElements(schematron, "assert"));
        return descriptors;
    }

    private static List<ObservationRuleDescriptor> collectFromElements(Document schematron, String localName) {
        List<ObservationRuleDescriptor> descriptors = new ArrayList<>();
        NodeList reports = schematron.getElementsByTagNameNS(SCH_NS, localName);
        for (int i = 0; i < reports.getLength(); i++) {
            Element ruleNode = (Element) reports.item(i);
            if (!isEmitEnabled(ruleNode)) {
                continue;
            }
            Element parentRule = (Element) ruleNode.getParentNode();
            String context = nonBlank(parentRule.getAttribute("context"), "*");
            String test = nonBlank(ruleNode.getAttribute("test"), "true()");
            String type = nonBlank(ruleNode.getAttributeNS(OBS_NS, "type"), "observation");
            String group = nonBlank(ruleNode.getAttributeNS(OBS_NS, "group"), "default");
            String copy = nonBlank(ruleNode.getAttributeNS(OBS_NS, "copy"), ".");
            String contextExpr = ruleNode.getAttributeNS(OBS_NS, "context");

            descriptors.add(new ObservationRuleDescriptor(
                context,
                test,
                type,
                group,
                copy,
                contextExpr.trim(),
                localName
            ));
        }
        return descriptors;
    }

    private static boolean isEmitEnabled(Element node) {
        String emit = node.getAttributeNS(OBS_NS, "emit");
        if (emit == null || emit.isBlank()) {
            emit = node.getAttribute("obs:emit");
        }
        return "true".equalsIgnoreCase(emit.trim());
    }

    private static String nonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}

