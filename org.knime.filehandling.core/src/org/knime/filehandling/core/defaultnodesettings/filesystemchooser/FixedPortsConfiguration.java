/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Created on Nov 3, 2020 by Sascha Wolke, KNIME GmbH
 */
package org.knime.filehandling.core.defaultnodesettings.filesystemchooser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortType;

/**
 * Simple {@link PortsConfiguration} implementation using fixed input and output ports.
 *
 * @author Sascha Wolke, KNIME GmbH
 */
public final class FixedPortsConfiguration implements PortsConfiguration {

    private final List<String> m_groupNames;

    private final PortType[] m_inputPortTypes;

    private final Map<String, int[]> m_inputGroupIndices;

    private final PortType[] m_outputPortTypes;

    private final Map<String, int[]> m_outputGroupIndices;

    /**
     * Builder to create a {@link FixedPortsConfiguration} instance.
     *
     * @author Sascha Wolke, KNIME GmbH
     */
    public static final class FixedPortsConfigurationBuilder {

        private final LinkedList<PortType> m_inputPortTypes = new LinkedList<>();

        private final HashMap<String, int[]> m_inputGroupIndices = new HashMap<>();

        private final LinkedList<PortType> m_outputPortTypes = new LinkedList<>();

        private final HashMap<String, int[]> m_outputGroupIndices = new HashMap<>();

        /**
         * Add a new group of input ports.
         *
         * @param groupName name of the port group
         * @param newPortTypes input port types
         * @return builder instance
         */
        public FixedPortsConfigurationBuilder addFixedInputPortGroup(final String groupName,
            final PortType... newPortTypes) {
            addPort(m_inputPortTypes, m_inputGroupIndices, groupName, newPortTypes);
            return this;
        }

        /**
         * Add a new group of output ports.
         *
         * @param groupName name of the port group
         * @param newPortTypes output port types
         * @return builder instance
         */
        public FixedPortsConfigurationBuilder addFixedOutputPortGroup(final String groupName,
            final PortType... newPortTypes) {
            addPort(m_outputPortTypes, m_outputGroupIndices, groupName, newPortTypes);
            return this;
        }

        private static void addPort(final LinkedList<PortType> portTypes, final HashMap<String, int[]> groupIndices,
            final String groupName, final PortType[] newPortTypes) {

            final int[] indices = new int[newPortTypes.length];
            final int portIndexOffset = portTypes.size();

            for (int i = 0; i < newPortTypes.length; i++) {
                indices[i] = portIndexOffset + i;
                portTypes.add(newPortTypes[i]);
            }

            groupIndices.put(groupName, indices);
        }

        private List<String> exportGroupNames() {
            final HashSet<String> result = new HashSet<>();
            result.addAll(m_inputGroupIndices.keySet());
            result.addAll(m_outputGroupIndices.keySet());
            return new ArrayList<>(result);
        }

        /**
         * @return the fixed ports configuration
         */
        public FixedPortsConfiguration build() {
            return new FixedPortsConfiguration( //
                exportGroupNames(), //
                m_inputPortTypes.toArray(new PortType[0]), //
                m_inputGroupIndices, //
                m_outputPortTypes.toArray(new PortType[0]), //
                m_outputGroupIndices); //
        }
    }

    /**
     * Private constructor, use {@link FixedPortsConfigurationBuilder} instead.
     */
    private FixedPortsConfiguration(final List<String> groupNames, final PortType[] inputPortTypes,
        final Map<String, int[]> inputGroupIndices, final PortType[] outputPortTypes,
        final Map<String, int[]> outputGroupIndices) {

        m_groupNames = groupNames;
        m_inputPortTypes = inputPortTypes;
        m_inputGroupIndices = inputGroupIndices;
        m_outputPortTypes = outputPortTypes;
        m_outputGroupIndices = outputGroupIndices;
    }

    @Override
    public PortType[] getInputPorts() {
        return m_inputPortTypes;
    }

    @Override
    public PortType[] getOutputPorts() {
        return m_outputPortTypes;
    }

    @Override
    public Map<String, int[]> getInputPortLocation() {
        return m_inputGroupIndices;
    }

    @Override
    public Map<String, int[]> getOutputPortLocation() {
        return m_outputGroupIndices;
    }

    @Override
    public List<String> getPortGroupNames() {
        return m_groupNames;
    }
}
