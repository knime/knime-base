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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.tablestructure;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.base.node.preproc.tablestructure.TableStructureValidatorColConflicts.TableStructureValidatorColConflict;
import org.knime.base.node.preproc.tablestructure.TableStructureValidatorConfiguration.ConfigurationContainer;
import org.knime.base.node.preproc.tablestructure.TableStructureValidatorNodeParameters.ColumnExistenceHandling;
import org.knime.base.node.preproc.tablestructure.TableStructureValidatorNodeParameters.RejectBehavior;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * {@link WebUINodeModel} for the Table Structure Validator node.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"deprecation", "restriction"})
class TableStructureValidatorNodeModel extends WebUINodeModel<TableStructureValidatorNodeParameters> {

    private TableStructureValidatorConfiguration m_validationConfig = new TableStructureValidatorConfiguration();

    /**
     * Constructor of the Table Structure Validator node model.
     */
    TableStructureValidatorNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE},
            TableStructureValidatorNodeParameters.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final TableStructureValidatorNodeParameters modelSettings) throws InvalidSettingsException {
        DataTableSpec in = inSpecs[0];

        TableStructureValidatorColConflicts conflicts = new TableStructureValidatorColConflicts();
        m_validationConfig.loadConfigurationInModel(modelSettings);
        ColumnRearranger columnRearranger = createRearranger(in, conflicts, modelSettings);

        return new DataTableSpec[]{columnRearranger.createSpec(), TableStructureValidatorColConflicts.CONFLICTS_SPEC};
    }

    @SuppressWarnings("java:S1941") // column re-arranger and return table creation can't be moved
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec,
        final TableStructureValidatorNodeParameters modelSettings) throws Exception {
        DataTableSpec in = ((BufferedDataTable)inData[0]).getDataTableSpec();
        TableStructureValidatorColConflicts conflicts = new TableStructureValidatorColConflicts();
        ColumnRearranger columnRearranger = createRearranger(in, conflicts, modelSettings);

        if (!conflicts.isEmpty() && modelSettings.m_validationFailureBehavior == RejectBehavior.FAIL_NODE) {
            throw new InvalidSettingsException("Validation failed:\n" + conflicts);
        }

        BufferedDataTable returnTable =
            exec.createColumnRearrangeTable((BufferedDataTable)inData[0], columnRearranger,
                exec.createSubExecutionContext(0.9));

        if (!conflicts.isEmpty()) {
            if (modelSettings.m_validationFailureBehavior == RejectBehavior.OUTPUT_TO_PORT_CHECK_DATA) {
                return new PortObject[]{InactiveBranchPortObject.INSTANCE,
                    createConflictsTable(conflicts, exec.createSubExecutionContext(0.1))};
            } else {
                throw new InvalidSettingsException("Validation failed:\n" + conflicts);
            }
        }

        return new PortObject[]{returnTable, InactiveBranchPortObject.INSTANCE};
    }

    /**
     * Creates a table containing the validation conflicts, according to the specification defined in
     * {@link TableStructureValidatorColConflicts#CONFLICTS_SPEC}.
     *
     * @param conflicts the list of conflicts to be included in the table
     * @param exec the execution context to create the table in
     * @return {@link BufferedDataTable} containing the conflicts
     */
    private static BufferedDataTable createConflictsTable(final TableStructureValidatorColConflicts conflicts,
        final ExecutionContext exec) {

        BufferedDataContainer createDataContainer = exec.createDataContainer(
            TableStructureValidatorColConflicts.CONFLICTS_SPEC);

        int index = 0;
        for (TableStructureValidatorColConflict conflict : conflicts) {
            createDataContainer.addRowToTable(conflict.toDataRow(RowKey.createRowKey(index)));
            index++;
        }
        createDataContainer.close();
        return createDataContainer.getTable();
    }

    /**
     * Creates the according {@link ColumnRearranger}.
     *
     * @param in the input {@link DataTableSpec}
     * @param conflicts list of {@link TableStructureValidatorColConflicts}
     * @return {@link ColumnRearranger}
     * @throws InvalidSettingsException if the settings do not allow to perform validation
     */
    private ColumnRearranger createRearranger(final DataTableSpec in,
        final TableStructureValidatorColConflicts conflicts, final TableStructureValidatorNodeParameters modelSettings)
        throws InvalidSettingsException {
        // create the reference spec from the model settings
        final var referenceTableSpec =
            new DataTableSpec(Arrays.stream(modelSettings.m_referenceStructureColumns).map(col -> {
                final DataColumnSpecCreator colSpecCreator =
                    new DataColumnSpecCreator(col.m_columnName, col.m_columnType);
                return colSpecCreator.createSpec();
            }).toArray(DataColumnSpec[]::new));

        // sort the entries according to the reference spec - and the best case insensitive matchings.
        Map<String, ConfigurationContainer> colToConfigs = m_validationConfig.applyConfiguration(in, conflicts);
        @SuppressWarnings("unchecked")
        Entry<String, ConfigurationContainer>[] array = colToConfigs.entrySet().toArray(new Entry[0]);
        Arrays.sort(array, sortAccordingToSpecComparator(referenceTableSpec));

        final var columnNamesAndDecorators =
                extractConfiguredColumnNamesAndCreateValidatorDecorators(array, referenceTableSpec, conflicts, in);
        String[] namesOfInputSpec = columnNamesAndDecorators.columnNames();
        Map<String, TableStructureValidatorCellDecorator> decorators = columnNamesAndDecorators.decorators();

        ColumnRearranger columnRearranger = new ColumnRearranger(in);

        switch (modelSettings.m_additionalColumnsHandling) {
            case REJECT:
                addUnknownColumnConflicts(namesOfInputSpec, in, conflicts);
                break;
            case REMOVE:
                columnRearranger.keepOnly(namesOfInputSpec);
                break;
            default:
                // IGNORE is done by permute.
                break;
        }
        // sort the names etc.
        columnRearranger.permute(namesOfInputSpec);

        for (Entry<String, TableStructureValidatorCellDecorator> decorator : decorators.entrySet()) {
            columnRearranger.replace(createCellFactory(decorator.getValue(), in.findColumnIndex(decorator.getKey())),
                decorator.getKey());
        }

        if (modelSettings.m_missingColumnHandling != ColumnExistenceHandling.NONE) {
            handleMissingColumns(columnRearranger, referenceTableSpec);
        }

        return columnRearranger;
    }

    private static ColumnNamesAndDecorators extractConfiguredColumnNamesAndCreateValidatorDecorators(
        final Entry<String, ConfigurationContainer>[] array, final DataTableSpec referenceTableSpec,
        final TableStructureValidatorColConflicts conflicts, final DataTableSpec in) throws InvalidSettingsException {
        String[] namesOfInputSpec = new String[array.length];
        int index = 0;
        Map<String, TableStructureValidatorCellDecorator> decorators = new HashMap<>();
        for (Entry<String, ConfigurationContainer> arr : array) {
            namesOfInputSpec[index] = arr.getKey();
            ConfigurationContainer colConfigContainer = arr.getValue();

            final int colIndex = referenceTableSpec.findColumnIndex(colConfigContainer.getRefColName());
            if (colIndex < 0) {
                throw new InvalidSettingsException("The configured column '%s' is not present in the reference table."
                        .formatted(colConfigContainer.getRefColName()));
            }

            final TableStructureValidatorColConfiguration colConfig = colConfigContainer.getConfiguration();
            final boolean shouldBeTraversed =
                colConfig.applyColConfiguration(referenceTableSpec.getColumnSpec(colIndex),
                    in.getColumnSpec(arr.getKey()), conflicts);

            if (shouldBeTraversed) {
                decorators.put(
                    arr.getKey(),
                    colConfig.createCellValidator(referenceTableSpec.getColumnSpec(colIndex),
                        in.getColumnSpec(arr.getKey()), conflicts));
            }
            index++;
        }

        return new ColumnNamesAndDecorators(namesOfInputSpec, decorators);
    }

    @SuppressWarnings("java:S6218") // different record instances are not compared or printed
    record ColumnNamesAndDecorators(
        String[] columnNames, Map<String, TableStructureValidatorCellDecorator> decorators) {
    }

    private void handleMissingColumns(final ColumnRearranger columnRearranger, final DataTableSpec referenceTableSpec) {
        var i = 0;
        DataTableSpec resultSpec = columnRearranger.createSpec();
        final Set<String> configuredColumns = m_validationConfig.getConfiguredColumns();
        for (DataColumnSpec colSpec : referenceTableSpec) {
            // configured but not existing columns are filled with missing values.
            if (!resultSpec.containsName(colSpec.getName()) && configuredColumns.contains(colSpec.getName())) {
                final var handledMissingFactory = createMissingValsOnlyCellFactory(colSpec);
                if (i < columnRearranger.getColumnCount()) {
                    columnRearranger.insertAt(i, handledMissingFactory);
                } else {
                    /*
                     * At this point, we have more columns configured than the input provides, and also
                     * might have skipped some in between (since `i` is incremented unconditionally). Thus, we
                     * are in a weird state of trying to merge and output possibly disjoint specs together.
                     *
                     * This seemed to never have worked prior to AP-21796, and without re-working column handling
                     * of this old node, simply append missing columns seems like a fair solution.
                     */
                    columnRearranger.append(handledMissingFactory);
                }
            }
            i++;
        }
    }

    private static void addUnknownColumnConflicts(
        final String[] namesOfInputSpec, final DataTableSpec in, final TableStructureValidatorColConflicts conflicts) {
        if (namesOfInputSpec.length < in.getNumColumns()) {
            Set<String> configuredColumns = new HashSet<>(Arrays.asList(namesOfInputSpec));
            Set<String> allColumnsOfInSpec = new HashSet<>(Arrays.asList(in.getColumnNames()));
            allColumnsOfInSpec.removeAll(configuredColumns);
            // add the difference the unknown columns to the conflicts
            for (String col : allColumnsOfInSpec) {
                conflicts.addConflict(TableStructureValidatorColConflicts.unknownColumn(col));
            }
        }
    }

    private static SingleCellFactory createCellFactory(
        final TableStructureValidatorCellDecorator decorator, final int index) {

        return new SingleCellFactory(false, decorator.getDataColumnSpec()) {

            @Override
            public DataCell getCell(final DataRow row) {
                return decorator.handleCell(row.getKey(), row.getCell(index));
            }
        };
    }

    private static SingleCellFactory createMissingValsOnlyCellFactory(final DataColumnSpec spec) {

        return new SingleCellFactory(false, spec) {

            @Override
            public DataCell getCell(final DataRow row) {
                return DataType.getMissingCell();
            }
        };
    }

    private static Comparator<Entry<String, ConfigurationContainer>> sortAccordingToSpecComparator(
        final DataTableSpec referenceTableSpec) {
        return (o1, o2) -> {
            int firstIndex = referenceTableSpec.findColumnIndex(o1.getValue().getRefColName());
            int secondIndex = referenceTableSpec.findColumnIndex(o2.getValue().getRefColName());
            return Integer.compare(firstIndex, secondIndex);
        };
    }

    @Override
    protected void reset() {
        // no op
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

}
