package name.jurgenei.gradle.xml;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Registers the XML transform plugin marker and exposes task types for build scripts.
 *
 * <p>The plugin does not add tasks automatically; consumers register {@link XsltTask}
 * and {@link XQueryTask} explicitly to configure input/output behavior per use case.</p>
 */
public class XmlTransformPlugin implements Plugin<Project> {
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

