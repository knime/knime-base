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
 *   Apr 14, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.partition;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.partition.PartitionNodeSettings.ActionOnEmptyInput;
import org.knime.base.node.preproc.sample.AbstractSamplingWebUINodeModel;
import org.knime.base.node.preproc.sample.StratifiedSamplingRowFilter;
import org.knime.base.util.prepoc.sample.SamplingUtil.CountMode;
import org.knime.base.util.prepoc.sample.SamplingUtil.SamplingMode;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;

/**
 * WebUI node model for 'Table Partitioner'.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class PartitionNodeModel extends AbstractSamplingWebUINodeModel<PartitionNodeSettings> {

    PartitionNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, PartitionNodeSettings.class);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final PartitionNodeSettings modelSettings) throws Exception {
        BufferedDataTable in = inData[0];
        BufferedDataTable[] outs = new BufferedDataTable[2];
        if (in.size() == 0 && modelSettings.m_actionEmpty.equals(ActionOnEmptyInput.FAIL)) {
            throw new InvalidSettingsException("Input table is empty.");
        }

        IRowFilter filter = getSamplingRowFilter(in, exec, modelSettings);
        final var containerSettings = DataContainerSettings.builder()//
            .withCheckDuplicateRowKeys(false)// we only copy parts of the input table
            .withInitializedDomain(true)// the domain of the input table is also valid for the output tables
            .withDomainUpdate(true)// unfortunately needed for backwards-compatibility
            .build();
        BufferedDataContainer firstOutCont = exec.createDataContainer(in.getDataTableSpec(), containerSettings);
        BufferedDataContainer secondOutCont = exec.createDataContainer(in.getDataTableSpec(), containerSettings);
        final double rowCount = in.size(); // floating point op. below
        // one of the flags will be set if one of the exceptions below
        // is thrown.
        boolean putRestInOut1 = false;
        boolean putRestInOut2 = false;
        try {
            int count = 0;
            for (DataRow row : in) {
                boolean matches = putRestInOut1;
                try {
                    // conditional check, will call "matches" only if necessary
                    matches |= (!putRestInOut2 && filter.matches(row, count));
                } catch (IncludeFromNowOn icf) {
                    assert !putRestInOut2;
                    putRestInOut1 = true;
                    matches = true;
                } catch (EndOfTableException ete) {
                    assert !putRestInOut1;
                    putRestInOut2 = true;
                    matches = false;
                }
                if (matches) {
                    firstOutCont.addRowToTable(row);
                } else {
                    secondOutCont.addRowToTable(row);
                }
                exec.setProgress(count / rowCount, "Processed row " + count + " (\"" + row.getKey() + "\")");
                exec.checkCanceled();
                count++;
            }
        } finally {
            firstOutCont.close();
            secondOutCont.close();
        }
        outs[0] = firstOutCont.getTable();
        outs[1] = secondOutCont.getTable();
        if (filter instanceof StratifiedSamplingRowFilter) {
            int classCount = ((StratifiedSamplingRowFilter)filter).getClassCount();
            if (classCount > outs[0].size()) {
                setWarningMessage("Class column contains more classes (" + classCount + ") than sampled rows ("
                    + outs[0].size() + ")");
            }
        }
        return outs;
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final PartitionNodeSettings modelSettings)
        throws InvalidSettingsException {
        final var inSpec = inSpecs[0];
        if (modelSettings.m_mode.equals(SamplingMode.STRATIFIED) && !inSpec.containsName(modelSettings.m_classColumn)) {
            throw new InvalidSettingsException(
                "Column '" + modelSettings.m_classColumn + "' for stratified sampling " + "does not exist");
        }
        DataTableSpec[] outs = new DataTableSpec[2];
        outs[0] = inSpecs[0];
        outs[1] = inSpecs[0];
        return outs;
    }

    @Override
    protected CountMode getCountMode(final PartitionNodeSettings settings) {
        return settings.m_partitioningMode;
    }

    @Override
    protected double getPercentage(final PartitionNodeSettings settings) {
        return settings.m_percentage;
    }

    @Override
    protected int getCount(final PartitionNodeSettings settings) {
        return settings.m_rowCount;
    }

    @Override
    protected SamplingMode getSamplingMode(final PartitionNodeSettings settings) {
        return settings.m_mode;
    }

    @Override
    protected String getClassColumn(final PartitionNodeSettings settings) {
        return settings.m_classColumn;
    }

    @Override
    protected Long getSeed(final PartitionNodeSettings settings) {
        return settings.m_useFixedRandomSeed ? settings.m_seed : null;
    }

}
