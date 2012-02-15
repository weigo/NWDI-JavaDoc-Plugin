/**
 * 
 */
package org.arachna.netweaver.javadoc;

import static org.junit.Assert.fail;

import java.util.HashSet;

import org.apache.velocity.app.VelocityEngine;
import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.DevelopmentConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unittests for {@link BuildFileGenerator}.
 * 
 * @author Dirk Weigenand
 */
public class BuildFileGeneratorTest {
    /**
     * Instance under test.
     */
    private BuildFileGenerator generator;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        final DevelopmentConfiguration developmentConfiguration = new DevelopmentConfiguration("DI1");
        final DevelopmentComponentFactory dcFactory = new DevelopmentComponentFactory();
        final AntHelper antHelper = new AntHelper("", dcFactory);
        generator = new BuildFileGenerator(antHelper, dcFactory, new HashSet<String>(), new VelocityEngine(), false);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void test() {
//        generator.execute(component);
        // fail("Not implemented yet!");
    }
}
