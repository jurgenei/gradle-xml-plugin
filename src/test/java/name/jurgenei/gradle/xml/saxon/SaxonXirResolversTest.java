package name.jurgenei.gradle.xml.saxon;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XsltCompiler;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SaxonXirResolversTest {

    @Test
    public void configuresProcessorAndCompiler() {
        Processor processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();

        SaxonXirResolvers.configure(processor);
        SaxonXirResolvers.configure(compiler);
    }

    @Test
    public void resolvesXirUrisAndSkipsNonXir() throws Exception {
        Object resolver = newResolver();
        ResourceResolver resourceResolver = (ResourceResolver) resolver;
        URIResolver uriResolver = (URIResolver) resolver;

        ResourceRequest direct = new ResourceRequest();
        direct.uri = "file:/tmp/direct.xir";
        assertNotNull(resourceResolver.resolve(direct));

        ResourceRequest fromRelative = new ResourceRequest();
        fromRelative.relativeUri = "lookup.xir";
        fromRelative.baseUri = "file:/tmp/base/main.xsl";
        Source resolvedRelative = resourceResolver.resolve(fromRelative);
        assertNotNull(resolvedRelative);

        ResourceRequest xml = new ResourceRequest();
        xml.uri = "file:/tmp/not-xir.xml";
        assertNull(resourceResolver.resolve(xml));

        Source xirWithQueryAndFragment = uriResolver.resolve("lookup.xir?version=1#fragment", "file:/tmp/base/main.xsl");
        assertNotNull(xirWithQueryAndFragment);
        assertNull(uriResolver.resolve("lookup.xml", "file:/tmp/base/main.xsl"));
    }

    @Test
    public void wrapsInvalidBaseUriAsTransformerException() throws Exception {
        URIResolver uriResolver = (URIResolver) newResolver();
        try {
            uriResolver.resolve("lookup.xir", "::invalid-base::");
            fail("Expected TransformerException");
        } catch (TransformerException ex) {
            assertTrue(ex.getMessage().contains("Invalid URI while resolving"));
        }
    }

    private static Object newResolver() throws Exception {
        Class<?> resolverClass = Class.forName("name.jurgenei.gradle.xml.saxon.SaxonXirResolvers$DualXirResolver");
        Constructor<?> constructor = resolverClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object resolver = constructor.newInstance();

        Method notBlank = resolverClass.getDeclaredMethod("notBlank", String.class);
        notBlank.setAccessible(true);
        assertTrue((Boolean) notBlank.invoke(null, "x"));
        return resolver;
    }
}
