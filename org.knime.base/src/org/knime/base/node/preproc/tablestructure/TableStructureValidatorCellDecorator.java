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
 *   12.02.2026 (mgohm): created
 */
package org.knime.base.node.preproc.tablestructure;

import static org.knime.core.node.util.CheckUtils.checkArgument;
import static org.knime.core.node.util.CheckUtils.checkNotNull;

import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.DataTypeHandling;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.NodeLogger;

/**
 * Uses the decorator pattern for data cell validation. I.e. missing value or domain checks can be dynamically added and
 * the functionality is nice encapsulated. See the factory methods on this class to receive instances.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
abstract class TableStructureValidatorCellDecorator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TableStructureValidatorCellDecorator.class);

    private TableStructureValidatorCellDecorator m_innerDecorator;

    /**
     * Use the factory methods to get an instance.
     *
     * @param innerDecorator {@link TableStructureValidatorCellDecorator}
     */
    private TableStructureValidatorCellDecorator(final TableStructureValidatorCellDecorator innerDecorator) {
        m_innerDecorator = innerDecorator;
    }

    /**
     * First invokes the {@link #handleCell(Long, RowKey, DataCell)} method on the inner decorator and afterwards
     * performs {@link #doHandleCell(Long, DataCell)} on the result.
     *
     * @param rowIndex the {@link Long} of the row this cell is contained in
     * @param rowKey the {@link RowKey} of the row this cell is contained in
     * @param cell the {@link DataCell}
     * @return the (decorated) {@link DataCell}
     */
    final DataCell handleCell(final Long rowIndex, final RowKey rowKey, final DataCell cell) {
        var decoratedCell = cell;
        if (m_innerDecorator != null) {
            decoratedCell = m_innerDecorator.handleCell(rowIndex, rowKey, cell);
        }

        return doHandleCell(rowIndex, rowKey, decoratedCell);
    }

    /**
     * Performs the actual validation, which may add conflicts or return a converted {@link DataCell}.
     *
     * @param rowIndex the {@link Long} of the row this {@link DataCell} is contained in
     * @param rowKey the {@link RowKey} of the row this {@link DataCell} is contained in
     * @param decoratedCell the {@link DataCell} which might be changed in inner decorators
     * @return the decorated {@link DataCell}
     */
    abstract DataCell doHandleCell(final Long rowIndex, final RowKey rowKey, final DataCell decoratedCell);

    /**
     * Retrieves the associated {@link DataColumnSpec} of this decorator.
     *
     * @return {@link DataColumnSpec}
     */
    protected DataColumnSpec getDataColumnSpec() {
        checkArgument(m_innerDecorator != null, "Should not be null");
        return m_innerDecorator.getDataColumnSpec();
    }

    /**
     * Retrieves the the input column name.
     *
     * @return input column name
     */
    protected String getInputColumnName() {
        checkArgument(m_innerDecorator != null, "Should not be null");
        return m_innerDecorator.getInputColumnName();
    }

    /**
     * The basic decorator which returns the name and the input spec.
     *
     * @param inputColumnName the input column name
     * @param inputSpec the input column spec
     * @return the decorator
     */
    static TableStructureValidatorCellDecorator forColumn(
        final String inputColumnName, final DataColumnSpec inputSpec) {
        return new TableStructureValidatorCellDecorator(null) {

            @Override
            protected DataCell doHandleCell(final Long rowIndex, final RowKey rowKey, final DataCell decoratedCell) {
                return decoratedCell;
            }

            @Override
            protected DataColumnSpec getDataColumnSpec() {
                return inputSpec;
            }

            @Override
            protected String getInputColumnName() {
                return inputColumnName;
            }
        };
    }

    /**
     * Tries to convert the given cell represented by {@link TableStructureValidatorCellDecorator} to the given
     * {@link CellFactory}. Missing values are directly returned.
     *
     * @param inner the inner {@link TableStructureValidatorCellDecorator}
     * @param handling the {@link DataTypeHandling} to determine whether to try to convert the cell or not
     * @param cellFactory the {@link CellFactory} to convert the cell to
     * @param referenceSpec the reference {@link DataColumnSpec} for the conversion
     * @param conflicts the {@link TableStructureValidatorColConflicts} to add conflicts to
     * @return {@link TableStructureValidatorCellDecorator}
     */
    @SuppressWarnings("java:S1188") // number of lines in anonymous class
    static TableStructureValidatorCellDecorator conversionCellDecorator(
        final TableStructureValidatorCellDecorator inner, final DataTypeHandling handling,
        final DataCellFactory dataCellFactory, final DataColumnSpec referenceSpec,
        final TableStructureValidatorColConflicts conflicts) {
        checkNotNull(handling);
        checkNotNull(dataCellFactory);
        checkNotNull(referenceSpec);
        return new TableStructureValidatorCellDecorator(inner) {

            @Override
            protected DataCell doHandleCell(final Long rowIndex, final RowKey rowkey, final DataCell decoratedCell) {
                try {
                    if (decoratedCell.isMissing()) {
                        return decoratedCell;
                    }
                    if (dataCellFactory instanceof FromString fromStringFactory) {
                        return fromStringFactory.createCell(decoratedCell.toString());
                    }
                    throw new IllegalArgumentException(
                        "Unsupported target data type: " + dataCellFactory.getDataType());
                } catch (RuntimeException e) {
                    LOGGER.debug(e);
                    conflicts.addConflict(TableStructureValidatorColConflicts.conversionFailed(getInputColumnName(),
                        rowIndex, rowkey, dataCellFactory.getDataType()));
                    return DataType.getMissingCell();
                }
            }

            @Override
            protected DataColumnSpec getDataColumnSpec() {
                return referenceSpec;
            }

        };
    }

}
