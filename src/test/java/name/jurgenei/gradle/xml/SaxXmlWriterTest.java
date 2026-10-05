/* (C)2026 */
package name.jurgenei.gradle.xml;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Unit tests for {@link SaxXmlWriter}.
 */
public class SaxXmlWriterTest {

    @Test
    public void writesPrettyXmlWithNamespacesAttributesAndText() throws Exception {
        SaxXmlWriter writer = SaxXmlWriter.createPretty();
        writer.startDocument();
        writer.startElement(
                "urn:test",
                "root",
                "t:root",
                List.of(SaxXmlWriter.attr("id", "A&B")),
                Map.of("t", "urn:test"));
        writer.comment("generated");
        writer.text("hello <world>");
        writer.emptyElement(
                "urn:test",
                "child",
                "t:child",
                List.of(SaxXmlWriter.attr("value", "\"quoted\"")),
                Map.of());
        writer.endElement("urn:test", "root", "t:root");
        writer.endDocument();

        String xml = writer.build();
        assertTrue(
                "xml declaration missing",
                xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));

        Document document = parse(xml);
        Element root = document.getDocumentElement();
        assertTrue("root element local name mismatch", "root".equals(root.getLocalName()));
        assertTrue("root id attribute mismatch", "A&B".equals(root.getAttribute("id")));

        NodeList children = root.getElementsByTagNameNS("urn:test", "child");
        assertTrue("expected one urn:test child", children.getLength() == 1);
        Element child = (Element) children.item(0);
        assertTrue(
                "child value attribute mismatch", "\"quoted\"".equals(child.getAttribute("value")));
        assertTrue(
                "root text missing escaped content",
                root.getTextContent().contains("hello <world>"));
    }

    @Test
    public void appendsXmlFragmentWithNamespaceHints() throws Exception {
        SaxXmlWriter writer = SaxXmlWriter.createPretty();
        writer.startDocument();
        writer.startElement("", "doc", "doc", List.of(), Map.of());
        writer.appendFragment(
                "<x:note x:kind=\"k\"><x:text>v</x:text></x:note>", Map.of("x", "urn:frag"));
        writer.endElement("", "doc", "doc");
        writer.endDocument();

        String xml = writer.build();
        Document document = parse(xml);
        NodeList notes = document.getElementsByTagNameNS("urn:frag", "note");
        assertTrue("expected one urn:frag note element", notes.getLength() == 1);
        Element note = (Element) notes.item(0);
        assertTrue(
                "namespaced attribute kind mismatch",
                "k".equals(note.getAttributeNS("urn:frag", "kind")));
        NodeList texts = note.getElementsByTagNameNS("urn:frag", "text");
        assertTrue("expected one urn:frag text element", texts.getLength() == 1);
        assertTrue("fragment text mismatch", "v".equals(texts.item(0).getTextContent()));
    }

    @Test
    public void supportsNamespacedAttributes() throws Exception {
        SaxXmlWriter writer = SaxXmlWriter.createPretty();
        writer.startDocument();
        writer.startElement(
                "urn:test",
                "root",
                "t:root",
                List.of(SaxXmlWriter.attr("urn:test", "flag", "t:flag", "yes")),
                Map.of("t", "urn:test"));
        writer.endElement("urn:test", "root", "t:root");
        writer.endDocument();

        String xml = writer.build();
        assertTrue(xml.contains("t:flag=\"yes\""));
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
}
