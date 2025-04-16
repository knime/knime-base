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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.preproc.filter.rowref;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * The Reference Row Filter node allow the filtering of row IDs based on a second reference table. Two modes are
 * possible, either the corresponding row IDs of the first table are included or excluded in the resulting output table.
 *
 * @author Christian Dietz, University of Konstanz
 * @author Paul Baernreuther, KNIME
 * @param <S> the model settings
 * @since 3.1
 */
@SuppressWarnings("restriction")
public abstract class AbstractRowRefNodeModel<S extends AbstractRowFilterRefNodeSettings> extends WebUINodeModel<S> {

    /** The minimum number of elements that is being read from the reference table even if memory is low. */
    private static final long MIN_ELEMENTS_READ = 128;

    static final double FRACTION_DOMAIN_UPDATE = 0.2;

    /**
     * @param config
     * @param settingsClass
     * @since 5.5
     */
    public AbstractRowRefNodeModel(final WebUINodeConfiguration config, final Class<S> settingsClass) {
        super(config, settingsClass);
    }

    abstract DataTableSpec[] getOutputSpecs(DataTableSpec inputSpec);

    abstract BufferedDataTable[] noopExecute(BufferedDataTable inputTable);

    abstract OutputCreator createOutputCreator(DataTableSpec spec, ExecutionContext exec, S settings);

    static abstract class OutputCreator {

        abstract void addRow(DataRow row, boolean isInSet);

        abstract BufferedDataTable[] createTables(boolean updateDomains,
            Supplier<ExecutionContext> domainUpdateExecSupplier) throws CanceledExecutionException;

    }

