package name.jurgenei.gradle.xml;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Registers the XML transform plugin marker and exposes task types for build scripts.
 *
 * <p>The plugin does not add tasks automatically; consumers register task types explicitly,
 * including {@link XsltTask}, {@link XQueryTask}, {@link SchematronTask}, {@link XsdTask},
 * {@link SchematronBootstrapTask}, {@link SchematronObservationCompileTask}, and
 * {@link SchematronExtractTask}.</p>
 */
public class XmlTransformPlugin implements Plugin<Project> {
    /**
     * Creates the plugin instance.
     */
    public XmlTransformPlugin() {
    }

    /**
     * Applies the plugin to a project.
     *
     * @param project Gradle project receiving the plugin
     */
    @Override
    public void apply(Project project) {
        // Task types are available by class once the plugin is applied.
    }
}

