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
 *   Jan 28, 2020 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row2;

import static org.knime.base.node.preproc.filter.row2.RowPredicateUtil.consumeNode;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import org.knime.base.data.filter.row.dialog.component.ColumnRole;
import org.knime.base.data.filter.row.dialog.component.DefaultGroupTypes;
import org.knime.base.data.filter.row.dialog.model.GroupType;
import org.knime.base.data.filter.row.dialog.model.Node;
import org.knime.base.node.preproc.filter.row2.operator.KnimeRowFilterOperatorRegistry;
import org.knime.base.node.preproc.filter.row2.operator.NoConditionRowPredicate;
import org.knime.base.node.preproc.filter.row2.operator.RowPredicate;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings methods of the Row Filter and Row Splitter nodes. Also it handles the configuration of
 * the input {@link DataTableSpec} and creates a {@link RowPredicate} used for filtering the input table in both nodes.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
abstract class AbstractRowFilterNodeModel extends NodeModel {

    /** key for storing settings in config object. */
    private static final String CFGFILTER = "rowFilter";

    private static final GroupType[] GROUP_TYPES = new GroupType[]{DefaultGroupTypes.AND, DefaultGroupTypes.OR};

    private final KnimeRowFilterConfig m_rowFilterConfig = getRowFilterConfig();

    private DataTableSpec m_dataTableSpec;

    private static final EnumSet<ColumnRole> ADDITIONAL_COLUMNS = EnumSet.of(ColumnRole.ROW_ID, ColumnRole.ROW_INDEX);

    /**
     * @param nrInDataPorts holds the number of input ports.
     * @param nrOutDataPorts holds the number of output ports.
     */
    protected AbstractRowFilterNodeModel(final int nrInDataPorts, final int nrOutDataPorts) {
        super(nrInDataPorts, nrOutDataPorts); // nr of input port, nr of output ports
    }

    protected void configureInputSpec(final DataTableSpec dataTableSpec) throws InvalidSettingsException {
        final DataColumnSpec[] additionalColumnsSpec =
            ADDITIONAL_COLUMNS.stream().map(c -> c.createColumnSpec(dataTableSpec)).toArray(DataColumnSpec[]::new);
        final DataTableSpecCreator specCreator = new DataTableSpecCreator();
        specCreator.setName(dataTableSpec.getName());
        specCreator.addColumns(additionalColumnsSpec);
        specCreator.addColumns(dataTableSpec);
        m_dataTableSpec = specCreator.createSpec();
        m_rowFilterConfig.validate(m_dataTableSpec, KnimeRowFilterOperatorRegistry.getInstance());
    }

    protected static KnimeRowFilterConfig getRowFilterConfig() {
        return new KnimeRowFilterConfig(CFGFILTER, GROUP_TYPES);
    }

    /**
     * Creates the {@link RowPredicate }n to filter the rows of the input table.
     *
     * @param tableSpec holds the input {@link DataTableSpec}.
     * @return the created {@link RowPredicate}
     * @throws InvalidSettingsException in case of no condition, empty group or not known elements (neither group or
     *             condition) encountered.
     */
    RowPredicate createRowPredicate(final DataTableSpec tableSpec) throws InvalidSettingsException {
        final Node root = m_rowFilterConfig.getRoot();
        RowPredicate rowPredicate;
        if (root != null) {
            rowPredicate = consumeNode(root, tableSpec);
            if (!m_rowFilterConfig.isQueryDefinesInclude()) {
                rowPredicate = RowPredicate.negate(rowPredicate);
            }
        } else {
            setWarningMessage("Filters were not specified. Returning input data.");
            rowPredicate = NoConditionRowPredicate.INSTANCE;
        }
        return rowPredicate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_rowFilterConfig.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowFilterConfig.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowFilterConfig.validateSettings(settings, m_dataTableSpec, KnimeRowFilterOperatorRegistry.getInstance());
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //no internals
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //no internals
    }

    @Override
    protected void reset() {
        m_dataTableSpec = null;
    }
}
