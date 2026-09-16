package name.jurgenei.gradle.xml.saxon;

import java.net.URI;
import java.util.Locale;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import name.jurgenei.xml.sexpr.SExpressionXmlReader;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ResourceRequest;
import net.sf.saxon.lib.ResourceResolver;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.resource.SuppliedItemResource;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.InputSource;

/**
 * Installs Saxon URI/resource resolvers that route {@code .sexpr} resources
 * through {@link SExpressionXmlReader}.
 */
public final class SaxonSexprResolvers {

    private static final String SEXPR_MEDIA_TYPE = "application/x-sexpr+xml";

    private SaxonSexprResolvers() {
    }

    /**
     * Configures resolver hooks on a Saxon processor.
     *
     * @param processor processor to configure
     */
    public static void configure(Processor processor) {
        registerSexprCollectionFactory(processor.getUnderlyingConfiguration());
        DualSexprResolver resolver = new DualSexprResolver();
        processor.getUnderlyingConfiguration().setResourceResolver(resolver);
    }

    /**
     * Configures resolver hooks on a compiler used for include/import.
     *
     * @param compiler compiler to configure
     */
    public static void configure(net.sf.saxon.s9api.XsltCompiler compiler) {
        DualSexprResolver resolver = new DualSexprResolver();
        compiler.setResourceResolver(resolver);
        compiler.setURIResolver(resolver);
    }

    private static void registerSexprCollectionFactory(Configuration configuration) {
        configuration.registerFileExtension("sexpr", SEXPR_MEDIA_TYPE);
        configuration.registerMediaType(SEXPR_MEDIA_TYPE, (context, details) -> {
            try {
                Processor processor = new Processor(context.getConfiguration());
                net.sf.saxon.om.NodeInfo node = processor.newDocumentBuilder()
                    .build(new SAXSource(new SExpressionXmlReader(), new InputSource(details.resourceUri)))
                    .getUnderlyingNode();
                return new SuppliedItemResource(() -> node, details.resourceUri);
            } catch (SaxonApiException ex) {
                throw new XPathException("Failed to parse S-expression resource: " + details.resourceUri, ex);
            }
        });
    }

    private static final class DualSexprResolver implements ResourceResolver, URIResolver {

        @Override
        public Source resolve(ResourceRequest request) throws XPathException {
            String candidate = chooseCandidateUri(request.uri, request.relativeUri, request.baseUri);
            if (!isSexprReference(candidate)) {
                return null;
            }
            return new SAXSource(new SExpressionXmlReader(), new InputSource(candidate));
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            String candidate;
            try {
                candidate = chooseCandidateUri(null, href, base);
            } catch (IllegalArgumentException ex) {
                throw new TransformerException("Invalid URI while resolving '" + href + "' against base '" + base + "'", ex);
            }
            if (!isSexprReference(candidate)) {
                return null;
            }
            return new SAXSource(new SExpressionXmlReader(), new InputSource(candidate));
        }

        private static String chooseCandidateUri(String absoluteUri, String relativeUri, String baseUri) {
            if (notBlank(absoluteUri)) {
                return absoluteUri;
            }
            if (notBlank(relativeUri)) {
                if (!notBlank(baseUri)) {
                    return relativeUri;
                }
                return URI.create(baseUri).resolve(relativeUri).toString();
            }
            return null;
        }

        private static boolean isSexprReference(String uri) {
            if (!notBlank(uri)) {
                return false;
            }
            String normalized = uri.toLowerCase(Locale.ROOT);
            int queryIdx = normalized.indexOf('?');
            int fragmentIdx = normalized.indexOf('#');
            int end = normalized.length();
            if (queryIdx >= 0) {
                end = Math.min(end, queryIdx);
            }
            if (fragmentIdx >= 0) {
                end = Math.min(end, fragmentIdx);
            }
            return normalized.substring(0, end).endsWith(".sexpr");
        }

        private static boolean notBlank(String value) {
            return value != null && !value.isBlank();
        }
    }
}
