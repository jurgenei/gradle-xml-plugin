package name.jurgenei.xml.sexpr;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SAX content handler that writes bracket-based S-expression representation.
 */
public final class SExpressionSerializer implements ContentHandler, LexicalHandler {
    private final Writer writer;
    private final Deque<ElementFrame> stack = new ArrayDeque<>();
    private final List<NamespaceDecl> pendingNamespaceDeclarations = new ArrayList<>();
    private final List<DocNode> documentNodes = new ArrayList<>();
    private final OutputFormat format;

    /**
     * Rendering mode for serialized S-expression output.
     */
    public enum OutputFormat {
        /** Compact single-line output with minimal whitespace. */
        COMPACT,
        /** Indented multi-line output for readability. */
        BEAUTIFIED
    }

    /**
     * Creates serializer using {@link OutputFormat#COMPACT} mode.
     *
     * @param writer destination writer
     */
    public SExpressionSerializer(Writer writer) {
        this(writer, OutputFormat.COMPACT);
    }

    /**
     * Creates serializer with explicit output format.
     *
     * @param writer destination writer
     * @param format requested rendering mode; defaults to compact when {@code null}
     */
    public SExpressionSerializer(Writer writer, OutputFormat format) {
        this.writer = writer;
        this.format = format == null ? OutputFormat.COMPACT : format;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() {
        documentNodes.clear();
        stack.clear();
        pendingNamespaceDeclarations.clear();
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            for (int i = 0; i < documentNodes.size(); i++) {
                writer.write(renderDocumentNode(documentNodes.get(i), 0));
                if (i + 1 < documentNodes.size()) {
                    writer.write(System.lineSeparator());
                }
            }
            writer.write(System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            throw new SAXException("Failed to flush S-expression output", e);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {
        pendingNamespaceDeclarations.add(new NamespaceDecl(prefix == null ? "" : prefix, uri == null ? "" : uri));
    }

    @Override
    public void endPrefixMapping(String prefix) {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        String name = (qName != null && !qName.isBlank()) ? qName : localName;
        ElementFrame frame = new ElementFrame(name);
        frame.namespaceDeclarations.addAll(pendingNamespaceDeclarations);
        pendingNamespaceDeclarations.clear();

        for (int i = 0; i < atts.getLength(); i++) {
            String attrName = atts.getQName(i);
            if (attrName == null || attrName.isBlank()) {
                attrName = atts.getLocalName(i);
            }
            frame.attributes.put(attrName, atts.getValue(i));
        }
        stack.push(frame);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        ElementFrame frame = stack.pop();
        if (stack.isEmpty()) {
            documentNodes.add(DocNode.element(frame));
            return;
        }
        stack.peek().children.add(Child.element(frame));
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (length == 0 || stack.isEmpty()) {
            return;
        }
        String text = new String(ch, start, length);
        if (text.isBlank()) {
            return;
        }
        stack.peek().children.add(Child.text(text));
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) {
    }

    @Override
    public void processingInstruction(String target, String data) {
        PiNode piNode = new PiNode(target, parsePiTokens(data));
        if (stack.isEmpty()) {
            documentNodes.add(DocNode.pi(piNode));
            return;
        }
        stack.peek().children.add(Child.pi(piNode));
    }

    @Override
    public void skippedEntity(String name) {
    }

    @Override
    public void startDTD(String name, String publicId, String systemId) {
    }

    @Override
    public void endDTD() {
    }

    @Override
    public void startEntity(String name) {
    }

    @Override
    public void endEntity(String name) {
    }

    @Override
    public void startCDATA() {
    }

    @Override
    public void endCDATA() {
    }

    @Override
    public void comment(char[] ch, int start, int length) {
        String text = new String(ch, start, length);
        if (stack.isEmpty()) {
            documentNodes.add(DocNode.comment(text));
            return;
        }
        stack.peek().children.add(Child.comment(text));
    }

    private String renderDocumentNode(DocNode node, int depth) {
        if (node.element != null) {
            return renderElement(node.element, depth);
        }
        if (node.comment != null) {
            return renderComment(node.comment);
        }
        return renderPi(node.pi);
    }

    private String renderElement(ElementFrame frame, int depth) {
        return format == OutputFormat.BEAUTIFIED
            ? renderElementBeautified(frame, depth)
            : renderElementCompact(frame, depth);
    }

    private String renderElementCompact(ElementFrame frame, int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append('(').append(frame.name);
        if (!frame.attributes.isEmpty()) {
            sb.append(' ').append(renderAttributeBlockCompact(frame.attributes));
        }
        if (!frame.namespaceDeclarations.isEmpty()) {
            sb.append(' ').append(renderNamespaceBlockCompact(frame.namespaceDeclarations));
        }
        for (Child child : frame.children) {
            sb.append(' ').append(renderChildCompact(child, depth + 1));
        }
        sb.append(')');
        return sb.toString();
    }

    private String renderElementBeautified(ElementFrame frame, int depth) {
        StringBuilder sb = new StringBuilder();
        String currentIndent = indent(depth);
        sb.append(currentIndent).append('(').append(frame.name);

        boolean hasChildren = !frame.children.isEmpty();
        boolean hasBlocks = !frame.attributes.isEmpty() || !frame.namespaceDeclarations.isEmpty();
        boolean hasStructuredChildren = frame.children.stream().anyMatch(child -> child.element != null || child.comment != null || child.pi != null);

        if (!frame.attributes.isEmpty()) {
            sb.append('\n').append(renderAttributeBlockBeautified(frame.attributes, depth + 1));
        }
        if (!frame.namespaceDeclarations.isEmpty()) {
            sb.append('\n').append(renderNamespaceBlockBeautified(frame.namespaceDeclarations, depth + 1));
        }

        if (!hasStructuredChildren && hasChildren) {
            for (Child child : frame.children) {
                sb.append(' ').append(quote(child.text));
            }
            sb.append(')');
            return sb.toString();
        }

        for (Child child : frame.children) {
            sb.append('\n').append(renderChildBeautified(child, depth + 1));
        }

        if (!hasChildren && !hasBlocks) {
            sb.append(')');
            return sb.toString();
        }

        sb.append(')');
        return sb.toString();
    }

    private String renderChildCompact(Child child, int depth) {
        if (child.element != null) {
            return renderElement(child.element, depth);
        }
        if (child.comment != null) {
            return renderComment(child.comment);
        }
        if (child.pi != null) {
            return renderPi(child.pi);
        }
        return quote(child.text);
    }

    private String renderChildBeautified(Child child, int depth) {
        if (child.element != null) {
            return renderElement(child.element, depth);
        }
        String leading = indent(depth);
        if (child.comment != null) {
            return leading + renderComment(child.comment);
        }
        if (child.pi != null) {
            return leading + renderPi(child.pi);
        }
        return leading + quote(child.text);
    }

    private String renderAttributeBlockCompact(Map<String, String> attributes) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (!first) {
                sb.append(' ');
            }
            sb.append(entry.getKey()).append(' ').append(quote(entry.getValue()));
            first = false;
        }
        sb.append(']');
        return sb.toString();
    }

    private String renderAttributeBlockBeautified(Map<String, String> attributes, int depth) {
        StringBuilder sb = new StringBuilder();
        String startIndent = indent(depth);
        String continuationIndent = startIndent + " ";
        int index = 0;

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (index == 0) {
                sb.append(startIndent).append('[');
            } else {
                sb.append('\n').append(continuationIndent);
            }
            sb.append(entry.getKey()).append(' ').append(quote(entry.getValue()));
            index++;
        }
        sb.append(']');
        return sb.toString();
    }

