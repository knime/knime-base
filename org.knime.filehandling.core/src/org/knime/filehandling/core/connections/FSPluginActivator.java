/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package org.knime.filehandling.core.connections;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * Plugin activator
 * 
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public class FSPluginActivator extends Plugin {
    // The shared instance.
    private static FSPluginActivator plugin;

    /**
     * The constructor.
     */
    public FSPluginActivator() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

    }

    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     * 
     * @return Singleton instance of the Plugin
     */
    public static FSPluginActivator getDefault() {
        return plugin;
    }

}

