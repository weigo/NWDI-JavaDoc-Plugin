/**
 * 
 */
package org.arachna.netweaver.javadoc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.arachna.netweaver.hudson.nwdi.NWDIProject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;

/**
 * Jenkins-Test for configuring a project with a {@link JavaDocBuilder}.
 * 
 * @author Dirk Weigenand
 */
public final class JavaDocBuilderConfigTest extends HudsonTestCase {
    /**
     * the project instance to use for the test.
     */
    private NWDIProject project;

    /**
     * the configuration form.
     */
    private HtmlForm config;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.project = this.hudson.createProject(NWDIProject.class, "nwdi");
        this.project.getBuildersList().add(new JavaDocBuilder(new HashSet<String>(), false));
        this.config = createWebClient().getPage(project, "configure").getFormByName("config");
    }

    public void testConfiguration() throws ElementNotFoundException, SAXException, Exception {
        submit(config);
        JavaDocBuilder builder = project.getBuildersList().get(JavaDocBuilder.class);

        assertFalse("useUmlGraph property should be false!", builder.getUseUmlGraph());
        assertTrue("Links should be empty!", builder.getLinks().isEmpty());
    }

    public void testConfigurationWithUseUmlGraphSet() throws ElementNotFoundException, SAXException, Exception {
        String attributeValue = "_.useUmlGraph";
        List<HtmlElement> elements = this.getElementsByTagAndAttributeName("input", "name", attributeValue);

        assertEquals(String.format("More than 1 attribute of type %s found! ", attributeValue), elements.size(), 1);

        elements.get(0).setAttribute("checked", "true");

        submit(config);
        JavaDocBuilder builder = project.getBuildersList().get(JavaDocBuilder.class);

        assertTrue("useUmlGraph property should be true!", builder.getUseUmlGraph());
        assertTrue("Links should be empty!", builder.getLinks().isEmpty());
    }

    public void testConfigurationWithOneLink() throws ElementNotFoundException, SAXException, Exception {
        String attributeValue = "link";
        List<HtmlElement> elements = this.getElementsByTagAndAttributeName("input", "name", attributeValue);
        assertEquals(String.format("More than 1 attribute of type %s found! ", attributeValue), elements.size(), 1);

        String url = "http://static.springsource.org/spring/docs/2.5.x/api/";
        elements.get(0).setAttribute("value", url);

        submit(config);
        JavaDocBuilder builder = project.getBuildersList().get(JavaDocBuilder.class);

        assertFalse("Links should not be empty!", builder.getLinks().isEmpty());
        assertTrue(String.format("Links should contain %s!", url), builder.getLinks().contains(url));
    }

    private List<HtmlElement> getElementsByTagAndAttributeName(String tagName, String attributeName,
        String attributeValue) {
        List<HtmlElement> elements = new ArrayList<HtmlElement>();

        for (HtmlElement element : this.config.getElementsByTagName(tagName)) {
            if (attributeValue.equals(element.getAttribute(attributeName))) {
                elements.add(element);
            }
        }

        return elements;
    }
}