    private String renderNamespaceBlockCompact(List<NamespaceDecl> declarations) {
        StringBuilder sb = new StringBuilder("[ns");
        if (declarations.size() == 1 && declarations.get(0).prefix.isEmpty()) {
            sb.append(' ').append(quote(declarations.get(0).uri));
        } else {
            for (NamespaceDecl declaration : declarations) {
                sb.append(' ').append(quote(declaration.prefix)).append(' ').append(quote(declaration.uri));
            }
        }
        sb.append(']');
        return sb.toString();
    }

    private String renderNamespaceBlockBeautified(List<NamespaceDecl> declarations, int depth) {
        String compact = renderNamespaceBlockCompact(declarations);
        return indent(depth) + compact;
    }

    private String renderComment(String value) {
        return "(# " + quote(value) + ")";
    }

    private String renderPi(PiNode pi) {
        StringBuilder sb = new StringBuilder();
        sb.append("(?").append(pi.target);
        for (PiToken token : pi.tokens) {
            sb.append(' ')
                .append(token.key)
                .append('=')
                .append(quote(token.value));
        }
        sb.append(')');
        return sb.toString();
    }

    private List<PiToken> parsePiTokens(String data) {
        List<PiToken> tokens = new ArrayList<>();
        if (data == null || data.isBlank()) {
            return tokens;
        }

        Cursor cursor = new Cursor(data);
        while (true) {
            cursor.skipWhitespace();
            if (cursor.isEof()) {
                return tokens;
            }

            String key = cursor.readKey();
            if (key.isEmpty()) {
                return List.of(new PiToken("data", data));
            }
            if (!cursor.consume('=')) {
                return List.of(new PiToken("data", data));
            }
            String value = cursor.readQuoted();
            if (value == null) {
                return List.of(new PiToken("data", data));
            }
            tokens.add(new PiToken(key, value));
        }
    }

