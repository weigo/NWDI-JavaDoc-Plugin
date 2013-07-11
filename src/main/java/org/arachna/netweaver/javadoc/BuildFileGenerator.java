/**
 *
 */
package org.arachna.netweaver.javadoc;

import hudson.ProxyConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.DevelopmentComponentType;
import org.arachna.netweaver.dc.types.PublicPartReference;

/**
 * An ant build file generator for the JavaDoc task.
 * 
 * @author Dirk Weigenand
 */
final class BuildFileGenerator {
    /**
     * Helper class for setting up an ant task with class path, source file sets etc.
     */
    private final AntHelper antHelper;

    /**
     * The HTTP-Proxy to use iff available.
     */
    private final ProxyConfiguration proxy;

    /**
     * links to add to existing javadoc documentation.
     */
    private final Collection<String> links;

    /**
     * Registry for development components.
     */
    private final DevelopmentComponentFactory dcFactory;

    /**
     * The template engine to use for build file generation.
     */
    private final VelocityEngine engine;

    /**
     * Paths to generated build files.
     */
    private final Collection<String> buildFilePaths = new HashSet<String>();

    /**
     * Indicate that UmlGraph should be used for generating UML images of class- and inheritance relations.
     */
    private final boolean useUmlGraph;

    /**
     * Create an executor for executing the Javadoc ant task using the given ant helper object and links to related javadoc documentation.
     * 
     * @param antHelper
     *            helper for populating an ant task with source filesets and class path for a given development component
     * @param links
     *            links to add to existing javadoc documentation
     * @param engine
     *            template engine to use for generating build files.
     */
    BuildFileGenerator(final AntHelper antHelper, final DevelopmentComponentFactory dcFactory, final Collection<String> links,
        final VelocityEngine engine, final boolean useUmlGraph) {
        this(antHelper, dcFactory, links, /*
                                           * Hudson. getInstance ().proxy
                                           */null, engine, useUmlGraph);
    }

    /**
     * Create an executor for executing the Javadoc ant task using the given ant helper object and links to related javadoc documentation.
     * 
     * @param antHelper
     *            helper for populating an ant task with source filesets and class path for a given development component
     * @param dcFactory
     *            registry for development components.
     * @param links
     *            links to add to existing javadoc documentation
     * @param proxy
     *            the wwwproxy to use for referencing external javadocs.
     * @param engine
     *            template engine to use for generating build files.
     * @param useUmlGraph
     *            indicate whether to run UmlGraph and include generated images (<code>true</code>: yes, run UmlGraph. <code>false</code>
     *            don't care about it).
     * 
     */
    BuildFileGenerator(final AntHelper antHelper, final DevelopmentComponentFactory dcFactory, final Collection<String> links,
        final ProxyConfiguration proxy, final VelocityEngine engine, final boolean useUmlGraph) {
        this.antHelper = antHelper;
        this.dcFactory = dcFactory;
        this.links = links;
        this.proxy = proxy;
        this.engine = engine;
        this.useUmlGraph = useUmlGraph;
    }

    /**
     * @return the buildFilePaths
     */
    final Collection<String> getBuildFilePaths() {
        return buildFilePaths;
    }

