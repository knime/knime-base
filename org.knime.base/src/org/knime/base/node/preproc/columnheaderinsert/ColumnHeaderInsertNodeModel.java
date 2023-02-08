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
 *
 */
package org.knime.base.node.preproc.columnheaderinsert;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class ColumnHeaderInsertNodeModel extends NodeModel {

    private static final String ROW_KEY_CONSTANT = "<row-keys>";

    private ColumnHeaderInsertConfig m_config;

    /**  Two ins, one out. */
    public ColumnHeaderInsertNodeModel() {
        super(2, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec dictTable = inSpecs[1];
        if (m_config == null) {
            m_config = autoConfigure(dictTable);
        }
        String lookupCol = m_config.getLookupColumn();
        if (lookupCol != null && !ROW_KEY_CONSTANT.equals(lookupCol)) {
            DataColumnSpec lookupColSpec = dictTable.getColumnSpec(lookupCol);
            if (lookupColSpec == null) {
                throw new InvalidSettingsException("Cannot find the specified lookup column \"" + lookupCol + "\". "
                    + "Make sure it is present in the dictionary table (2nd input).");
            }
            if (!lookupColSpec.getType().isCompatible(StringValue.class)) {
                throw new InvalidSettingsException(
                    "The specified lookup column \"" + lookupCol + "\" is not String-compatible. "
                        + "Make sure the column type is String or related.");
            }
        } else {
            // use row key column
        }
        String valueColumn = m_config.getValueColumn();
        DataColumnSpec valueColumnSpec = dictTable.getColumnSpec(valueColumn);
        if (valueColumnSpec == null) {
            throw new InvalidSettingsException("Cannot find the specified value column \"" + valueColumn + "\". "
                    + "Make sure it is present in the dictionary table (2nd input).");
        }
        if (!valueColumnSpec.getType().isCompatible(StringValue.class)) {
            throw new InvalidSettingsException(
                "The specified value column \"" + valueColumn + "\" is not String-compatible. "
                    + "Make sure the column type is String or related.");
        }
        return null;
    }

    private static ColumnHeaderInsertConfig autoConfigure(final DataTableSpec dictSpec)
        throws InvalidSettingsException {
        var config = new ColumnHeaderInsertConfig();
        config.setFailIfNoMatch(true);
        config.setLookupColumn("<row-keys>");
        config.setValueColumn(findDefaultValueColumn(dictSpec).getName());
        return config;
    }

    private static DataColumnSpec findDefaultValueColumn(final DataTableSpec dictSpec) throws InvalidSettingsException {
        var valueColumn = dictSpec.stream()//
            .filter(c -> c.getType().equals(StringCell.TYPE))//
            .findFirst();
        if (valueColumn.isPresent()) {
            return valueColumn.get();
        } else {
            return dictSpec.stream()//
                .filter(c -> c.getType().isCompatible(StringValue.class))//
                .findFirst()
                .orElseThrow(() -> new InvalidSettingsException(
                    "No columns of type String were found in the input. Specify at least one."));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // init name map
        LinkedHashMap<String, String> dictionaryMap =
            new LinkedHashMap<String, String>();
        DataTableSpec dataSpec = inData[0].getDataTableSpec();
        for (DataColumnSpec dataCol : dataSpec) {
            dictionaryMap.put(dataCol.getName(), null);
        }

        // read dictionary
        BufferedDataTable dictionaryTable = inData[1];
        DataTableSpec dictionaryTableSpec = dictionaryTable.getDataTableSpec();
        String lookupColumn = m_config.getLookupColumn();
        int lookupColIdx = lookupColumn == null ? -1
                : dictionaryTableSpec.findColumnIndex(lookupColumn);
        String valueColumnIdx = m_config.getValueColumn();
        int valueColIndex = dictionaryTableSpec.findColumnIndex(valueColumnIdx);
        int rowIndex = 0;
        final long rowCount = dictionaryTable.size();
        for (DataRow row : dictionaryTable) {
            RowKey key = row.getKey();
            exec.setProgress(rowIndex / (double)rowCount, "Reading dictionary, "
                + "row \"" + key + "\" (" + rowIndex + "/" + rowCount + ")");
            rowIndex += 1;
            String lookup;
            if (lookupColIdx < 0) {
                lookup = row.getKey().getString();
            } else {
                DataCell c = row.getCell(lookupColIdx);
                lookup = c.isMissing() ? null
                        : ((StringValue)c).getStringValue();
            }
            if (!dictionaryMap.containsKey(lookup)) {
                continue;
            }
            DataCell valueCell = row.getCell(valueColIndex);
            // if missing, assign original column name
            String value = valueCell.isMissing() ? lookup
                    : ((StringValue)valueCell).getStringValue();
            if (dictionaryMap.put(lookup, value) != null) {
                throw new Exception("Multiple occurrences of lookup key \"" + lookup
                    + "\" have been found in dictionary table. The lookup keys must be unique. "
                    + "Duplicates can be removed, e.g. by using the GroupBy node.");
            }
        }

        // check consistency in new column name values
        HashSet<String> uniqNames = new HashSet<String>();
        for (Map.Entry<String, String> e : dictionaryMap.entrySet()) {
            String value = e.getValue();
            if (value == null) {
                if (m_config.isFailIfNoMatch()) {
                    throw new Exception("Cannot find a name value for the input \"" + e.getKey() + "\". "
                        + "Specify a name replacement for this column in the dictionary table (2nd input). "
                        + "Otherwise, uncheck the dialog option for the node not to fail.");
                } else {
                    value = e.getKey(); // (try to) keep original name
                }
            }
            String newName = value;
            int unifier = 1;
            while (!uniqNames.add(newName)) {
                newName = value + " (#" + (unifier++) + ")";
            }
            e.setValue(newName);
        }

        // assign new names
        DataColumnSpec[] cols = new DataColumnSpec[dataSpec.getNumColumns()];
        for (int i = 0; i < cols.length; i++) {
            DataColumnSpec c = dataSpec.getColumnSpec(i);
            DataColumnSpecCreator creator = new DataColumnSpecCreator(c);
            creator.setName(dictionaryMap.get(c.getName()));
            cols[i] = creator.createSpec();
        }
        DataTableSpec outSpec = new DataTableSpec(dataSpec.getName(), cols);
        BufferedDataTable outTable =
            exec.createSpecReplacerTable(inData[0], outSpec);
        return new BufferedDataTable[] {outTable};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            // can be null if the node was never configured because it was never connected
            m_config.saveConfiguration(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new ColumnHeaderInsertConfig().loadConfigurationInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ColumnHeaderInsertConfig c = new ColumnHeaderInsertConfig();
        c.loadConfigurationInModel(settings);
        m_config = c;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

}
