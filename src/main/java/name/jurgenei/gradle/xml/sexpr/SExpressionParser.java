package name.jurgenei.gradle.xml.sexpr;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses a minimal S-expression format into SAX events.
 */
public final class SExpressionParser {

    /**
     * Creates parser for minimal S-expression syntax.
     */
    public SExpressionParser() {
    }

    /**
     * Parses S-expression input and emits equivalent SAX events.
     *
     * @param reader character stream containing S-expression document
     * @param handler SAX handler receiving parsed events
     * @throws IOException if input is malformed or cannot be read
     * @throws SAXException if SAX handler fails while consuming events
     */
    public void parse(Reader reader, ContentHandler handler) throws IOException, SAXException {
        StringBuilder source = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            source.append(buffer, 0, read);
        }

        Cursor cursor = new Cursor(source.toString());
        cursor.skipTrivia();
        Node root = parseNode(cursor);
        cursor.skipTrivia();
        if (!cursor.isEof()) {
            throw new IOException("Unexpected trailing content at position " + cursor.position());
        }

        handler.startDocument();
        emit(root, handler);
        handler.endDocument();
    }

    private Node parseNode(Cursor cursor) throws IOException {
        cursor.expect('(');
        cursor.skipTrivia();
        String name = cursor.readSymbol();
        if (name.isEmpty()) {
            throw new IOException("Missing node name at position " + cursor.position());
        }

        Node node = new Node(name);
        while (true) {
            cursor.skipTrivia();
            if (cursor.isEof()) {
                throw new IOException("Unexpected end of input while parsing node '" + name + "'");
            }
            char ch = cursor.peek();
            if (ch == ')') {
                cursor.next();
                return node;
            }
            if (ch == '(') {
                Node child = parseNode(cursor);
                if (child.name.startsWith("@") && child.children.size() == 1 && child.children.get(0) instanceof String value) {
                    node.attributes.put(child.name.substring(1), value);
                } else {
                    node.children.add(child);
                }
                continue;
            }
            if (ch == '"') {
                node.children.add(cursor.readString());
                continue;
            }
            node.children.add(cursor.readSymbol());
        }
    }

    private void emit(Node node, ContentHandler handler) throws SAXException {
        AttributesImpl attributes = new AttributesImpl();
        for (Map.Entry<String, String> entry : node.attributes.entrySet()) {
            attributes.addAttribute("", entry.getKey(), entry.getKey(), "CDATA", entry.getValue());
        }

        handler.startElement("", node.name, node.name, attributes);
        for (Object child : node.children) {
            if (child instanceof String text) {
                if (text.isBlank()) {
                    continue;
                }
                char[] chars = text.toCharArray();
                handler.characters(chars, 0, chars.length);
            } else if (child instanceof Node nested) {
                emit(nested, handler);
            }
        }
        handler.endElement("", node.name, node.name);
    }

    private static final class Node {
        private final String name;
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final List<Object> children = new ArrayList<>();

        private Node(String name) {
            this.name = name;
        }
    }

    private static final class Cursor {
        private final String source;
        private int index;

        private Cursor(String source) {
            this.source = source;
        }

        private boolean isEof() {
            return index >= source.length();
        }

        private int position() {
            return index;
        }

        private char peek() {
            return source.charAt(index);
        }

        private char next() {
            return source.charAt(index++);
        }

        private void expect(char expected) throws IOException {
            if (isEof() || next() != expected) {
                throw new IOException("Expected '" + expected + "' at position " + index);
            }
        }

        private void skipTrivia() {
            while (!isEof()) {
                char ch = peek();
                if (Character.isWhitespace(ch)) {
                    index++;
                    continue;
                }
                if (ch == ';') {
                    while (!isEof() && peek() != '\n') {
                        index++;
                    }
                    continue;
                }
                return;
            }
        }

        private String readSymbol() {
            int start = index;
            while (!isEof()) {
                char ch = peek();
                if (Character.isWhitespace(ch) || ch == '(' || ch == ')' || ch == '"') {
                    break;
                }
                index++;
            }
            return source.substring(start, index);
        }

        private String readString() throws IOException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (!isEof()) {
                char ch = next();
                if (ch == '"') {
                    return sb.toString();
                }
                if (ch == '\\') {
                    if (isEof()) {
                        throw new IOException("Unexpected end of input in string escape");
                    }
                    char escaped = next();
                    switch (escaped) {
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        default -> sb.append(escaped);
                    }
                    continue;
                }
                sb.append(ch);
            }
            throw new IOException("Unterminated string literal at position " + index);
        }
    }
}

