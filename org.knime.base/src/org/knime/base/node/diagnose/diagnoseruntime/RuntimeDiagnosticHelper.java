/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   23 Jun 2022 (leon.wenzler): created
 */
package org.knime.base.node.diagnose.diagnoseruntime;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps the HotSpotDiagnosticMXBean interface into accessible methods using reflection. For the interface, see
 * {@link "https://docs.oracle.com/javase/7/docs/jre/api/management/extension/com/sun/management/HotSpotDiagnosticMXBean.html"}
 *
 * @author Leon Wenzler, KNIME AG, Schloss
 */
public final class RuntimeDiagnosticHelper {

    private static final Logger LOGGER = Logger.getLogger(RuntimeDiagnosticHelper.class.getName());

    private static final String MX_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    // stores the hotspot diagnostic MXBean object
    @SuppressWarnings("java:S3077")
    private static volatile Object hotspotMXBeanInstance;

    /**
     * Initializes the hotspot MXBean field
     */
    private static synchronized void initializeHotspotMXBean() {
        if (hotspotMXBeanInstance == null) {
            hotspotMXBeanInstance = getHotspotMBean();
        }
    }

    /**
     * Retrieves the HotSpotDiagnosticMXBean from the platform server.
     *
     * @return hotspot MXBean object
     * @throws IllegalAccessException
     */
    private static Object getHotspotMBean() {
        try {
            var clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            var server = ManagementFactory.getPlatformMBeanServer();
            return ManagementFactory.newPlatformMXBeanProxy(server, MX_BEAN_NAME, clazz);
        } catch (ClassNotFoundException | IOException e) {
            LOGGER.log(Level.WARNING,
                String.format("HotSpotDiagnosticMXBean object could not be created: %s", e.getMessage()));
            return null;
        }
    }

    /**
     * Dumps the entire heap to a file.
     *
     * @param fileName location of the file
     * @param dump only live objects, i.e. objects that are reachable from others
     * @throws IllegalAccessException
     */
    static void dumpHeap(final String fileName, final boolean live) {
        initializeHotspotMXBean();
        try {
            // accessing the access-restricted HotSpotDiagnosticMXBean interface
            var clazz = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            var method = clazz.getMethod("dumpHeap", String.class, boolean.class);
            method.invoke(hotspotMXBeanInstance, fileName, live);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InvocationTargetException e) {
            LOGGER.log(Level.WARNING, String.format("Heap could not be dumped to file: %s", e.getMessage()));
        }
    }

    /**
     * Retrieves all VM options as a list of Strings.
     *
     * @return list of VMOptions
     */
    static List<String> getVMOptions() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments();
    }

    /**
     * Hides the constructor.
     */
    private RuntimeDiagnosticHelper() {
    }
}