    /**
     * @since 5.5
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final S settings)
        throws InvalidSettingsException {
        final var dataColumn = settings.m_dataColumn;
        final var dataColumnUsesRowID = dataColumn.getEnumChoice().isPresent();
        if (!dataColumnUsesRowID) {
            final var dataColName = dataColumn.getStringChoice();
            final DataColumnSpec dataColSpec = inSpecs[0].getColumnSpec(dataColName);
            if (dataColSpec == null) {
                throw new InvalidSettingsException("The selected data column \"" + dataColName
                    + "\" does not exist in the table that should be filtered.");
            }
        }
        final var refColumn = settings.m_referenceColumn;
        final var refColumnUsesRowID = refColumn.getEnumChoice().isPresent();
        if (!refColumnUsesRowID) {
            final var refColName = refColumn.getStringChoice();
            final DataColumnSpec refColSpec = inSpecs[1].getColumnSpec(refColName);
            if (refColSpec == null) {
                throw new InvalidSettingsException(
                    "The selected reference column \"" + refColName + "\" does not exist in the reference table.");
            }
        }
        if (dataColumnUsesRowID != refColumnUsesRowID) {
            if (dataColumnUsesRowID) {
                setWarningMessage("Using string representation of reference table column " + refColumnUsesRowID
                    + " for RowID comparison.");
            } else {
                setWarningMessage("Using string representation of data table column " + dataColumn.getStringChoice()
                    + " for RowID comparison.");
            }
        } else if (!dataColumnUsesRowID) { // i.e. both use column names
            final DataColumnSpec dataColSpec = inSpecs[0].getColumnSpec(dataColumn.getStringChoice());
            final DataColumnSpec refColSpec = inSpecs[1].getColumnSpec(refColumn.getStringChoice());

            if (!refColSpec.getType().equals(dataColSpec.getType())) {
                setWarningMessage(
                    "The selected columns have different type. Using string representation for comparison.");
            }
        }
        return getOutputSpecs(inSpecs[0]);
    }

    /**
     * @since 5.5
     */
    @SuppressWarnings({"null", "resource"})
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final S settings) throws Exception {

        final BufferedDataTable dataTable = inData[0];
        if (dataTable.size() < 1) {
            return noopExecute(dataTable);
        }
        final DataTableSpec dataTableSpec = dataTable.getSpec();
        final BufferedDataTable refTable = inData[1];
        final DataTableSpec refTableSpec = refTable.getSpec();
        //check if we have to use String for comparison
        boolean filterByString = false;
        final var dataColumn = settings.m_dataColumn;
        final var refColumn = settings.m_referenceColumn;
        final var useDataRowKey = dataColumn.getEnumChoice().isPresent();
        final var useRefRowKey = refColumn.getEnumChoice().isPresent();
        if (useDataRowKey != useRefRowKey) {
            filterByString = true;
        } else if (!useDataRowKey) {
            final DataColumnSpec refColSpec = refTableSpec.getColumnSpec(refColumn.getStringChoice());
            final DataColumnSpec datColSpec = dataTableSpec.getColumnSpec(dataColumn.getStringChoice());
            if (!refColSpec.getType().equals(datColSpec.getType())) {
                filterByString = true;
            }
        }

        final var outputCreator = createOutputCreator(dataTableSpec, exec, settings);

        final double refTableSizeFraction = (double)refTable.size() / (refTable.size() + dataTable.size());
        final double fractionWithoutDomainUpdate = settings.m_updateDomains ? 1.0 - FRACTION_DOMAIN_UPDATE : 1.0;
        final ExecutionMonitor readRefMon =
            exec.createSubExecutionContext(refTableSizeFraction * fractionWithoutDomainUpdate);
        final ExecutionMonitor writeMon =
            exec.createSubExecutionContext((1 - refTableSizeFraction) * fractionWithoutDomainUpdate);

        // we only init the disk-backed bit array if memory becomes low while reading the reference set
        final MemoryAlertSystem memSys = MemoryAlertSystem.getInstance();
        DiskBackedBitArray bitArray = null;
        boolean fullyFitsIntoMemory = true;

        long rowCnt = 0;
        final Iterator<DataRow> refTableIterator = refTable.iterator();
        final var refColIdx = toColIndex(refTableSpec, refColumn);
        final var dataColIdx = toColIndex(dataTableSpec, dataColumn);
        do {
            //create the set to filter by
            final Set<Object> keySet = new HashSet<Object>();

            long elementsRead = 0;
            while (refTableIterator.hasNext()) {
                exec.checkCanceled();
                if (filterByString) {
                    if (refColIdx.isEmpty()) {
                        keySet.add(refTableIterator.next().getKey().getString());
                    } else {
                        keySet.add(refTableIterator.next().getCell(refColIdx.get()).toString());
                    }
                } else {
                    if (refColIdx.isEmpty()) {
                        keySet.add(refTableIterator.next().getKey());
                    } else {
                        keySet.add(refTableIterator.next().getCell(refColIdx.get()));
                    }
                }
                readRefMon.setProgress(rowCnt++ / (double)refTable.size(), () -> "Reading reference table...");
                elementsRead++;

                if (memSys.isMemoryLow() && elementsRead >= MIN_ELEMENTS_READ) {
                    fullyFitsIntoMemory = false;
                    break;
                }
            }

            if (!fullyFitsIntoMemory) {
                if (bitArray == null) {
                    bitArray = new DiskBackedBitArray(dataTable.size());
                } else {
                    bitArray.setPosition(0);
                }
            }

            rowCnt = 1;
            for (final DataRow row : dataTable) {
                exec.checkCanceled();
                //get the right value to check for...
                final Object val2Compare;
                if (filterByString) {
                    if (dataColIdx.isEmpty()) {
                        val2Compare = row.getKey().getString();
                    } else {
                        val2Compare = row.getCell(dataColIdx.get()).toString();
                    }
                } else {
                    if (dataColIdx.isEmpty()) {
                        val2Compare = row.getKey();
                    } else {
                        val2Compare = row.getCell(dataColIdx.get());
                    }
                }

                if (fullyFitsIntoMemory) {
                    //...include/exclude matching rows by checking the val2Compare
                    writeMon.setProgress(rowCnt++ / (double)dataTable.size(), () -> "Filtering...");
                    outputCreator.addRow(row, keySet.contains(val2Compare));
                } else {
                    // use the bit array to memorize which rows to keep / discard
                    if (keySet.contains(val2Compare)) {
                        bitArray.setBit();
                    } else {
                        bitArray.skipBit();
                    }
                }
            }

        } while (refTableIterator.hasNext());

        if (!fullyFitsIntoMemory) {
            bitArray.setPosition(0);
            rowCnt = 1;
            for (final DataRow row : dataTable) {
                exec.checkCanceled();
                writeMon.setProgress(rowCnt++ / (double)dataTable.size(), () -> "Filtering...");
                outputCreator.addRow(row, bitArray.getBit());
            }
            bitArray.close();
        }

        return outputCreator.createTables(settings.m_updateDomains,
            () -> exec.createSubExecutionContext(FRACTION_DOMAIN_UPDATE));

    }

    /**
     * @return either an empty optional if row keys are used or containing a non-negative column index
     */
    private static Optional<Integer> toColIndex(final DataTableSpec spec, final StringOrEnum<RowIDChoice> column) {
        if (column.getEnumChoice().isPresent()) {
            return Optional.empty();
        }
        final var colIndex = spec.findColumnIndex(column.getStringChoice());
        CheckUtils.checkState(colIndex >= 0, "Column not found: " + column.getStringChoice());
        return Optional.of(colIndex);
    }

    static BufferedDataTable updateDomain(final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException {
        var domainCalculator = new DataTableDomainCreator(table.getDataTableSpec(), false);
        domainCalculator.updateDomain(table, exec);
        var specWithNewDomain = domainCalculator.createSpec();
        return exec.createSpecReplacerTable(table, specWithNewDomain);
    }

}
