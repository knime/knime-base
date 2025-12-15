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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.viz.property.color;

import static org.mockito.Mockito.mock;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.node.DefaultModel.ConfigureInput;
import org.knime.node.DefaultModel.ConfigureOutput;
import org.knime.node.DefaultModel.ExecuteInput;
import org.knime.node.DefaultModel.ExecuteOutput;
import org.knime.node.parameters.NodeParameters;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Shared test helper classes for Color Designer node tests, providing reusable test implementations
 * of ConfigureInput, ConfigureOutput, ExecuteInput, and ExecuteOutput.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
final class ColorDesignerTestHelper {

    private ColorDesignerTestHelper() {
        // Utility class
    }

    /**
     * Test implementation of ConfigureInput that only implements the methods used by the factories.
     */
    static final class TestConfigureInput implements ConfigureInput {
        private final NodeParameters m_parameters;

        private final DataTableSpec[] m_inTableSpecs;

        TestConfigureInput(final NodeParameters parameters, final DataTableSpec[] inTableSpecs) {
            m_parameters = parameters;
            m_inTableSpecs = inTableSpecs;
        }

        @Override
        public <S extends NodeParameters> S getParameters() {
            @SuppressWarnings("unchecked")
            final S result = (S)m_parameters;
            return result;
        }

        @Override
        public DataTableSpec[] getInTableSpecs() {
            return m_inTableSpecs;
        }

        // Unused methods can return null
        @Override
        public PortObjectSpec[] getInPortSpecs() {
            return null;
        }

        @Override
        public <S extends PortObjectSpec> S getInPortSpec(final int index) {
            return null;
        }

        @Override
        public DataTableSpec getInTableSpec(final int portIndex) {
            return null;
        }

        @Override
        public PortType[] getOutPortTypes() {
            return null;
        }

        @Override
        public PortsConfiguration getPortsConfiguration() {
            return null;
        }
    }

    /**
     * Test implementation of ConfigureOutput that only implements the methods used by the factories.
     */
    static final class TestConfigureOutput implements ConfigureOutput {
        PortObjectSpec[] m_outSpecs;

        @Override
        public <S extends PortObjectSpec> void setOutSpecs(final S... specs) {
            m_outSpecs = specs;
        }

        // Unused methods can be empty
        @Override
        public <S extends PortObjectSpec> void setOutSpec(final int index, final S spec) {
        }

        @Override
        public void setWarningMessage(final String message) {
        }
    }

    /**
     * Test implementation of ExecuteInput that only implements the methods used by the factories.
     */
    static final class TestExecuteInput implements ExecuteInput {
        private final NodeParameters m_parameters;

        private final BufferedDataTable[] m_inTables;

        private final ExecutionContext m_executionContext;

        TestExecuteInput(final NodeParameters parameters, final BufferedDataTable[] inTables) {
            m_parameters = parameters;
            m_inTables = inTables;
            m_executionContext = mock(ExecutionContext.class);
            Mockito.when(m_executionContext.createSpecReplacerTable(ArgumentMatchers.any(BufferedDataTable.class),
                ArgumentMatchers.any(DataTableSpec.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Override
        public <S extends NodeParameters> S getParameters() {
            @SuppressWarnings("unchecked")
            final S result = (S)m_parameters;
            return result;
        }

        @Override
        public BufferedDataTable[] getInTables() {
            return m_inTables;
        }

        @Override
        public ExecutionContext getExecutionContext() {
            return m_executionContext;
        }

        // Unused methods can return null
        @Override
        public PortObject[] getInPortObjects() {
            return null;
        }

        @Override
        public <D extends PortObject> D getInPortObject(final int portIndex) {
            return null;
        }

        @Override
        public BufferedDataTable getInTable(final int portIndex) {
            return null;
        }

        @Override
        public PortType[] getOutPortTypes() {
            return null;
        }

        @Override
        public PortsConfiguration getPortsConfiguration() {
            return null;
        }
    }

    /**
     * Test implementation of ExecuteOutput that only implements the methods used by the factories.
     */
    static final class TestExecuteOutput implements ExecuteOutput {
        PortObject[] m_outData;

        @Override
        public <D extends PortObject> void setOutData(final D... data) {
            m_outData = data;
        }

        // Unused methods can be empty
        @Override
        public <D extends PortObject> void setOutData(final int index, final D data) {
        }

        @Override
        public <D extends PortObject> void setInternalData(final D... data) {
        }

        @Override
        public void setWarningMessage(final String message) {
        }
    }
}
