/**
 * 
 */
package org.arachna.netweaver.javadoc;

import hudson.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.arachna.netweaver.dc.types.Compartment;
import org.arachna.netweaver.dc.types.CompartmentByNameComparator;
import org.arachna.netweaver.dc.types.CompartmentState;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentConfiguration;
import org.arachna.netweaver.hudson.nwdi.DCWithJavaSourceAcceptingFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generator for JavaDoc overview page for all development components in a
 * development configuration.
 * 
 * @author Dirk Weigenand
 */
final class OverviewGenerator {

    /**
     * development configuration to use for generating a javadoc overview page
     * for all development components containing java sources.
     */
    private final DevelopmentConfiguration developmentConfiguration;
    private final File workspace;

    OverviewGenerator(final File workspace, final DevelopmentConfiguration developmentConfiguration) {
        this.workspace = workspace;
        this.developmentConfiguration = developmentConfiguration;

    }

    void execute() {
        final File javaDocFolder = new File(workspace, "javadoc");

        if (!javaDocFolder.exists()) {
            if (!javaDocFolder.mkdirs()) {
                throw new IllegalStateException("Could not mkdir " + javaDocFolder.getAbsolutePath());
            }
        }

        createIndexHtml(javaDocFolder);
        createStyleCss(new File(javaDocFolder, "style.css"));
    }

    /**
     * @param javaDocFolder
     * @throws IOException
     */
    private void createStyleCss(final File styleCss) {
        Writer writer = null;

        try {
            writer = new FileWriter(styleCss);
            Util.copyStream(new InputStreamReader(this.getClass().getResourceAsStream("style.css")), writer);
        }
        catch (final IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (final IOException e) {
                    Logger.getLogger(getClass()).error("", e);
                }
            }
        }
    }

    /**
     * Generate <code>index.html</code> as an entry point for all available
     * JavaDoc documentation.
     * 
     * @param configuration
     *            {@link DevelopmentConfiguration} to generate entry page for.
     */
    protected void createIndexHtml(final File baseDir) {
        Writer result = null;
        Writer indexXml = null;

        try {
            result = new OutputStreamWriter(new FileOutputStream(new File(baseDir, "index.html")), Charset.forName("UTF-8"));
            indexXml = new OutputStreamWriter(new FileOutputStream(new File(baseDir, "index.xml")), Charset.forName("UTF-8"));
            final Transformer transformer =
                TransformerFactory.newInstance().newTransformer(new StreamSource(this.getClass().getResourceAsStream("JavaDocIndex.xsl")));
            transformer.setParameter("track", this.developmentConfiguration.getCaption());
            final DOMSource indexDom = createIndexDom();
            transformer.transform(indexDom, new StreamResult(result));

            TransformerFactory.newInstance().newTransformer().transform(indexDom, new StreamResult(indexXml));
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        catch (final TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
        catch (final TransformerFactoryConfigurationError e) {
            throw new IllegalStateException(e);
        }
        catch (final TransformerException e) {
            throw new IllegalStateException(e);
        }
        catch (final ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (final IOException e) {
                    Logger.getLogger(getClass()).error("", e);
                }
            }

            if (indexXml != null) {
                try {
                    indexXml.close();
                }
                catch (final IOException e) {
                    Logger.getLogger(getClass()).error("", e);
                }
            }

        }
    }

    /**
     * @param javaDocFolder
     * @return
     */
    public DOMSource createIndexDom() throws ParserConfigurationException, IOException {
        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document document = builder.newDocument();
        final Element compartments = document.createElement("compartments");
        document.appendChild(compartments);

        for (final Compartment compartment : getNonEmptyCompartmentsSortedAlphabetically(this.developmentConfiguration)) {
            final Collection<DevelopmentComponent> components = this.getDevelopmentComponentsWithJavaSources(compartment);

            final Element compartmentElement = document.createElement("compartment");
            compartmentElement.setAttribute("name", compartment.getSoftwareComponent());
            compartments.appendChild(compartmentElement);

            for (final DevelopmentComponent component : components) {
                final Element dc = document.createElement("dc");
                compartmentElement.appendChild(dc);

                dc.setAttribute("vendor", component.getVendor());
                dc.setAttribute("name", component.getName());
                dc.setAttribute("folder", component.getVendor() + "~" + component.getName().replace('/', '~'));

                dc.appendChild(document.createTextNode(component.getDescription()));
            }
        }

        return new DOMSource(document);
    }

    /**
     * @param configuration
     * @return
     */
    private List<Compartment> getNonEmptyCompartmentsSortedAlphabetically(final DevelopmentConfiguration configuration) {
        final List<Compartment> compartments = new ArrayList<Compartment>();

        for (final Compartment compartment : configuration.getCompartments(CompartmentState.Source)) {
            if (!compartment.getDevelopmentComponents().isEmpty()) {
                compartments.add(compartment);
            }
        }

        Collections.sort(compartments, new CompartmentByNameComparator());

        return compartments;
    }

    private Collection<DevelopmentComponent> getDevelopmentComponentsWithJavaSources(final Compartment compartment) {
        final Collection<DevelopmentComponent> components = new ArrayList<DevelopmentComponent>();
        final DCWithJavaSourceAcceptingFilter filter = new DCWithJavaSourceAcceptingFilter();

        for (final DevelopmentComponent component : compartment.getDevelopmentComponents()) {
            if (filter.accept(component)) {
                components.add(component);
            }
        }

        return components;
    }
}
