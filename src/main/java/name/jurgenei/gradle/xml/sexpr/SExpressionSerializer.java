package name.jurgenei.gradle.xml.sexpr;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * SAX content handler that writes a minimal S-expression representation.
 */
public final class SExpressionSerializer implements ContentHandler {
    private final Writer writer;
    private final Deque<ElementFrame> stack = new ArrayDeque<>();
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
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new SAXException("Failed to flush S-expression output", e);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) {
    }

    @Override
    public void endPrefixMapping(String prefix) {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        String name = (qName != null && !qName.isBlank()) ? qName : localName;
        ElementFrame frame = new ElementFrame(name);
        for (int i = 0; i < atts.getLength(); i++) {
            String attrName = atts.getQName(i);
            if (attrName == null || attrName.isBlank()) {
                attrName = atts.getLocalName(i);
            }
            frame.attributes.add(new Attribute(attrName, atts.getValue(i)));
        }
        stack.push(frame);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        ElementFrame frame = stack.pop();

        if (stack.isEmpty()) {
            try {
                writer.write(render(frame, 0));
                writer.write(System.lineSeparator());
            } catch (IOException e) {
                throw new SAXException("Failed to write S-expression output", e);
            }
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
        stack.peek().children.add(Child.text(quote(text)));
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) {
    }

    @Override
    public void processingInstruction(String target, String data) {
    }

    @Override
    public void skippedEntity(String name) {
    }

    private String render(ElementFrame frame, int depth) {
        if (format == OutputFormat.BEAUTIFIED) {
            return renderBeautified(frame, depth);
        }
        StringBuilder sb = new StringBuilder();
        sb.append('(').append(frame.name);
        for (Attribute attribute : frame.attributes) {
            sb.append(' ').append("(@").append(attribute.name).append(' ').append(quote(attribute.value)).append(')');
        }
        for (Child child : frame.children) {
            sb.append(' ').append(renderChildCompact(child, depth + 1));
        }
        sb.append(')');
        return sb.toString();
    }

    private String renderChildCompact(Child child, int depth) {
        if (child.element != null) {
            return render(child.element, depth);
        }
        return child.text;
    }

    private String renderBeautified(ElementFrame frame, int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(depth)).append('(').append(frame.name);
        for (Attribute attribute : frame.attributes) {
            sb.append(' ')
                .append("(@")
                .append(attribute.name)
                .append(' ')
                .append(quote(attribute.value))
                .append(')');
        }

        boolean hasElementChildren = frame.children.stream().anyMatch(child -> child.element != null);
        if (!hasElementChildren) {
            for (Child child : frame.children) {
                sb.append(' ').append(child.text);
            }
            sb.append(')');
            return sb.toString();
        }

        for (Child child : frame.children) {
            sb.append('\n').append(renderChildBeautified(child, depth + 1));
        }
        sb.append('\n').append(indent(depth)).append(')');
        return sb.toString();
    }

    private String renderChildBeautified(Child child, int depth) {
        if (child.element != null) {
            return render(child.element, depth);
        }
        return indent(depth) + child.text;
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
        private final List<Attribute> attributes = new ArrayList<>();
        private final List<Child> children = new ArrayList<>();

        private ElementFrame(String name) {
            this.name = name;
        }
    }

    private static final class Child {
        private final ElementFrame element;
        private final String text;

        private Child(ElementFrame element, String text) {
            this.element = element;
            this.text = text;
        }

        private static Child element(ElementFrame element) {
            return new Child(element, null);
        }

        private static Child text(String text) {
            return new Child(null, text);
        }
    }

    private record Attribute(String name, String value) {
    }
}

