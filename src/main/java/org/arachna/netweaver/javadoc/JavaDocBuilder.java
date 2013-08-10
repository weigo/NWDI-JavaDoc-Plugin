package org.arachna.netweaver.javadoc;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.velocity.app.VelocityEngine;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.hudson.nwdi.AntTaskBuilder;
import org.arachna.netweaver.hudson.nwdi.DCWithJavaSourceAcceptingFilter;
import org.arachna.netweaver.hudson.nwdi.NWDIBuild;
import org.arachna.netweaver.hudson.nwdi.NWDIProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link Builder} for JavaDoc documentation for SAP NetWeaver development
 * components.
 * 
 * @author Dirk Weigenand
 */
public class JavaDocBuilder extends AntTaskBuilder {
    /**
     * URLs to JavaDoc generated elsewhere that should be linked into the
     * generated javadoc documentation.
     */
    private final Collection<String> links = new HashSet<String>();

    private boolean useUmlGraph;

    /**
     * @return the useUmlGraph
     */
    public final boolean getUseUmlGraph() {
        return useUmlGraph;
    }

    public void setUseUmlGraph(final boolean useUmlGraph) {
        this.useUmlGraph = useUmlGraph;
    }

    /**
     * @return the links
     */
    public Collection<String> getLinks() {
        return links;
    }

    public void setLinks(final Collection<String> links) {
        if (links != null) {
            for (final String link : links) {
                addLink(link);
            }
        }
    }

    void addLink(final String link) {
        final String l = Util.fixEmpty(link);

        if (l != null) {
            links.add(l);
        }
    }

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public JavaDocBuilder(final Collection<String> links, final boolean useUmlGraph) {
        setLinks(links);
        this.useUmlGraph = useUmlGraph;
    }

    public JavaDocBuilder() {
    }

    /**
     * Generate JavaDoc documentation for the development components modified in
     * the current build.
     * 
     * @param build
     *            the build ask for modified development components.
     * @param launcher
     *            launcher to use to execute the Ant JavaDoc task.
     * @param listener
     *            the listener to use for logging.
     */
    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final NWDIBuild nwdiBuild = (NWDIBuild)build;
        final VelocityEngine velocityEngine = getVelocityEngine();
        final BuildFileGenerator generator =
            new BuildFileGenerator(getAntHelper(), nwdiBuild.getDevelopmentComponentFactory(), links, velocityEngine, useUmlGraph);

        try {
            for (final DevelopmentComponent component : nwdiBuild.getAffectedDevelopmentComponents(new DCWithJavaSourceAcceptingFilter())) {
                final String location = generator.execute(component);

                if (location != null) {
                    execute(nwdiBuild, launcher, listener, "javadoc", location, null);
                }
            }

            final OverviewGenerator overview =
                new OverviewGenerator(new File(getAntHelper().getPathToWorkspace()), nwdiBuild.getDevelopmentConfiguration());
            overview.execute();
        }
        catch (final InterruptedException e) {
            // simply quit execution.
        }

        return true;
    }

    /**
     * Get the properties to use when calling ant.
     * 
     * @return the properties to use when calling ant.
     */
    @Override
    protected String getAntProperties() {
        return String.format("umlgraph.dir=%s/plugins/NWDI-JavaDoc-Plugin/WEB-INF/lib", Hudson.getInstance().root.getAbsolutePath()
            .replace("\\", "/"));
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link JavaDocBuilder}. Used as a singleton. The class is
     * marked as public so that it can be accessed from views.
     */
    @Extension
    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return NWDIProject.class.equals(aClass);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "NWDI JavaDoc Builder";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Builder newInstance(final StaplerRequest req, final JSONObject formData) throws FormException {
            final JavaDocBuilder builder = new JavaDocBuilder();

            builder.setUseUmlGraph(Boolean.valueOf(formData.getString("useUmlGraph")));
            final JSONObject config = (JSONObject)formData.get("advancedConfiguration");

            if (config != null) {
                final JSONObject linkConfig = config.getJSONObject("links");

                if (linkConfig.isArray()) {
                    final JSONArray linkCfg = config.getJSONArray("links");

                    for (int i = 0; i < linkCfg.size(); i++) {
                        final JSONObject entry = linkCfg.getJSONObject(i);
                        builder.addLink(entry.getString("link"));
                    }
                }
                else {
                    builder.addLink(linkConfig.getString("link"));
                }
            }

            return builder;
        }
    }
}
