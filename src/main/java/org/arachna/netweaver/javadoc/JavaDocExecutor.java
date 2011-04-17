/**
 *
 */
package org.arachna.netweaver.javadoc;

import hudson.model.Hudson;

import java.net.InetSocketAddress;
import java.util.HashSet;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javadoc;
import org.apache.tools.ant.types.FileSet;
import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;

/**
 * Wrapper for the Ant JavaDoc task. Sets up the task and executes it.
 * 
 * @author Dirk Weigenand
 */
public final class JavaDocExecutor {
    /**
     * Helper class for setting up an ant task with class path, source file sets
     * etc.
     */
    private final AntHelper antHelper;

    private final InetSocketAddress proxy;

    /**
     * 
     * @param antHelper
     */
    JavaDocExecutor(final AntHelper antHelper) {
        this.antHelper = antHelper;
        proxy = (InetSocketAddress)Hudson.getInstance().proxy.createProxy().address();
    }

    public void execute(final DevelopmentComponent component) {
        final Javadoc task = new Javadoc();
        final HashSet<String> excludes = new HashSet<String>();

        for (final FileSet sources : antHelper.createSourceFileSets(component, excludes, excludes)) {
            task.addFileset(sources);
        }

        final Project project = new Project();
        task.setClasspath(antHelper.createClassPath(project, component));
        task.setEncoding("UTF-8");
        task.setUse(true);
        task.setAuthor(true);

        setProxyConfigurationParams(task);
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
}
