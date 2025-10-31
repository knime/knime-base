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
package org.knime.base.node.preproc.datavalidator;

import static org.knime.core.node.util.CheckUtils.checkSetting;
import static org.knime.core.node.util.CheckUtils.checkSettingNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.base.node.preproc.datavalidator.DataValidatorColConflicts.DataValidatorColConflict;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.ConfigurationContainer;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.RejectBehavior;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.ValidateAtBehavior;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;

/**
 * NodeModel for the data validation node.
 *
 * @author Marcel Hansert, University of Konstanz
 * @since 2.10
 */
class DataValidatorNodeModel extends NodeModel {

    private DataValidatorConfiguration m_config;

    /** One input, two optional output. */
    DataValidatorNodeModel() {
        this(1, 2);
    }

    /**
     * One input, two optional output.
     *
     * @param input the input count
     * @param output the output count
     * */
    DataValidatorNodeModel(final int input, final int output) {
        super(input, output);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        checkSettingNotNull(m_config, "Configuration is missing.");
        DataTableSpec in = inSpecs[0];

        DataValidatorColConflicts conflicts = new DataValidatorColConflicts();
        ColumnRearranger columnRearranger = null;
        columnRearranger = createRearranger(in, conflicts);

        if (!conflicts.isEmpty() && ValidateAtBehavior.VALIDATE_BOTH == m_config.getValidateAtBehavior()) {
            checkSetting(RejectBehavior.FAIL_NODE != m_config.getFailingBehavior(), "Validation failed:\n%s",
                conflicts);
            return new DataTableSpec[]{null, DataValidatorColConflicts.CONFLICTS_SPEC};
        } else {
            return new DataTableSpec[]{columnRearranger.createSpec(), DataValidatorColConflicts.CONFLICTS_SPEC};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        DataTableSpec in = ((BufferedDataTable)inData[0]).getDataTableSpec();
        checkSettingNotNull(m_config, "Configuration is missing.");
        DataValidatorColConflicts conflicts = new DataValidatorColConflicts();
        ColumnRearranger columnRearranger = createRearranger(in, conflicts);

        if (!conflicts.isEmpty()) {
            switch (m_config.getFailingBehavior()) {
                case FAIL_NODE:
                    throw new InvalidSettingsException("Validation failed:\n" + conflicts);
                default:
            }
        }

        BufferedDataTable returnTable =
            exec.createColumnRearrangeTable((BufferedDataTable)inData[0], columnRearranger,
                exec.createSubExecutionContext(0.9));

        if (!conflicts.isEmpty()) {
            switch (m_config.getFailingBehavior()) {
                case OUTPUT_TO_PORT_CHECK_DATA:
                    return new PortObject[]{InactiveBranchPortObject.INSTANCE,
                        createConflictsTable(conflicts, exec.createSubExecutionContext(0.1))};
                case FAIL_NODE:
                    throw new InvalidSettingsException("Validation failed:\n" + conflicts);
            }
        }

        return new PortObject[]{returnTable, InactiveBranchPortObject.INSTANCE};
    }

    /**
     * @param conflicts
     * @param exec
     * @return
     */
    private BufferedDataTable createConflictsTable(final DataValidatorColConflicts conflicts,
        final ExecutionContext exec) {

        BufferedDataContainer createDataContainer = exec.createDataContainer(DataValidatorColConflicts.CONFLICTS_SPEC);

        int index = 0;
        for (DataValidatorColConflict conflict : conflicts) {
            createDataContainer.addRowToTable(conflict.toDataRow(RowKey.createRowKey(index++)));
        }
        createDataContainer.close();
        return createDataContainer.getTable();
    }

    /**
     * Creates the according {@link ColumnRearranger}.
     *
     * @param in input table
     * @param conflicts list of tracked validation conflicts
     * @return the column rearranger
     * @throws InvalidSettingsException if the settings do not allow to perform validation
     */
    private ColumnRearranger createRearranger(final DataTableSpec in, final DataValidatorColConflicts conflicts)
        throws InvalidSettingsException {
        Map<String, ConfigurationContainer> colToConfigs = m_config.applyConfiguration(in, conflicts);

        // sort the entries according to the reference spec - and the best case insensitive matchings.
        @SuppressWarnings("unchecked")
        Entry<String, ConfigurationContainer>[] array = colToConfigs.entrySet().toArray(new Entry[0]);
        Arrays.sort(array, sortAccordingToSpecComparator(m_config.getReferenceTableSpec()));

        String[] namesOfInputSpec = new String[array.length];
        int index = 0;

        Map<String, DataValidatorCellDecorator> decorators = new HashMap<>();

        for (Entry<String, ConfigurationContainer> arr : array) {
            namesOfInputSpec[index] = arr.getKey();
            ConfigurationContainer colConfigContainer = arr.getValue();

            final DataTableSpec referenceTableSpec = m_config.getReferenceTableSpec();
            final int colIndex = referenceTableSpec.findColumnIndex(colConfigContainer.getRefColName());
            if (colIndex < 0) {
                throw new InvalidSettingsException("The configured column '%s' is not present in the reference table."
                        .formatted(colConfigContainer.getRefColName()));
            }

            final DataValidatorColConfiguration colConfig = colConfigContainer.getConfiguration();
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

        ColumnRearranger columnRearranger = new ColumnRearranger(in);

        switch (m_config.getUnknownColumnsHandling()) {
            case REJECT:
                if (namesOfInputSpec.length < in.getNumColumns()) {
                    Set<String> configuredColumns = new HashSet<>(Arrays.asList(namesOfInputSpec));
                    Set<String> allColumnsOfInSpec = new HashSet<>(Arrays.asList(in.getColumnNames()));
                    allColumnsOfInSpec.removeAll(configuredColumns);
                    // add the difference the unknown columns to the conflicts
                    for (String col : allColumnsOfInSpec) {
                        conflicts.addConflict(DataValidatorColConflicts.unknownColumn(col));
                    }
                }
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

        for (Entry<String, DataValidatorCellDecorator> decorator : decorators.entrySet()) {
            columnRearranger.replace(createCellFactory(decorator.getValue(), in.findColumnIndex(decorator.getKey())),
                decorator.getKey());
        }

        DataTableSpec resultSpec = columnRearranger.createSpec();
        //add all missing columns as columns containing missing values only
        var i = 0;
        final Set<String> configuredColumns = m_config.getConfiguredColumns();
        for (DataColumnSpec colSpec : m_config.getReferenceTableSpec()) {
            // configured but not existing columns are filled with missing values.
            if (!resultSpec.containsName(colSpec.getName()) && configuredColumns.contains(colSpec.getName())) {
                final var missingFactory = createMissingValsOnlyCellFactory(colSpec);
                if (i < columnRearranger.getColumnCount()) {
                    columnRearranger.insertAt(i, missingFactory);
                } else {
                    /*
                     * At this point, we have more columns configured than the input provides, and also
                     * might have skipped some inbetween (since `i` is incremented unconditionally). Thus, we
                     * are in a weird state of trying to merge and output possibly disjoint specs together.
                     *
                     * This seemed to never have worked prior to AP-21796, and without re-working column handling
                     * of this old node, simply append missing columns seems like a fair solution.
                     */
                    columnRearranger.append(missingFactory);
                }
            }
            i++;
        }

        return columnRearranger;
    }

    private static SingleCellFactory createCellFactory(final DataValidatorCellDecorator decorator, final int index) {

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

    /**
     * @param referenceTableSpec
     * @return
     */
    private static Comparator<Entry<String, ConfigurationContainer>> sortAccordingToSpecComparator(
        final DataTableSpec referenceTableSpec) {

        return new Comparator<Entry<String, ConfigurationContainer>>() {

            @Override
            public int compare(final Entry<String, ConfigurationContainer> o1,
                final Entry<String, ConfigurationContainer> o2) {
                int firstIndex = referenceTableSpec.findColumnIndex(o1.getValue().getRefColName());
                int secondIndex = referenceTableSpec.findColumnIndex(o2.getValue().getRefColName());
                return Integer.compare(firstIndex, secondIndex);
            }

        };
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            m_config.saveSettings(settings);
        }
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataValidatorConfiguration dataValidatorConfiguration = createConfig();
        dataValidatorConfiguration.loadConfigurationInModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataValidatorConfiguration dataValidatorConfiguration = createConfig();
        dataValidatorConfiguration.loadConfigurationInModel(settings);
        m_config = dataValidatorConfiguration;
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

    /**
     * @return the config
     */
    protected DataValidatorConfiguration getConfig() {
        return m_config;
    }

    /**
     * @return the config
     */
    protected DataValidatorConfiguration getOrCreateDefaultConfig() {
        if (m_config == null) {
            m_config = createConfig();
        }
        return m_config;
    }

    /**
     * @return the config
     */
    protected DataValidatorConfiguration createConfig() {
        return new DataValidatorConfiguration();
    }
}
