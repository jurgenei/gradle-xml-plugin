package name.jurgenei.gradle.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts `sel:*` annotated rule metadata from a Schematron document.
 */
final class SelRuleCollector {
    static final String SCH_NS = "http://purl.oclc.org/dsdl/schematron";
    static final String SEL_NS = "http://jurgenei.name/sel";

    private SelRuleCollector() {
    }

    static SelRuleSet collect(Document schematron) {
        List<SelRuleDescriptor> descriptors = new ArrayList<>();
        descriptors.addAll(collectFromElements(schematron, "report"));
        descriptors.addAll(collectFromElements(schematron, "assert"));
        String defaultPhase = schematron.getDocumentElement().getAttribute("defaultPhase").trim();
        Map<String, List<String>> phasePatterns = collectPhasePatterns(schematron);
        return new SelRuleSet(List.copyOf(descriptors), defaultPhase, phasePatterns);
    }

    private static List<SelRuleDescriptor> collectFromElements(Document schematron, String localName) {
        List<SelRuleDescriptor> descriptors = new ArrayList<>();
        NodeList reports = schematron.getElementsByTagNameNS(SCH_NS, localName);
        for (int i = 0; i < reports.getLength(); i++) {
            Element ruleNode = (Element) reports.item(i);
            if (!isEmitEnabled(ruleNode)) {
                continue;
            }
            Element parentRule = (Element) ruleNode.getParentNode();
            String context = nonBlank(parentRule.getAttribute("context"), "*");
            String test = nonBlank(ruleNode.getAttribute("test"), "true()");
            String type = nonBlank(ruleNode.getAttributeNS(SEL_NS, "type"), "sel");
            String group = nonBlank(ruleNode.getAttributeNS(SEL_NS, "group"), "default");
            String copy = nonBlank(ruleNode.getAttributeNS(SEL_NS, "copy"), ".");
            String contextExpr = ruleNode.getAttributeNS(SEL_NS, "context");
            String patternId = owningPatternId(ruleNode);

            descriptors.add(new SelRuleDescriptor(
                context,
                test,
                type,
                group,
                copy,
                contextExpr.trim(),
                localName,
                patternId
            ));
        }
        return descriptors;
    }

    private static Map<String, List<String>> collectPhasePatterns(Document schematron) {
        Map<String, List<String>> phasePatterns = new LinkedHashMap<>();
        NodeList phases = schematron.getElementsByTagNameNS(SCH_NS, "phase");
        for (int i = 0; i < phases.getLength(); i++) {
            Element phase = (Element) phases.item(i);
            String phaseId = phase.getAttribute("id").trim();
            if (phaseId.isEmpty()) {
                continue;
            }
            List<String> activePatterns = new ArrayList<>();
            NodeList activeNodes = phase.getElementsByTagNameNS(SCH_NS, "active");
            for (int j = 0; j < activeNodes.getLength(); j++) {
                Element active = (Element) activeNodes.item(j);
                String patternRef = active.getAttribute("pattern").trim();
                if (!patternRef.isEmpty()) {
                    activePatterns.add(patternRef);
                }
            }
            phasePatterns.put(phaseId, List.copyOf(activePatterns));
        }
        return Map.copyOf(phasePatterns);
    }

    private static String owningPatternId(Element node) {
        Node current = node.getParentNode();
        while (current != null) {
            if (current instanceof Element element
                && SCH_NS.equals(element.getNamespaceURI())
                && "pattern".equals(element.getLocalName())) {
                return element.getAttribute("id").trim();
            }
            current = current.getParentNode();
        }
        return "";
    }

    private static boolean isEmitEnabled(Element node) {
        String emit = node.getAttributeNS(SEL_NS, "emit");
        if (emit == null || emit.isBlank()) {
            emit = node.getAttribute("sel:emit");
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
