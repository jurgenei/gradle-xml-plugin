/* (C)2026 */
package name.jurgenei.gradle.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collected SEL rules and Schematron phase metadata.
 *
 * @param rules all collected SEL rules in deterministic order.
 * @param defaultPhase schema default phase id; empty when not defined.
 * @param phasePatterns phase id -> active pattern ids mapping.
 * @param outputConfig schema-level SEL output namespace/prefix defaults.
 */
record SelRuleSet(
        List<SelRuleDescriptor> rules,
        String defaultPhase,
        Map<String, List<String>> phasePatterns,
        SelOutputConfig outputConfig) {
    List<SelRuleDescriptor> rulesForPhase(String phase) {
        if ("#ALL".equals(phase)) {
            return rules;
        }
        if ("#DEFAULT".equals(phase)) {
            if (defaultPhase == null || defaultPhase.isBlank()) {
                return rules;
            }
            return rulesForNamedPhase(defaultPhase);
        }
        return rulesForNamedPhase(phase);
    }

    private List<SelRuleDescriptor> rulesForNamedPhase(String phaseId) {
        if (!phasePatterns.containsKey(phaseId)) {
            String known =
                    phasePatterns.isEmpty() ? "<none>" : String.join(", ", phasePatterns.keySet());
            throw new IllegalArgumentException(
                    "Unknown Schematron phase '" + phaseId + "'. Known phases: " + known);
        }
        Set<String> activePatterns = Set.copyOf(phasePatterns.get(phaseId));
        List<SelRuleDescriptor> selected = new ArrayList<>();
        for (SelRuleDescriptor rule : rules) {
            if (activePatterns.contains(rule.patternId())) {
                selected.add(rule);
            }
        }
        return selected;
    }
}
