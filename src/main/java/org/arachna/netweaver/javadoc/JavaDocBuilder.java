package org.arachna.netweaver.javadoc;

import hudson.Extension;
import hudson.Launcher;
import hudson.PluginFirstClassLoader;
import hudson.PluginWrapper;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.util.Collection;

import net.sf.json.JSONObject;

import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.hudson.nwdi.NWDIBuild;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link Builder} for JavaDoc documentation for SAP NetWeaver development
 * components.
 * 
 * @author Dirk Weigenand
 */
public class JavaDocBuilder extends Builder {
    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public JavaDocBuilder() {
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final NWDIBuild nwdiBuild = (NWDIBuild)build;
        final Collection<DevelopmentComponent> components = nwdiBuild.getAffectedDevelopmentComponents();

        final PluginWrapper pluginWrapper = Hudson.getInstance().getPluginManager().getPlugin("NWDI-Checkstyle-Plugin");
        final PluginFirstClassLoader pluginFirstClassLoader = (PluginFirstClassLoader)pluginWrapper.classLoader;
        Thread.currentThread().setContextClassLoader(pluginFirstClassLoader);

        final JavaDocExecutor executor = new JavaDocExecutor(nwdiBuild.getAntHelper(listener.getLogger()));

        for (final DevelopmentComponent component : components) {
            executor.execute(component);
        }

        return true;
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
     * 
     * <p>
     * See
     * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return NWDIBuild.class.equals(aClass);
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "NWDI JavaDoc";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }
    }
}
