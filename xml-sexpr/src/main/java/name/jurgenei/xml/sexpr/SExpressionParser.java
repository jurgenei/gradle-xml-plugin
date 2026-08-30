package name.jurgenei.xml.sexpr;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses bracket-based S-expression format into SAX events.
 */
public final class SExpressionParser {

    /**
     * Creates parser for bracket-based S-expression syntax.
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
        LexicalHandler lexical = handler instanceof LexicalHandler value ? value : null;
        parse(reader, handler, lexical);
    }

    /**
     * Parses S-expression input and emits SAX events including optional lexical events.
     *
     * @param reader character stream containing S-expression document
     * @param handler SAX handler receiving parsed events
     * @param lexical optional lexical handler for comments
     * @throws IOException if input is malformed or cannot be read
     * @throws SAXException if SAX handler fails while consuming events
     */
    public void parse(Reader reader, ContentHandler handler, LexicalHandler lexical) throws IOException, SAXException {
        StringBuilder source = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            source.append(buffer, 0, read);
        }

        Cursor cursor = new Cursor(source.toString());
        cursor.skipTrivia();
        Item root = parseItem(cursor);
        if (!(root instanceof ElementNode elementRoot)) {
            throw new IOException("Root item must be element at position " + cursor.position());
        }

        cursor.skipTrivia();
        if (!cursor.isEof()) {
            throw new IOException("Unexpected trailing content at position " + cursor.position());
        }

