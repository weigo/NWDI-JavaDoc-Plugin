/**
 * 
 */
package org.arachna.netweaver.javadoc;

import hudson.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
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

    OverviewGenerator(File workspace, DevelopmentConfiguration developmentConfiguration) {
        this.workspace = workspace;
        this.developmentConfiguration = developmentConfiguration;

    }

    void execute() {
        File javaDocFolder = new File(workspace, "javadoc");

        if (!javaDocFolder.exists()) {
            if (!javaDocFolder.mkdirs()) {
                throw new RuntimeException("Could not mkdir " + javaDocFolder.getAbsolutePath());
            }
        }

        createIndexHtml(new File(javaDocFolder, "index.html"));
        createStyleCss(new File(javaDocFolder, "style.css"));
    }

    /**
     * @param javaDocFolder
     * @throws IOException
     */
    private void createStyleCss(File styleCss) {
        Writer writer = null;

        try {
            writer = new FileWriter(styleCss);
            Util.copyStream(new InputStreamReader(this.getClass().getResourceAsStream("style.css")), writer);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
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
    protected void createIndexHtml(File output) {
        Writer result = null;

        try {
            result = new FileWriter(output);
            Transformer transformer =
                TransformerFactory.newInstance().newTransformer(
                    new StreamSource(this.getClass().getResourceAsStream("JavaDocIndex.xsl")));
            transformer.setParameter("track", this.developmentConfiguration.getCaption());
            transformer.transform(createIndexDom(), new StreamResult(result));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        catch (TransformerFactoryConfigurationError e) {
            throw new RuntimeException(e);
        }
        catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (result != null) {
                try {
                    result.close();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param javaDocFolder
     * @return
     */
    public DOMSource createIndexDom() throws ParserConfigurationException, IOException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.newDocument();
        Element compartments = document.createElement("compartments");
        document.appendChild(compartments);

        for (Compartment compartment : getNonEmptyCompartmentsSortedAlphabetically(this.developmentConfiguration)) {
            Collection<DevelopmentComponent> components = this.getDevelopmentComponentsWithJavaSources(compartment);

            Element compartmentElement = document.createElement("compartment");
            compartmentElement.setAttribute("name", compartment.getSoftwareComponent());
            compartments.appendChild(compartmentElement);

            for (DevelopmentComponent component : components) {
                Element dc = document.createElement("dc");
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
    private List<Compartment> getNonEmptyCompartmentsSortedAlphabetically(DevelopmentConfiguration configuration) {
        List<Compartment> compartments = new ArrayList<Compartment>();

        for (Compartment compartment : configuration.getCompartments(CompartmentState.Source)) {
            if (!compartment.getDevelopmentComponents().isEmpty()) {
                compartments.add(compartment);
            }
        }

        Collections.sort(compartments, new CompartmentByNameComparator());

        return compartments;
    }

    private Collection<DevelopmentComponent> getDevelopmentComponentsWithJavaSources(Compartment compartment) {
        Collection<DevelopmentComponent> components = new ArrayList<DevelopmentComponent>();
        DCWithJavaSourceAcceptingFilter filter = new DCWithJavaSourceAcceptingFilter();

        for (DevelopmentComponent component : compartment.getDevelopmentComponents()) {
            if (filter.accept(component)) {
                components.add(component);
            }
        }

        return components;
    }
}
