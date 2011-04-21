package org.arachna.netweaver.javadoc;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import net.sf.json.JSONObject;

import org.apache.tools.ant.BuildException;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
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
public class JavaDocBuilder extends Builder {

    /**
     * URLs to JavaDoc generated elsewhere that should be linked into the
     * generated javadoc documentation.
     */
    private Collection<URL> links = new HashSet<URL>();

    /**
     * @return the links
     */
    public String getLinks() {
        StringBuilder links = new StringBuilder();

        for (URL link : this.links) {
            links.append(link.toString()).append('\n');
        }

        return links.toString();
    }

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public JavaDocBuilder(String links) {
        setLinks(links);
    }

    private void setLinks(String links) {
        if (links != null) {
            for (String link : links.split("\n\r")) {
                String trimmedLink = link.trim();

                if (trimmedLink.length() > 0) {
                    try {
                        this.links.add(new URL(trimmedLink));
                    }
                    catch (MalformedURLException mfue) {
                        // FIXME: Should say something about malformed package
                        // list urls
                    }
                }
            }
        }
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        final NWDIBuild nwdiBuild = (NWDIBuild)build;
        final JavaDocExecutor executor =
            new JavaDocExecutor(nwdiBuild.getDevelopmentConfiguration(), nwdiBuild.getAntHelper(listener.getLogger()),
                nwdiBuild.getDevelopmentComponentFactory(), this.links);

        for (final DevelopmentComponent component : nwdiBuild
            .getAffectedDevelopmentComponents(new DCWithJavaSourceAcceptingFilter())) {
            long start = System.currentTimeMillis();

            try {
                listener.getLogger().append(
                    String.format("Running JavaDoc on %s:%s...", component.getVendor(), component.getName()));
                executor.execute(component);
            }
            catch (BuildException be) {
                listener.getLogger().append(
                    String.format("\n%s: %s\n", be.getLocalizedMessage().trim(), component.getSourceFolders()));
            }
            finally {
                listener.getLogger().append(
                    String.format(" (%f sec.).\n", (System.currentTimeMillis() - start) / 1000f));
            }
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
            return "NWDI JavaDoc";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }
    }
}
