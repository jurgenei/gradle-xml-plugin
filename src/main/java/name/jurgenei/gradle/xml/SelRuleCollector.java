package name.jurgenei.gradle.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Extracts `sel:*` annotated rule metadata from a Schematron document.
 */
final class SelRuleCollector {
    static final String SCH_NS = "http://purl.oclc.org/dsdl/schematron";
    static final String SEL_NS = "http://jurgenei.name/sel";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private SelRuleCollector() {
    }

    static SelRuleSet collect(Document schematron) {
        Map<String, SelPreset> presets = collectPresets(schematron);
        List<SelRuleDescriptor> descriptors = new ArrayList<>();
        descriptors.addAll(collectFromElements(schematron, "report", presets));
        descriptors.addAll(collectFromElements(schematron, "assert", presets));
        String defaultPhase = schematron.getDocumentElement().getAttribute("defaultPhase").trim();
        Map<String, List<String>> phasePatterns = collectPhasePatterns(schematron);
        return new SelRuleSet(List.copyOf(descriptors), defaultPhase, phasePatterns);
    }

    private static List<SelRuleDescriptor> collectFromElements(
        Document schematron,
        String localName,
        Map<String, SelPreset> presets
    ) {
        List<SelRuleDescriptor> descriptors = new ArrayList<>();
        NodeList reports = schematron.getElementsByTagNameNS(SCH_NS, localName);
        for (int i = 0; i < reports.getLength(); i++) {
            Element ruleNode = (Element) reports.item(i);
            if (!hasSelAnnotations(ruleNode)) {
                continue;
            }
            Element parentRule = (Element) ruleNode.getParentNode();
            String context = nonBlank(parentRule.getAttribute("context"), "*");
            String test = nonBlank(ruleNode.getAttribute("test"), "true()");
            EffectiveSelMetadata metadata = resolveSelMetadata(ruleNode, context, test, presets);
            String patternId = owningPatternId(ruleNode);

            descriptors.add(new SelRuleDescriptor(
                context,
                test,
                metadata.type(),
                metadata.group(),
                metadata.copy(),
                metadata.context(),
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

    private static boolean hasSelAnnotations(Element node) {
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (!SEL_NS.equals(attribute.getNamespaceURI())) {
                continue;
            }
            String localName = attribute.getLocalName();
            if ("emit".equals(localName)) {
                continue;
            }
            return true;
        }
        String[] selAttributes = {"preset", "type", "group", "copy", "context"};
        for (String attr : selAttributes) {
            if (!selAttribute(node, attr).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static EffectiveSelMetadata resolveSelMetadata(
        Element node,
        String ruleContext,
        String ruleTest,
        Map<String, SelPreset> presets
    ) {
        String type = "sel";
        String group = "default";
        String copy = ".";
        String contextExpr = "";

        String referenced = selAttribute(node, "preset");
        if (!referenced.isBlank()) {
            for (String presetId : splitReferences(referenced)) {
                SelPreset preset = presets.get(presetId);
                if (preset == null) {
                    throw new IllegalArgumentException(
                        "Unknown SEL preset '" + presetId + "' referenced by rule context='"
                            + ruleContext + "' test='" + ruleTest + "'"
                    );
                }
                type = nonBlank(preset.type(), type);
                group = nonBlank(preset.group(), group);
                copy = nonBlank(preset.copy(), copy);
                contextExpr = nonBlank(preset.context(), contextExpr);
            }
        }

        type = nonBlank(selAttribute(node, "type"), type);
        group = nonBlank(selAttribute(node, "group"), group);
        copy = nonBlank(selAttribute(node, "copy"), copy);
        contextExpr = nonBlank(selAttribute(node, "context"), contextExpr);
        return new EffectiveSelMetadata(type, group, copy, contextExpr);
    }

    private static List<String> splitReferences(String references) {
        List<String> ids = new ArrayList<>();
        for (String token : WHITESPACE.split(references.trim())) {
            if (!token.isBlank()) {
                ids.add(token.trim());
            }
        }
        return ids;
    }

    private static Map<String, SelPreset> collectPresets(Document schematron) {
        Map<String, SelPreset> presets = new LinkedHashMap<>();
        NodeList presetNodes = schematron.getElementsByTagNameNS(SEL_NS, "preset");
        for (int i = 0; i < presetNodes.getLength(); i++) {
            Element presetNode = (Element) presetNodes.item(i);
            String id = presetNode.getAttribute("id").trim();
            if (id.isEmpty()) {
                throw new IllegalArgumentException("SEL preset missing required @id attribute");
            }
            if (presets.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate SEL preset id '" + id + "'");
            }
            presets.put(id, new SelPreset(
                id,
                plainOrSelAttribute(presetNode, "type"),
                plainOrSelAttribute(presetNode, "group"),
                plainOrSelAttribute(presetNode, "copy"),
                plainOrSelAttribute(presetNode, "context")
            ));
        }
        return Map.copyOf(presets);
    }

    private static String plainOrSelAttribute(Element node, String localName) {
        String value = node.getAttribute(localName);
        if (value == null || value.isBlank()) {
            value = selAttribute(node, localName);
        }
        return value == null ? "" : value.trim();
    }

    private static String selAttribute(Element node, String localName) {
        String value = node.getAttributeNS(SEL_NS, localName);
        if (value == null || value.isBlank()) {
            value = node.getAttribute("sel:" + localName);
        }
        return value == null ? "" : value.trim();
    }

    private static String nonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private record SelPreset(
        String id,
        String type,
        String group,
        String copy,
        String context
    ) {
    }

    private record EffectiveSelMetadata(
        String type,
        String group,
        String copy,
        String context
    ) {
    }
}
