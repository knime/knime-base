/*
 * ------------------------------------------------------------------------
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
 *   May 1, 2008 (wiswedel): created
 */
package org.knime.base.node.flowvariable.variabletotablerow4;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToDataColumnConverter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.util.CheckUtils;

/**
 * NodeModel for the "Variable To TableRow" node which converts variables into a single row values with the variable
 * names as column headers.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class VariableToTable4NodeModel extends AbstractVariableToTableNodeModel {

    private final SettingsModelString m_rowID = createSettingsModelRowID();

    static SettingsModelString createSettingsModelRowID() {
        return new SettingsModelString("row_id", "values");
    }

    /**
     * Constructor.
     */
    VariableToTable4NodeModel() {
        super(FlowVariablePortObject.TYPE_OPTIONAL);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        final BufferedDataContainer cont = exec.createDataContainer(createOutSpec(false));
        try (final VariableToDataColumnConverter conv = new VariableToDataColumnConverter()) {
            cont.addRowToTable(createTableRow(exec, conv, m_rowID.getStringValue()));
            cont.close();
            return new BufferedDataTable[]{cont.getTable()};
        }
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        CheckUtils.checkSetting(m_rowID.getStringValue() != null && !m_rowID.getStringValue().trim().isEmpty(),
            "Please specify a row name.");
        return super.configure(inSpecs);
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowID.loadSettingsFrom(settings);
        super.loadValidatedSettingsFrom(settings);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rowID.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowID.validateSettings(settings);
        super.validateSettings(settings);
    }

}