        handler.startDocument();
        emitElement(elementRoot, handler, lexical, new ArrayDeque<>());
        handler.endDocument();
    }

    private Item parseItem(Cursor cursor) throws IOException {
        cursor.expect('(');
        cursor.skipTrivia();
        String head = cursor.readSymbol();
        if (head.isEmpty()) {
            throw new IOException("Missing item head at position " + cursor.position());
        }

        if ("#".equals(head)) {
            String value = parseComment(cursor);
            return new CommentNode(value);
        }
        if (head.startsWith("?")) {
            PiNode pi = parseProcessingInstruction(cursor, head.substring(1));
            return pi;
        }
        if (head.startsWith("@")) {
            throw new IOException("Legacy @-attribute syntax not supported at position " + cursor.position());
        }

        ElementNode node = new ElementNode(head);
        while (true) {
            cursor.skipTrivia();
            if (cursor.isEof()) {
                throw new IOException("Unexpected end of input while parsing node '" + head + "'");
            }
            char ch = cursor.peek();
            if (ch == ')') {
                cursor.next();
                return node;
            }
            if (ch == '(') {
                Item child = parseItem(cursor);
                node.children.add(child);
                continue;
            }
            if (ch == '[') {
                parseBracketBlock(cursor, node);
                continue;
            }
            if (ch == '"') {
                node.children.add(new TextNode(cursor.readString()));
                continue;
            }
            throw new IOException("Unexpected token in element body at position " + cursor.position());
        }
    }

    private String parseComment(Cursor cursor) throws IOException {
        cursor.skipTrivia();
        if (cursor.peek() != '"') {
            throw new IOException("Comment value must be quoted string at position " + cursor.position());
        }
        String value = cursor.readString();
        cursor.skipTrivia();
        cursor.expect(')');
        return value;
    }

    private PiNode parseProcessingInstruction(Cursor cursor, String target) throws IOException {
        if (target.isBlank()) {
            throw new IOException("Processing instruction target missing at position " + cursor.position());
        }

        List<PiToken> tokens = new ArrayList<>();
        while (true) {
            cursor.skipTrivia();
            if (cursor.isEof()) {
                throw new IOException("Unexpected end of input in processing instruction '" + target + "'");
            }
            if (cursor.peek() == ')') {
                cursor.next();
                return new PiNode(target, tokens);
            }

            String key = cursor.readSymbol();
            if (key.isEmpty()) {
                throw new IOException("Processing instruction key missing at position " + cursor.position());
            }
            cursor.expect('=');
            if (cursor.peek() != '"') {
                throw new IOException("Processing instruction value must be quoted at position " + cursor.position());
            }
            String value = cursor.readString();
            tokens.add(new PiToken(key, value));
        }
    }

    private void parseBracketBlock(Cursor cursor, ElementNode node) throws IOException {
        cursor.expect('[');
        cursor.skipTrivia();
        if (cursor.peek() == ']') {
            throw new IOException("Empty bracket block not allowed at position " + cursor.position());
        }

        String first = cursor.readSymbol();
        if (first.isBlank()) {
            throw new IOException("Bracket block key missing at position " + cursor.position());
        }

        if ("ns".equals(first)) {
            parseNamespaceBlock(cursor, node);
            return;
        }

        String key = first;
        while (true) {
            cursor.skipTrivia();
            if (cursor.peek() != '"') {
                throw new IOException("Attribute value for '" + key + "' must be quoted at position " + cursor.position());
            }
            node.attributes.put(key, cursor.readString());
            cursor.skipTrivia();

            if (cursor.peek() == ']') {
                cursor.next();
                return;
            }

            key = cursor.readSymbol();
            if (key.isBlank()) {
                throw new IOException("Attribute name missing at position " + cursor.position());
            }
        }
    }

    private void parseNamespaceBlock(Cursor cursor, ElementNode node) throws IOException {
        List<String> values = new ArrayList<>();
        while (true) {
            cursor.skipTrivia();
            if (cursor.isEof()) {
                throw new IOException("Unexpected end of input in namespace block");
            }
            if (cursor.peek() == ']') {
                cursor.next();
                break;
            }
            if (cursor.peek() != '"') {
                throw new IOException("Namespace entries must be quoted strings at position " + cursor.position());
            }
            values.add(cursor.readString());
        }

        if (values.isEmpty()) {
            throw new IOException("Namespace block requires at least one quoted value");
        }

        if (values.size() == 1) {
            node.namespaceDeclarations.add(new NamespaceDecl("", values.get(0)));
            return;
        }

        if ((values.size() % 2) != 0) {
            throw new IOException("Namespace block requires one default URI or prefix/URI pairs");
        }

        for (int i = 0; i < values.size(); i += 2) {
            node.namespaceDeclarations.add(new NamespaceDecl(values.get(i), values.get(i + 1)));
        }
    }

    private void emitElement(ElementNode node, ContentHandler handler, LexicalHandler lexical, Deque<Map<String, String>> namespaceStack) throws SAXException {
        Map<String, String> parent = namespaceStack.isEmpty() ? Map.of() : namespaceStack.peek();
        Map<String, String> current = new LinkedHashMap<>(parent);

        for (NamespaceDecl declaration : node.namespaceDeclarations) {
            String prefix = declaration.prefix;
            String uri = declaration.uri;
            handler.startPrefixMapping(prefix, uri);
            current.put(prefix, uri);
        }

        String elementPrefix = prefixOf(node.name);
        String elementUri = current.getOrDefault(elementPrefix, "");
        String elementLocal = localNameOf(node.name);

        AttributesImpl attributes = new AttributesImpl();
        for (Map.Entry<String, String> entry : node.attributes.entrySet()) {
            String attrName = entry.getKey();
            String attrPrefix = prefixOf(attrName);
            String attrUri = attrPrefix.isEmpty() ? "" : current.getOrDefault(attrPrefix, "");
            String attrLocal = localNameOf(attrName);
            attributes.addAttribute(attrUri, attrLocal, attrName, "CDATA", entry.getValue());
        }

        namespaceStack.push(current);
        handler.startElement(elementUri, elementLocal, node.name, attributes);

        for (Item child : node.children) {
            if (child instanceof TextNode textNode) {
                if (textNode.value.isBlank()) {
                    continue;
                }
                char[] chars = textNode.value.toCharArray();
                handler.characters(chars, 0, chars.length);
            } else if (child instanceof ElementNode nested) {
                emitElement(nested, handler, lexical, namespaceStack);
            } else if (child instanceof CommentNode commentNode) {
                if (lexical != null) {
                    char[] chars = commentNode.value.toCharArray();
                    lexical.comment(chars, 0, chars.length);
                }
            } else if (child instanceof PiNode piNode) {
                handler.processingInstruction(piNode.target, toPiData(piNode.tokens));
            }
        }

        handler.endElement(elementUri, elementLocal, node.name);
        namespaceStack.pop();

        for (int i = node.namespaceDeclarations.size() - 1; i >= 0; i--) {
            handler.endPrefixMapping(node.namespaceDeclarations.get(i).prefix);
        }
    }

    private String toPiData(List<PiToken> tokens) {
        StringBuilder sb = new StringBuilder();
        for (PiToken token : tokens) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(token.key)
                .append('=')
                .append(quote(token.value));
        }
        return sb.toString();
    }

    private String quote(String value) {
        String escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
        return '"' + escaped + '"';
    }

    private String prefixOf(String qName) {
        int index = qName.indexOf(':');
        if (index <= 0) {
            return "";
        }
        return qName.substring(0, index);
    }

    private String localNameOf(String qName) {
        int index = qName.indexOf(':');
        if (index < 0 || index + 1 >= qName.length()) {
            return qName;
        }
        return qName.substring(index + 1);
    }

    private sealed interface Item permits ElementNode, TextNode, CommentNode, PiNode {
    }

    private static final class ElementNode implements Item {
        private final String name;
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final List<NamespaceDecl> namespaceDeclarations = new ArrayList<>();
        private final List<Item> children = new ArrayList<>();

        private ElementNode(String name) {
            this.name = name;
        }
    }

    private record TextNode(String value) implements Item {
    }

    private record CommentNode(String value) implements Item {
    }

    private record PiNode(String target, List<PiToken> tokens) implements Item {
    }

    private record NamespaceDecl(String prefix, String uri) {
    }

    private record PiToken(String key, String value) {
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
                if (Character.isWhitespace(ch)
                    || ch == '('
                    || ch == ')'
                    || ch == '['
                    || ch == ']'
                    || ch == '"'
                    || ch == '=') {
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

