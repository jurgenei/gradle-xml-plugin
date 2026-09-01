package name.jurgenei.gradle.xml.sexpr;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class SExpressionFormatTest {

    @Test
    public void parsesBracketSyntaxWithNamespacesCommentsAndPi() throws Exception {
        String input = """
            (m:math
              [ns \"m\" \"http://www.w3.org/1998/Math/MathML\"]
              [id \"b1\" version \"1.0\"]
              (# \"this is a comment\")
              (?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\")
              (m:mfrac
                (m:mi \"a\")
                (m:mi \"b\")))
            """;

        RecordingHandler handler = new RecordingHandler();
        new SExpressionParser().parse(new StringReader(input), handler, handler);

        Assert.assertTrue(handler.events.contains("spm:m=http://www.w3.org/1998/Math/MathML"));
        Assert.assertTrue(handler.events.contains("start:m:math:id=b1,version=1.0"));
        Assert.assertTrue(handler.events.contains("comment:this is a comment"));
        Assert.assertTrue(handler.events.contains("pi:xml-stylesheet:type=\"text/xsl\" href=\"style.xsl\""));
        Assert.assertTrue(handler.events.contains("chars:a"));
        Assert.assertTrue(handler.events.contains("chars:b"));
    }

    @Test
    public void serializesBracketSyntaxWithStablePiTokens() throws Exception {
        String input = """
            (book
              [id \"b1\" version \"1.0\"]
              (?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\")
              (title \"XML\"))
            """;

        StringWriter writer = new StringWriter();
        SExpressionSerializer serializer = new SExpressionSerializer(writer, SExpressionSerializer.OutputFormat.COMPACT);
        new SExpressionParser().parse(new StringReader(input), serializer, serializer);

        String output = writer.toString();
        Assert.assertTrue(output.contains("[id \"b1\" version \"1.0\"]"));
        Assert.assertTrue(output.contains("(?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\")"));
    }

    @Test
    public void rejectsLegacyAtAttributeSyntax() {
        String input = "(book (@id \"b1\") (title \"XML\"))";
        IOException error = Assert.assertThrows(IOException.class,
            () -> new SExpressionParser().parse(new StringReader(input), new DefaultHandler()));
        Assert.assertNotNull(error.getMessage());
    }

    private static final class RecordingHandler extends DefaultHandler implements LexicalHandler {
        private final List<String> events = new ArrayList<>();

        @Override
        public void startPrefixMapping(String prefix, String uri) {
            events.add("spm:" + prefix + "=" + uri);
        }

        @Override
        public void endPrefixMapping(String prefix) {
            events.add("epm:" + prefix);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            StringBuilder sb = new StringBuilder();
            sb.append("start:").append(qName);
            if (attributes.getLength() > 0) {
                sb.append(":");
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (i > 0) {
                        sb.append(",");
                    }
                    sb.append(attributes.getQName(i)).append("=").append(attributes.getValue(i));
                }
            }
            events.add(sb.toString());
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            events.add("end:" + qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            events.add("chars:" + new String(ch, start, length));
        }

        @Override
        public void processingInstruction(String target, String data) {
            events.add("pi:" + target + ":" + data);
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
            events.add("comment:" + new String(ch, start, length));
        }
    }
}



