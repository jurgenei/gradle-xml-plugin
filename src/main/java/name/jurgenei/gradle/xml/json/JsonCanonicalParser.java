package name.jurgenei.gradle.xml.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Parses canonical JSON element representation into SAX events.
 */
public final class JsonCanonicalParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Creates parser for canonical JSON/XML mapping.
     */
    public JsonCanonicalParser() {
    }

    /**
     * Parses canonical JSON input and emits equivalent SAX events.
     *
     * @param reader character stream containing canonical JSON document
     * @param handler SAX handler receiving parsed events
     * @throws IOException if input is malformed or cannot be read
     * @throws SAXException if SAX handler fails while consuming events
     */
    public void parse(Reader reader, ContentHandler handler) throws IOException, SAXException {
        JsonNode root = MAPPER.readTree(reader);
        if (root == null || root.isNull()) {
            throw new IOException("Canonical JSON input is empty");
        }

        handler.startDocument();
        emitElement(root, handler);
        handler.endDocument();
    }

    private void emitElement(JsonNode node, ContentHandler handler) throws IOException, SAXException {
        if (!node.isObject()) {
            throw new IOException("Expected object node for element, got: " + node.getNodeType());
        }

        String type = textOrNull(node.get("type"));
        if (type != null && !"element".equals(type)) {
            throw new IOException("Expected element node type, got: " + type);
        }

        String name = textOrNull(node.get("name"));
        if (name == null || name.isBlank()) {
            throw new IOException("Element node requires non-empty 'name'");
        }

        AttributesImpl attributes = new AttributesImpl();
        JsonNode attributesNode = node.get("attributes");
        if (attributesNode != null && !attributesNode.isNull()) {
            if (!attributesNode.isObject()) {
                throw new IOException("'attributes' must be an object for element: " + name);
            }
            Iterator<Map.Entry<String, JsonNode>> it = attributesNode.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String attrName = entry.getKey();
                String attrValue = scalarToString(entry.getValue());
                attributes.addAttribute("", attrName, attrName, "CDATA", attrValue);
            }
        }

        handler.startElement("", name, name, attributes);

        JsonNode childrenNode = node.get("children");
        if (childrenNode != null && !childrenNode.isNull()) {
            if (!childrenNode.isArray()) {
                throw new IOException("'children' must be an array for element: " + name);
            }
            emitChildren((ArrayNode) childrenNode, handler, name);
        }

        handler.endElement("", name, name);
    }

    private void emitChildren(ArrayNode children, ContentHandler handler, String parentName) throws IOException, SAXException {
        for (JsonNode child : children) {
            if (child == null || child.isNull()) {
                continue;
            }

            if (child.isTextual() || child.isNumber() || child.isBoolean()) {
                emitText(scalarToString(child), handler);
                continue;
            }

            if (!child.isObject()) {
                throw new IOException("Unsupported child node type under element '" + parentName + "': " + child.getNodeType());
            }

            String childType = textOrNull(child.get("type"));
            if ("text".equals(childType)) {
                emitText(scalarToString(child.get("value")), handler);
                continue;
            }

            if ("element".equals(childType) || child.hasNonNull("name")) {
                emitElement(child, handler);
                continue;
            }

            throw new IOException("Unsupported object child under element '" + parentName + "': missing recognized 'type'");
        }
    }

    private void emitText(String value, ContentHandler handler) throws SAXException {
        if (value == null || value.isBlank()) {
            return;
        }
        char[] chars = value.toCharArray();
        handler.characters(chars, 0, chars.length);
    }

    private String scalarToString(JsonNode node) throws IOException {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isObject() || node.isArray()) {
            throw new IOException("Expected scalar JSON value, got " + node.getNodeType());
        }
        return node.asText();
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }
}