    private String indent(int depth) {
        return "  ".repeat(Math.max(0, depth));
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

    private static final class ElementFrame {
        private final String name;
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private final List<NamespaceDecl> namespaceDeclarations = new ArrayList<>();
        private final List<Child> children = new ArrayList<>();

        private ElementFrame(String name) {
            this.name = name;
        }
    }

    private record NamespaceDecl(String prefix, String uri) {
    }

    private record PiToken(String key, String value) {
    }

    private record PiNode(String target, List<PiToken> tokens) {
    }

    private static final class Child {
        private final ElementFrame element;
        private final String text;
        private final String comment;
        private final PiNode pi;

        private Child(ElementFrame element, String text, String comment, PiNode pi) {
            this.element = element;
            this.text = text;
            this.comment = comment;
            this.pi = pi;
        }

        private static Child element(ElementFrame element) {
            return new Child(element, null, null, null);
        }

        private static Child text(String text) {
            return new Child(null, text, null, null);
        }

        private static Child comment(String comment) {
            return new Child(null, null, comment, null);
        }

        private static Child pi(PiNode pi) {
            return new Child(null, null, null, pi);
        }
    }

    private static final class DocNode {
        private final ElementFrame element;
        private final String comment;
        private final PiNode pi;

        private DocNode(ElementFrame element, String comment, PiNode pi) {
            this.element = element;
            this.comment = comment;
            this.pi = pi;
        }

        private static DocNode element(ElementFrame element) {
            return new DocNode(element, null, null);
        }

        private static DocNode comment(String comment) {
            return new DocNode(null, comment, null);
        }

        private static DocNode pi(PiNode pi) {
            return new DocNode(null, null, pi);
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

        private void skipWhitespace() {
            while (!isEof() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private String readKey() {
            int start = index;
            while (!isEof()) {
                char ch = source.charAt(index);
                if (Character.isWhitespace(ch) || ch == '=') {
                    break;
                }
                index++;
            }
            return source.substring(start, index);
        }

        private boolean consume(char expected) {
            if (isEof() || source.charAt(index) != expected) {
                return false;
            }
            index++;
            return true;
        }

        private String readQuoted() {
            if (isEof() || source.charAt(index) != '"') {
                return null;
            }
            index++;
            StringBuilder sb = new StringBuilder();
            while (!isEof()) {
                char ch = source.charAt(index++);
                if (ch == '"') {
                    return sb.toString();
                }
                if (ch == '\\') {
                    if (isEof()) {
                        return null;
                    }
                    char escaped = source.charAt(index++);
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
            return null;
        }
    }
}

