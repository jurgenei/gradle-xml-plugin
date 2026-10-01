package name.jurgenei.gradle.xml.xir;

import name.jurgenei.xir.XirParser;
import name.jurgenei.xir.XirSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;

public class XirSerializerCoverageTest {

    @Test
    public void rendersLegacyModeShapesWhenRequested() throws Exception {
        String input = """
            (.
              (book { xmlns:m "urn:math" id "b1" }
                (! "legacy-comment")
                (?xml-stylesheet { type "text/xsl" href "style.xsl" })
                (m:title "XML")))
            """;

        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.BEAUTIFIED,
            XirSerializer.SyntaxMode.LEGACY
        );

        new XirParser().parse(new StringReader(input), serializer, serializer);
        String output = writer.toString();

        Assert.assertTrue(output.contains("[id \"b1\"]"));
        Assert.assertTrue(output.contains("[ns \"m\" \"urn:math\"]") || output.contains("[ns \"\" \"urn:math\"]"));
        Assert.assertTrue(output.contains("(# \"legacy-comment\")"));
        Assert.assertTrue(output.contains("(?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\")"));
    }

    @Test
    public void rendersEmptyCanonicalDocumentWhenNoEvents() throws Exception {
        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.BEAUTIFIED,
            XirSerializer.SyntaxMode.CANONICAL
        );

        serializer.startDocument();
        serializer.endDocument();

        Assert.assertEquals("(.)" + System.lineSeparator(), writer.toString());
    }

    @Test
    public void keepsRawPiDataWhenNotTokenized() throws Exception {
        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.COMPACT,
            XirSerializer.SyntaxMode.CANONICAL
        );

        serializer.startDocument();
        serializer.processingInstruction("target", "raw-data without-equals");
        serializer.endDocument();

        String output = writer.toString();
        Assert.assertTrue(output.contains("(?target {data \"raw-data without-equals\"})"));
    }
}

