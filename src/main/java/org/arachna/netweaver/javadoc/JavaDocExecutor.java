/**
 *
 */
package org.arachna.netweaver.javadoc;

import hudson.model.Hudson;

import java.io.File;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javadoc;
import org.apache.tools.ant.taskdefs.Javadoc.LinkArgument;
import org.apache.tools.ant.types.FileSet;
import org.arachna.ant.AntHelper;
import org.arachna.ant.ExcludesFactory;
import org.arachna.netweaver.dc.types.Compartment;
import org.arachna.netweaver.dc.types.CompartmentState;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.DevelopmentConfiguration;
import org.arachna.netweaver.dc.types.PublicPartReference;
import org.arachna.netweaver.dctool.JdkHomeAlias;

/**
 * Wrapper for the Ant JavaDoc task. Sets up the task and executes it.
 *
 * @author Dirk Weigenand
 */
final class JavaDocExecutor {
    /**
     * Helper class for setting up an ant task with class path, source file sets
     * etc.
     */
    private final AntHelper antHelper;

    /**
     * The HTTP-Proxy to use iff available.
     */
    private final InetSocketAddress proxy;

    /**
     * links to add to existing javadoc documentation
     */
    private final Collection<URL> links;

    /**
     * Registry for development components.
     */
    private final DevelopmentComponentFactory dcFactory;

    private DevelopmentConfiguration developmentConfiguration;

    /**
     * Create an executor for executing the Javadoc ant task using the given ant
     * helper object and links to related javadoc documentation.
     *
     * @param developmentConfiguration
     *
     * @param antHelper
     *            helper for populating an ant task with source filesets and
     *            class path for a given development component
     * @param links
     *            links to add to existing javadoc documentation
     */
    JavaDocExecutor(final DevelopmentConfiguration developmentConfiguration, final AntHelper antHelper,
        final DevelopmentComponentFactory dcFactory, Collection<URL> links) {
        this.developmentConfiguration = developmentConfiguration;
        this.antHelper = antHelper;
        this.dcFactory = dcFactory;
        this.links = links;
        proxy = (InetSocketAddress)Hudson.getInstance().proxy.createProxy().address();
    }

    JavaDocExecutor(final DevelopmentConfiguration developmentConfiguration, final AntHelper antHelper,
        final DevelopmentComponentFactory dcFactory, Collection<URL> links, InetSocketAddress proxy) {
        this.developmentConfiguration = developmentConfiguration;
        this.antHelper = antHelper;
        this.dcFactory = dcFactory;
        this.links = links;
        this.proxy = proxy;
    }

    /**
     * Run the Ant Javadoc task for the given development component.
     *
     * @param component
     *            development component to document with JavaDoc.
     */
    public void execute(final DevelopmentComponent component) {
        File javaDocFolder = new File(this.getJavaDocFolder(component));

        if (!javaDocFolder.exists() && !javaDocFolder.mkdirs()) {
            throw new RuntimeException(String.format("Can't create %s!", this.getJavaDocFolder(component)));
        }

        final Javadoc task = new Javadoc();

        Project project = new Project();
        task.setProject(project);
        task.setClasspath(antHelper.createClassPath(project, component));
        task.setEncoding("UTF-8");
        task.setUse(true);
        task.setAuthor(true);
        task.setDestdir(javaDocFolder);

        setLinks(task, component);

        final HashSet<String> excludes = new HashSet<String>();
        setFileSets(project, task, antHelper.createSourceFileSets(component, excludes, excludes));

        setProxyConfigurationParams(task);

        task.setSource(getSourceVersion());
        task.setHeader(String.format("Compartment %s<br/>Development Component %s:%s", component.getCompartment()
            .getName(), component.getVendor(), component.getName()));

        task.execute();
    }

    private String getSourceVersion() {
        JdkHomeAlias alias = this.developmentConfiguration.getJdkHomeAlias();
        String sourceVersion;
        if (alias != null) {
            sourceVersion = alias.getSourceVersion();
        }
        else {
            String[] versionParts = System.getProperty("java.version").replace('_', '.').split("\\.");
            sourceVersion = String.format("%s.%s", versionParts[0], versionParts[1]);
        }

        return sourceVersion;
    }

    private void setLinks(final Javadoc task, final DevelopmentComponent component) {
        for (URL linkURL : this.links) {
            LinkArgument link = task.createLink();
            link.setPackagelistURL(linkURL);
            link.setOffline(false);
        }

        for (PublicPartReference referencedDC : component.getUsedDevelopmentComponents()) {
            DevelopmentComponent usedDC = this.dcFactory.get(referencedDC.getVendor(), referencedDC.getName());

            if (usedDC != null && usedDC.getCompartment().isSourceState()) {
                LinkArgument link = task.createLink();
                link.setPackagelistLoc(new File(getOfflineJavaDocFolder(usedDC)));
                link.setOffline(true);
            }
        }
    }

    private void setFileSets(Project project, Javadoc task, Collection<FileSet> sources) {
        for (final FileSet source : sources) {
            source.setProject(project);
            task.addFileset(source);
        }
    }

    private void setProxyConfigurationParams(final Javadoc task) {
        if (proxy != null) {
            if (proxy.getPort() > 0) {
                task.setAdditionalparam(String.format("-J-Dhttp.proxyHost=%s -J-Dhttp.proxyPort=%d",
                    proxy.getHostName(), proxy.getPort()));
            }
            else {
                task.setAdditionalparam(String.format("-J-Dhttp.proxyHost=%s", proxy.getHostName()));
            }
        }
    }

    private String getOfflineJavaDocFolder(DevelopmentComponent component) {
        return String.format("%s/javadoc/%s~%s/package-list", this.antHelper.getPathToWorkspace(),
            component.getVendor(), component.getName().replace('/', '~')).replace('/', File.separatorChar);
    }

    private String getJavaDocFolder(DevelopmentComponent component) {
        return String.format("%s/javadoc/%s~%s", this.antHelper.getPathToWorkspace(), component.getVendor(),
            component.getName().replace('/', '~')).replace('/', File.separatorChar);
    }

    public static final void main(String args[]) {
        DevelopmentConfiguration developmentConfiguration = new DevelopmentConfiguration("PN3_Libs70_D");
        Compartment compartment =
            new Compartment("LIBJEE", CompartmentState.Source, "gisa.de", "Libraries f�r NW 7.0", "LIBJEE");
        developmentConfiguration.add(compartment);
        DevelopmentComponentFactory dcFactory = new DevelopmentComponentFactory();
        DevelopmentComponent component = dcFactory.create("gisa.de", "lib/security/api/test/helper");
        component.addSourceFolder("src/packages");
        // component.addSourceFolder("test/packages");

        compartment.add(component);
        AntHelper antHelper =
            new AntHelper("C:/tmp/hudson/jobs/libjee/workspace", dcFactory, new ExcludesFactory(), new PrintStream(
                System.err));

        JavaDocExecutor executor =
            new JavaDocExecutor(developmentConfiguration, antHelper, dcFactory, new HashSet<URL>(), null);
        executor.execute(component);
    }
}