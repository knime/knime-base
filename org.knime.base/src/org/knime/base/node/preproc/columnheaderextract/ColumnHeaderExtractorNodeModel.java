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
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.columnheaderextract;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.DataValueColumnFilter;
import org.knime.core.webui.node.dialog.impl.Schema;

/**
 * This is the model implementation of ColumnHeaderExtractor.
 *
 *
 * @author Bernd Wiswedel
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class ColumnHeaderExtractorNodeModel extends NodeModel {

    static final String CFG_TRANSPOSE_COL_HEADER = "transposeColHeader";

    static final String CFG_COLTYPE = "coltype";

    /** Selected column type. */
    enum ColType {
        /** All columns. */
        @Schema(title = "All")
        ALL(DataValue.class),
        /** String-compatible columns. */
        @Schema(title = "String")
        STRING(StringValue.class),
        /** Integer-compatible columns. */
        @Schema(title = "Integer")
        INTEGER(IntValue.class),
        /** Double-compatible columns. */
        @Schema(title = "Double")
        DOUBLE(DoubleValue.class);

        private final ColumnFilter m_filter;

        ColType(final Class<? extends DataValue> cl) {
            m_filter = new DataValueColumnFilter(cl);
        }

        /** @return title-case version of the type's name */
        String displayString() {
            final String upper = name();
            return upper.charAt(0) + upper.substring(1).toLowerCase(Locale.US);
        }

        static ColType fromDisplayString(final String displayString) throws InvalidSettingsException {
            return Stream.of(values())
                    .filter(ct -> ct.displayString().equals(displayString))
                    .findFirst()
                    .orElseThrow(() -> new InvalidSettingsException("Unable to get col type for \""
                            + displayString + "\""));
        }

        /** @return associated filter. */
        public ColumnFilter getFilter() {
            return m_filter;
        }
    }

    private final HiLiteHandler m_hiliteHandler = new HiLiteHandler();

    private final SettingsModelBoolean m_replaceColHeader;

    private final SettingsModelString m_unifyHeaderPrefix;

    private final SettingsModelString m_colTypeFilter;

    private final SettingsModelBoolean m_transposeColHeader;

    /**
     * Constructor for the node model.
     */
    protected ColumnHeaderExtractorNodeModel() {
        super(1, 2);
        m_replaceColHeader = createReplaceColHeader();
        m_unifyHeaderPrefix = createUnifyHeaderPrefix(m_replaceColHeader);
        m_colTypeFilter = createColTypeFilter();
        m_transposeColHeader = createTransposeColHeader();
    }

    /**
     * Computes the columns to be renamed and their new names.
     *
     * @param inSpec input table spec
     * @return map from column index of columns to be renamed to their new name
     * @throws InvalidSettingsException in case of problems with settings
     */
    private Map<Integer, String> renamingScheme(final DataTableSpec inSpec) throws InvalidSettingsException {
        final var namePrefix = m_unifyHeaderPrefix.getStringValue();
        final Set<String> usedNames = new HashSet<>();

        // look up filter type
        final var colTypeStr = m_colTypeFilter.getStringValue();
        final ColumnFilter filter = ColType.fromDisplayString(colTypeStr).getFilter();

        // use a linked hash map here to preserve column order when renaming
        final LinkedHashMap<Integer, String> rename = new LinkedHashMap<>();
        for (var i = 0; i < inSpec.getNumColumns(); i++) {
            final var col = inSpec.getColumnSpec(i);
            if (filter.includeColumn(col)) {
                // put a placeholder in first, we'll decide on the name later
                rename.put(i, null);
            } else {
                usedNames.add(col.getName());
            }
        }

        var index = 0; // re-use index in loop - prevent repeated adds to the hash set - fixes bug 5920
        for (final Entry<Integer, String> e : rename.entrySet()) {
            String newName;
            do {
                newName = namePrefix + index;
                index++;
            } while (!usedNames.add(newName));
            e.setValue(newName);
        }

        return rename;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        final var inSpec = inData[0].getDataTableSpec();
        final Map<Integer, String> scheme = renamingScheme(inSpec);
        DataTableSpec spec0 = createOutSpecPort0(inSpec, scheme);
        DataTableSpec spec1 = createOutSpecPort1(inSpec, scheme);

        BufferedDataContainer cont = exec.createDataContainer(spec0);
        if (m_transposeColHeader.getBooleanValue()) {
            // one row per selected column with old name as value and new name as row ID
            final var rename = m_replaceColHeader.getBooleanValue();
            for (final Entry<Integer, String> e : scheme.entrySet()) {
                final DataColumnSpec colSpec = inSpec.getColumnSpec(e.getKey());
                cont.addRowToTable(new DefaultRow(rename ? e.getValue() : colSpec.getName(), colSpec.getName()));
            }
        } else {
            // one row with the original names of all selected columns
            cont.addRowToTable(new DefaultRow("Column Header",
                scheme.keySet().stream().map(i -> inSpec.getColumnSpec(i).getName()).toArray(String[]::new)));
        }
        cont.close();
        BufferedDataTable table0 = cont.getTable();
        BufferedDataTable table1 = exec.createSpecReplacerTable(inData[0], spec1);

        return new BufferedDataTable[]{table0, table1};
    }

    @Override
    protected void reset() {
        // no internals
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        final Map<Integer, String> scheme = renamingScheme(inSpec);
        DataTableSpec spec0 = createOutSpecPort0(inSpec, scheme);
        DataTableSpec spec1 = createOutSpecPort1(inSpec, scheme);
        return new DataTableSpec[]{spec0, spec1};
    }

    /**
     * Computes the table specification for the column header output.
     *
     * @param spec input spec
     * @param scheme renaming scheme
     * @return output spec
     */
    private DataTableSpec createOutSpecPort0(final DataTableSpec spec, final Map<Integer, String> scheme) {
        final String[] colNames;
        if (m_transposeColHeader.getBooleanValue()) {
            colNames = new String[] { "Column Header" };
        } else if (m_replaceColHeader.getBooleanValue()) {
            colNames = scheme.values().stream().toArray(String[]::new);
        } else {
            colNames = scheme.keySet().stream().map(i -> spec.getColumnSpec(i).getName()).toArray(String[]::new);
        }
        final var cols = new DataColumnSpec[colNames.length];
        for (var i = 0; i < cols.length; i++) {
            cols[i] = new DataColumnSpecCreator(colNames[i], StringCell.TYPE).createSpec();
        }
        return new DataTableSpec("Column Headers", cols);
    }

    /**
     * Computes the table specification for the potentially renamed table output.
     *
     * @param spec input spec
     * @param scheme renaming scheme
     * @return output spec
     */
    private DataTableSpec createOutSpecPort1(final DataTableSpec spec, final Map<Integer, String> scheme) {
        if (m_replaceColHeader.getBooleanValue()) {
            final List<DataColumnSpec> colSpecs = new ArrayList<>();
            for (var i = 0; i < spec.getNumColumns(); i++) {
                final DataColumnSpec c = spec.getColumnSpec(i);
                final String newName = scheme.get(i);
                if (newName != null) {
                    final var newSpecCreator = new DataColumnSpecCreator(c);
                    newSpecCreator.setName(newName);
                    colSpecs.add(newSpecCreator.createSpec());
                } else if (true) {
                    colSpecs.add(c);
                }
            }
            return new DataTableSpec(spec.getName(), colSpecs.toArray(DataColumnSpec[]::new));
        } else {
            return spec;
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_replaceColHeader.saveSettingsTo(settings);
        m_unifyHeaderPrefix.saveSettingsTo(settings);
        m_colTypeFilter.saveSettingsTo(settings);
        m_transposeColHeader.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_replaceColHeader.loadSettingsFrom(settings);
        m_unifyHeaderPrefix.loadSettingsFrom(settings);
        m_colTypeFilter.loadSettingsFrom(settings);
        if (settings.containsKey(m_transposeColHeader.getConfigName())) {
            // this option was added in 4.7.0
            m_transposeColHeader.loadSettingsFrom(settings);
        }
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_replaceColHeader.validateSettings(settings);
        m_unifyHeaderPrefix.validateSettings(settings);
        m_colTypeFilter.validateSettings(settings);
        if (settings.containsKey(m_transposeColHeader.getConfigName())) {
            // this option was added in 4.7.0
            m_transposeColHeader.validateSettings(settings);
        }
    }

    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // no internals
    }

    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        switch (outIndex) {
        case 0:
            return m_hiliteHandler;
        case 1:
            return super.getOutHiLiteHandler(0);
        default:
            throw new IndexOutOfBoundsException("Invalid port: " + outIndex);
        }
    }

    /** @return new settings model for replace col header property. */
    static SettingsModelBoolean createReplaceColHeader() {
        return new SettingsModelBoolean("replaceColHeader", true);
    }

    /** @param replaceColHeader column header model (enable/disable listener)
     * @return new settings model for prefix of new header */
    static SettingsModelString createUnifyHeaderPrefix(final SettingsModelBoolean replaceColHeader) {
        final var result = new SettingsModelString("unifyHeaderPrefix", "Column ");
        replaceColHeader.addChangeListener(chEvent -> result.setEnabled(replaceColHeader.getBooleanValue()));
        return result;
    }

    /** @return new settings model for column filter type. */
    static SettingsModelString createColTypeFilter() {
        return new SettingsModelString(CFG_COLTYPE, ColType.ALL.displayString());
    }

    /** @return new settings model for transpose col header property. */
    static SettingsModelBoolean createTransposeColHeader() {
        return new SettingsModelBoolean(CFG_TRANSPOSE_COL_HEADER, false);
    }

}