    /**
     * Run the Ant Javadoc task for the given development component.
     * 
     * @param component
     *            development component to document with JavaDoc.
     */
    public void execute(final DevelopmentComponent component) {
        final Set<String> sources = new HashSet<String>();

        for (final String folder : antHelper.createSourceFileSets(component)) {
            sources.add(folder);
        }

        if (!sources.isEmpty()) {
            final Context context = createContext(component, sources);

            Writer buildFile = null;

            try {
                final String location = createBuildXmlLocation(component);
                buildFile = new FileWriter(location);
                evaluateContext(context, buildFile);
                buildFilePaths.add(location);
            }
            catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            finally {
                if (buildFile != null) {
                    try {
                        buildFile.close();
                    }
                    catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * @param component
     * @return
     */
    private String createBuildXmlLocation(final DevelopmentComponent component) {
        final String baseLocation = antHelper.getBaseLocation(component);
        final String location = String.format("%s/javadoc-build.xml", baseLocation);
        return location;
    }

    /**
     * @param context
     * @param buildFile
     * @throws IOException
     */
    private void evaluateContext(final Context context, final Writer buildFile) throws IOException {
        engine.evaluate(context, buildFile, "javadoc-build", getTemplateReader());
    }

    /**
     * @param component
     * @param sources
     * @return
     */
    private Context createContext(final DevelopmentComponent component, final Set<String> sources) {
        final Context context = new VelocityContext();
        context.put("sourcePaths", sources);
        context.put("classes", component.getOutputFolder());
        context.put("classpaths", antHelper.createClassPath(component));
        context.put("javaDocDir", getJavaDocFolder(component));
        context.put("source", component.getCompartment().getDevelopmentConfiguration().getSourceVersion());
        context.put("header", getHeader(component));
        context.put("links", getLinks(component));
        context.put("proxy", getProxyConfigurationParams());
        context.put("useUmlGraph", useUmlGraph(component));
        context.put("vendor", component.getVendor());
        context.put("component", component.getNormalizedName("~"));

        return context;
    }

    /**
     * Indicate that UML diagrams should be generated when generating JavaDoc documentation.
     * 
     * Diagrams are generated when the build mandates so (via build configuration option useUmlGraph) and the given development component
     * contains Java sources (determined via the component type).
     * 
     * @return <code>true</code> when UML diagrams should be generated for the given development component, <code>false</code> otherwise.
     */
    private Boolean useUmlGraph(final DevelopmentComponent component) {
        return Boolean.valueOf(useUmlGraph) && !DevelopmentComponentType.WebDynpro.equals(component.getType())
            && component.getType().canContainJavaSources();
    }

    /**
     * @return
     */
    private Reader getTemplateReader() {
        return new InputStreamReader(this.getClass().getResourceAsStream("/org/arachna/netweaver/javadoc/javadoc-build.vm"));
    }

    /**
     * @param component
     * @return
     */
    private String getHeader(final DevelopmentComponent component) {
        return String.format("&lt;div class='compartment'&gt;Compartment %s&lt;br/&gt;Development Component %s:%s&lt;/div&gt;", component
            .getCompartment().getName(), component.getVendor(), component.getName());
    }

    /**
     * Set links to external javadocs. Dependencies will be determined from the given DC and the links configured in the respective project.
     * 
     * @param component
     *            DC to use to determine which other projects (DCs) should be referenced.
     */
    private Collection<String> getLinks(final DevelopmentComponent component) {
        final Collection<String> links = new HashSet<String>();
        links.addAll(this.links);

        for (final PublicPartReference referencedDC : component.getUsedDevelopmentComponents()) {
            final DevelopmentComponent usedDC = dcFactory.get(referencedDC.getVendor(), referencedDC.getName());

            if (usedDC != null && usedDC.getCompartment().isSourceState()) {
                links.add(getJavaDocFolder(usedDC));
            }
        }

        return links;
    }

    /**
     * Create proxy configuration parameters for the javadoc tool.
     * 
     * @return a string suited for javadocs additional params to configure http proxy access
     */
    private String getProxyConfigurationParams() {
        final StringBuilder proxyConfig = new StringBuilder();

        if (proxy != null) {
            final InetSocketAddress address = (InetSocketAddress)proxy.createProxy().address();
            if (address.getPort() > 0) {
                proxyConfig.append(String.format("-J-Dhttp.proxyHost=%s -J-Dhttp.proxyPort=%d", address.getHostName(), address.getPort()));
            }
            else {
                proxyConfig.append(String.format("-J-Dhttp.proxyHost=%s", address.getHostName()));
            }
        }

        return proxyConfig.toString();
    }

    /**
     * Calculate the folder to output javadoc to for the given development component.
     * 
     * @param component
     *            development component to calculate the javadoc output folder for
     * @return folder to output javadoc to
     */
    private String getJavaDocFolder(final DevelopmentComponent component) {
        return String.format("%s/javadoc/%s~%s", antHelper.getPathToWorkspace(), component.getVendor(), component.getNormalizedName("~"))
            .replace('/', File.separatorChar);
    }
}
