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
 *   Dec 18, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.domain.editprobability;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.DoubleStream;

import org.knime.base.node.preproc.domain.editnominal.EditNominalDomainConfiguration;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.data.probability.nominal.NominalDistributionCell;
import org.knime.core.data.probability.nominal.NominalDistributionCellFactory;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;

import com.google.common.collect.Sets;

/**
 * This class is the implementation of the node model of the "EditProbabilityDistributionDomain" node. It enables the
 * manipulation of the nominal probability distribution domain values.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
final class EditProbabilityDistributionDomainNodeModel extends NodeModel {

    /**
     * Holds the configuration of the domain editor panel section
     */
    private final EditNominalDomainConfiguration m_configuration;

    /**
     * Holds the configuration regarding the epsilon value
     */
    private final EditEpsilonConfiguration m_epsilonConfiguration;

    /**
     * One input and one output port.
     *
     */
    protected EditProbabilityDistributionDomainNodeModel() {
        super(1, 1);
        m_configuration = new EditNominalDomainConfiguration();
        m_epsilonConfiguration = new EditEpsilonConfiguration();
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        checkConfiguration();
        return new DataTableSpec[]{
            createColumnRearranger(inSpecs[0], null, m_configuration.getConfiguredColumns()).createSpec()};
    }

    /**
     *
     * @param newOrder holds the order of the {@link DataCell} in the domain values
     * @return the meta data corresponding to the new order
     */
    private static NominalDistributionValueMetaData createNewMetaData(final List<DataCell> newOrder) {
        String[] arrayOfNewOrder =
            newOrder.stream().filter(x -> !x.equals(EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL))
                .map(DataCell::toString).toArray(String[]::new);
        return new NominalDistributionValueMetaData(arrayOfNewOrder);
    }

    /**
     * This class creates the new {@link DataCell} in case of modified domain values of selected columns
     *
     * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
     */
    private static class NominalDistributionEditor extends SingleCellFactory {

        private final NominalDistributionValueMetaData m_meta;

        private final NominalDistributionCellFactory m_factory;

        private final int m_columnIndex;

        private final double m_epsilonValue;

        NominalDistributionEditor(final DataColumnSpec newSpec, final FileStoreFactory fsFactory, final int columnIndex,
            final double epsilonValue) {
            super(newSpec);
            m_meta = NominalDistributionValueMetaData.extractFromSpec(newSpec);
            m_factory = fsFactory != null
                ? new NominalDistributionCellFactory(fsFactory, m_meta.getValues().toArray(new String[0])) : null;
            m_columnIndex = columnIndex;
            m_epsilonValue = epsilonValue;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            final DataCell dataCell = row.getCell(m_columnIndex);
            if (dataCell.isMissing()) {
                return dataCell;
            }
            final NominalDistributionCell nominalCell = (NominalDistributionCell)dataCell;
            final Set<String> previousProbabilityClasses = nominalCell.getKnownValues();
            final Set<String> probabilityClasses = m_meta.getValues();
            double[] probabilities = new double[m_meta.size()];
            final Set<String> addedValues = Sets.difference(probabilityClasses, previousProbabilityClasses);
            final Iterator<String> probabilityIterator = probabilityClasses.iterator();
            for (int i = 0; i < probabilities.length; i++) {
                final String probabilityToAdd = probabilityIterator.next();
                probabilities[i] =
                    (addedValues.contains(probabilityToAdd) ? 0 : nominalCell.getProbability(probabilityToAdd));
            }
            if (!previousProbabilityClasses.stream().allMatch(probabilityClasses::contains)) {
                probabilities = normalizeProbabilities(probabilities);
            }
            return m_factory.createCell(probabilities, m_epsilonValue);
        }

        private static double[] normalizeProbabilities(final double[] probabilities) {
            final double sum = DoubleStream.of(probabilities).sum();
            if (sum == 0) {
                final int length = probabilities.length;
                final double[] normalizedProbabilities = new double[length];
                Arrays.fill(normalizedProbabilities, 1.0 / length);
                return normalizedProbabilities;
            } else {
                return Arrays.stream(probabilities).map(x -> x / sum).toArray();
            }

        }
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        checkConfiguration();
        final BufferedDataTable dataTable = inData[0];
        final DataTableSpec dataTableSpec = dataTable.getDataTableSpec();
        final Set<String> configuredColumns = new HashSet<>(m_configuration.getConfiguredColumns());
        final FileStoreFactory fsFactory = FileStoreFactory.createFileStoreFactory(exec);
        if (configuredColumns.isEmpty()) {
            return new BufferedDataTable[]{dataTable};
        } else {
            final ColumnRearranger columnRearranger =
                createColumnRearranger(dataTableSpec, fsFactory, configuredColumns);
            return new BufferedDataTable[]{exec.createColumnRearrangeTable(dataTable, columnRearranger, exec)};
        }
    }

    /**
     * This method creates the {@link ColumnRearranger} to create the output table.
     *
     * @param dataTableSpec holds the {@link DataTableSpec} of the input table
     * @param fsFactory holds the {@link FileStoreFactory} to create the new {@link NominalDistributionCell} cells
     * @param configuredColumns configuredColumns holds the confgiured columns
     * @return the {@link ColumnRearranger}
     * @throws InvalidSettingsException in case a configured column may no longer exist or has changed type and it is
     *             not chosen to be ignored.
     */
    private ColumnRearranger createColumnRearranger(final DataTableSpec dataTableSpec, final FileStoreFactory fsFactory,
        final Set<String> configuredColumns) throws InvalidSettingsException {
        final ColumnRearranger columnRearranger = new ColumnRearranger(dataTableSpec);
        final Iterator<String> iterator = configuredColumns.iterator();
        final Set<String> configuredNotExistingColumns = new HashSet<>();
        while (iterator.hasNext()) {
            final String configuredColumn = iterator.next();
            if (!dataTableSpec.containsName(configuredColumn)) {
                configuredNotExistingColumns.add(configuredColumn);
            } else {
                final DataColumnSpec dataColumnSpec = dataTableSpec.getColumnSpec(configuredColumn);
                if (!dataColumnSpec.getType().isCompatible(NominalDistributionValue.class)) {
                    handleTypeIncompatibility(dataColumnSpec, configuredColumn);
                } else {
                    replaceConfiguredColumns(dataTableSpec, configuredColumn, columnRearranger, fsFactory);
                }
            }
        }
        checkForNotExistingColumns(configuredNotExistingColumns);
        return columnRearranger;
    }


    /**
     * This method checks for configured columns that no longer exist
     *
     * @param configuredNotExistingColumns hold the configured columns that no longer exist
     * @throws InvalidSettingsException if one of the configured no longer exists and it is not chosen to be
     *             ignored in the dialog.
     */
    private void checkForNotExistingColumns(final Set<String> configuredNotExistingColumns)
        throws InvalidSettingsException {
        if (!configuredNotExistingColumns.isEmpty()) {
            String missingColumnsString = "Following columns are configured but no longer exist: \n"
                    + ConvenienceMethods.getShortStringFrom(configuredNotExistingColumns, 5);
                CheckUtils.checkSetting(m_configuration.isIgnoreNotExistingColumns(), missingColumnsString);
                setWarningMessage(missingColumnsString);
        }
    }

    /**
     * This method handles cases when a configured column has changed type.
     *
     * @param dataColumnSpec holds the {@link DataColumnSpec} of the input table
     * @param configuredColumn holds the configured column
     * @throws InvalidSettingsException thrown if one of the configured columns has changed type and it is not chosen to
     *             be ignored in the dialog.
     */
    private void handleTypeIncompatibility(final DataColumnSpec dataColumnSpec, final String configuredColumn)
        throws InvalidSettingsException {
        CheckUtils.checkSetting(m_configuration.isIgnoreWrongTypes(),
            "Column '%s' must be of type '%s' but was of type: '%s'", configuredColumn,
            NominalDistributionValue.class.getTypeName(), dataColumnSpec.getType());
    }

    /**
     * This method replaces the olds columns with the new configured columns.
     *
     * @param dataTableSpec holds the {@link DataTableSpec} of the input table
     * @param configuredColumn holds the name of the confiugred column
     * @param columnRearranger holds the {@link ColumnRearranger}
     * @param fsFactory holds the {@link FileStoreFactory}
     */
    private void replaceConfiguredColumns(final DataTableSpec dataTableSpec, final String configuredColumn,
        final ColumnRearranger columnRearranger, final FileStoreFactory fsFactory) {
        final DataColumnSpecCreator newSpecCreator =
            new DataColumnSpecCreator(configuredColumn, NominalDistributionCellFactory.TYPE);
        newSpecCreator.addMetaData(createNewMetaData(m_configuration.getSorting(configuredColumn)), true);
        final NominalDistributionEditor editor = new NominalDistributionEditor(newSpecCreator.createSpec(), fsFactory,
            dataTableSpec.findColumnIndex(configuredColumn), Math.pow(10, -m_epsilonConfiguration.getEpsilonValue()));
        columnRearranger.replace(editor, configuredColumn);
    }

    /**
     * Checks if the node is configured.
     *
     * @throws InvalidSettingsException if the node is not configured
     */
    private void checkConfiguration() throws InvalidSettingsException {
        if (m_configuration == null) {
            throw new InvalidSettingsException("Missing Configuration.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_configuration.saveSettings(settings);
        m_epsilonConfiguration.saveSettings(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final EditNominalDomainConfiguration editNominalDomainConfiguration = new EditNominalDomainConfiguration();
        editNominalDomainConfiguration.loadConfigurationInModel(settings);
        final EditEpsilonConfiguration editEpsilonConfiguration = new EditEpsilonConfiguration();
        editEpsilonConfiguration.loadConfigurationInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuration.reset();
        m_configuration.loadConfigurationInModel(settings);
        m_epsilonConfiguration.loadConfigurationInModel(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no internals

    }

}
