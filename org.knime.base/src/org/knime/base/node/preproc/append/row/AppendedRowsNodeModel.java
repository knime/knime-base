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
package org.knime.base.node.preproc.append.row;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.data.filter.column.FilterColumnRowInput;
import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedRowsIterator;
import org.knime.core.data.append.AppendedRowsIterator.TableIndexAndRowKey;
import org.knime.core.data.append.AppendedRowsRowInput;
import org.knime.core.data.append.AppendedRowsTable;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InternalTableAPI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.message.Message;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;

/**
 * {@link org.knime.core.node.NodeModel} that concatenates its two input table to one output table.
 *
 * @see AppendedRowsTable
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsNodeModel extends NodeModel {

    /**
     * NodeSettings key flag if to fail on duplicate ids . This option was added in v2.3 and will overrule the append
     * suffix/skip flags if true.
     */
    static final String CFG_FAIL_ON_DUPLICATES = "fail_on_duplicates";

    /**
     * NodeSettings key flag if new RowIDs should be generated. This option was added in v5.1 and will overrule the
     * other RowID related settings (fail, skip, append).
     */
    static final String CFG_NEW_ROWIDS = "create_new_rowids";

    /**
     * NodeSettings key if to append suffix (boolean). If false, skip the rows.
     */
    static final String CFG_APPEND_SUFFIX = "append_suffix";

    /** NodeSettings key: suffix to append. */
    static final String CFG_SUFFIX = "suffix";

    /** NodeSettings key: enable hiliting. */
    static final String CFG_HILITING = "enable_hiliting";

    /** NodeSettings key: Use only the intersection of columns. */
    static final String CFG_INTERSECT_COLUMNS = "intersection_of_columns";

    private boolean m_isFailOnDuplicate = false; //NOSONAR: be explicit

    private boolean m_isAppendSuffix = false; //NOSONAR: be explicit

    private boolean m_createNewRowIDs = true;

    private String m_suffix = "_dup";

    private boolean m_isIntersection;

    private boolean m_enableHiliting;

    /** Custom output handler */
    private final HiLiteHandler m_customHiliteHandler = new HiLiteHandler();

    /** Hilite translators for every input table. */
    private HiLiteTranslator[] m_hiliteTranslators;

    /** Default hilite handler used if hilite translation is disabled. */
    private final HiLiteHandler m_dftHiliteHandler = new HiLiteHandler();

    /**
     * Creates new node model with two inputs and one output.
     */
    public AppendedRowsNodeModel() {
        super(2, 1);
        m_hiliteTranslators = initHiLiteTranslators(2);
    }

    /**
     * Create new node with given number of inputs. All inputs except the first one are declared as optional.
     *
     * @param nrIns Nr inputs, must be >=1.
     */
    AppendedRowsNodeModel(final int nrIns) {
        super(getInPortTypes(nrIns), new PortType[]{BufferedDataTable.TYPE});
        m_hiliteTranslators = initHiLiteTranslators(nrIns);
    }

    /**
     * Constructor.
     *
     * @param portsConfiguration the ports configuration
     */
    AppendedRowsNodeModel(final PortsConfiguration portsConfiguration) {
        super(portsConfiguration.getInputPorts(), portsConfiguration.getOutputPorts());
        m_hiliteTranslators = initHiLiteTranslators(getNrInPorts());
    }

    private final HiLiteTranslator[] initHiLiteTranslators(final int n) {
        final var translators = new HiLiteTranslator[n];
        for (var i = 0; i < n; ++i) {
            translators[i] = new HiLiteTranslator();
            translators[i].addToHiLiteHandler(m_customHiliteHandler);
        }
        return translators;
    }

    private static final PortType[] getInPortTypes(final int nrIns) {
        if (nrIns < 1) {
            throw new IllegalArgumentException("invalid input count: " + nrIns);
        }
        final var result = new PortType[nrIns];
        Arrays.fill(result, BufferedDataTable.TYPE_OPTIONAL);
        result[0] = BufferedDataTable.TYPE;
        return result;
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] rawInData, final ExecutionContext exec)
        throws Exception {

        // remove all null tables first (optional input data)
        final var noNullArray = noNullArray(rawInData);
        final var noNullSpecs = new DataTableSpec[noNullArray.length];
        for (var i = 0; i < noNullArray.length; i++) {
            noNullSpecs[i] = noNullArray[i].getDataTableSpec();
        }

        //table can only be wrapped for the row id strategies append suffix, fail and create new
        if (m_isAppendSuffix || m_isFailOnDuplicate || m_createNewRowIDs) {
            //just wrap the tables virtually instead of traversing it and copying the rows

            var concatTable = concatenate(exec, noNullArray);
            if (m_isIntersection) {
                //wrap the table and filter the non-intersecting columns
                final var actualOutSpec = getOutputSpec(noNullSpecs);
                final var currentOutSpec = concatTable.getDataTableSpec();
                final var intersectCols = getIntersection(actualOutSpec, currentOutSpec);
                final var cr = new ColumnRearranger(currentOutSpec);
                cr.keepOnly(intersectCols);
                concatTable = exec.createColumnRearrangeTable(concatTable, cr, exec);
            }
            if (m_enableHiliting) {
                createHiliteMappings(exec, noNullArray);
            }
            return new BufferedDataTable[]{concatTable};
        } else {
            //traverse the table and copy the rows
            var totalRowCount = 0L;
            final var inputs = new RowInput[noNullArray.length];
            for (var i = 0; i < noNullArray.length; i++) {
                totalRowCount += noNullArray[i].size();
                inputs[i] = new DataTableRowInput(noNullArray[i]);
            }
            final var outputSpec = getOutputSpec(noNullSpecs);
            final var output = new BufferedDataTableRowOutput(
                exec.createDataContainer(outputSpec, DataContainerSettings.getDefault()));
            run(inputs, output, exec, totalRowCount);
            return new BufferedDataTable[]{output.getDataTable()};
        }

    }

    private BufferedDataTable concatenate(final ExecutionContext exec, final BufferedDataTable[] tables)
        throws CanceledExecutionException {
        if (m_createNewRowIDs) {
            return InternalTableAPI.concatenateWithNewRowID(exec, tables);
        } else {
            //virtually create the concatenated table (no traverse necessary)
            Optional<String> suffix = m_isAppendSuffix ? Optional.of(m_suffix) : Optional.empty();
            return exec.createConcatenateTable(exec, suffix, m_isFailOnDuplicate, tables);
        }
    }

    private static BufferedDataTable[] noNullArray(final BufferedDataTable[] rawInData) {
        final var nonNullList = new ArrayList<BufferedDataTable>();
        for (BufferedDataTable t : rawInData) {
            if (t != null) {
                nonNullList.add(t);
            }
        }
        return nonNullList.toArray(new BufferedDataTable[nonNullList.size()]);
    }

    private static RowInput[] noNullArray(final RowInput[] rawInData) {
        final var nonNullList = new ArrayList<RowInput>();
        for (RowInput t : rawInData) {
            if (t != null) {
                nonNullList.add(t);
            }
        }
        return nonNullList.toArray(new RowInput[nonNullList.size()]);
    }

    void run(final RowInput[] inputs, final RowOutput output, final ExecutionContext exec, final long totalRowCount)
        throws InterruptedException, CanceledExecutionException {
        RowInput[] corrected;
        if (m_isIntersection) {
            final RowInput[] noNullArray = noNullArray(inputs);
            corrected = new RowInput[noNullArray.length];
            final var inSpecs = new DataTableSpec[noNullArray.length];
            for (var i = 0; i < noNullArray.length; i++) {
                inSpecs[i] = noNullArray[i].getDataTableSpec();
            }
            String[] intersection = getIntersection(inSpecs);
            for (var i = 0; i < noNullArray.length; i++) {
                corrected[i] = new FilterColumnRowInput(noNullArray[i], intersection);
            }
        } else {
            corrected = inputs;
        }

        final var appendedInput = AppendedRowsRowInput.create(corrected, getDuplicatePolicy(), m_suffix, exec,
            totalRowCount, m_enableHiliting);
        try {
            DataRow next;
            // note, this iterator throws runtime exceptions when canceled.
            while ((next = appendedInput.poll()) != null) {
                // may throw exception, also sets progress
                output.push(next);
            }
        } catch (AppendedRowsIterator.RuntimeCanceledExecutionException rcee) { //NOSONAR: only interested in cause
            throw rcee.getCause();
        } finally {
            output.close();
        }
        if (appendedInput.getNrRowsSkipped() > 0) {
            setWarningMessage("Filtered out " + appendedInput.getNrRowsSkipped() + " duplicate row(s).");
        }
        if (m_enableHiliting) {
            createHiliteTranslation(exec, appendedInput.getDuplicateNameMapWithIndices(), corrected.length);
        }
    }

    private AppendedRowsTable.DuplicatePolicy getDuplicatePolicy() {
        if (m_createNewRowIDs) {
            return AppendedRowsTable.DuplicatePolicy.CreateNew;
        } else if (m_isFailOnDuplicate) {
            return AppendedRowsTable.DuplicatePolicy.Fail;
        } else if (m_isAppendSuffix) {
            return AppendedRowsTable.DuplicatePolicy.AppendSuffix;
        } else {
            return AppendedRowsTable.DuplicatePolicy.Skip;
        }
    }

    private DataTableSpec getOutputSpec(final DataTableSpec[] nonNullInSpecs) {
        DataTableSpec[] corrected;
        if (m_isIntersection) {
            corrected = new DataTableSpec[nonNullInSpecs.length];
            String[] intersection = getIntersection(nonNullInSpecs);
            for (var i = 0; i < nonNullInSpecs.length; i++) {
                corrected[i] = FilterColumnTable.createFilterTableSpec(nonNullInSpecs[i], intersection);
            }
        } else {
            corrected = nonNullInSpecs;
        }
        return AppendedRowsTable.generateDataTableSpec(corrected);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] rawInSpecs) throws InvalidSettingsException {
        List<DataTableSpec> noNullSpecList = new ArrayList<>();
        for (DataTableSpec s : rawInSpecs) {
            if (s != null) {
                noNullSpecList.add(s);
            }
        }
        DataTableSpec[] noNullSpecs = noNullSpecList.toArray(new DataTableSpec[noNullSpecList.size()]);
        DataTableSpec outputSpec = getOutputSpec(noNullSpecs);
        return new DataTableSpec[]{outputSpec};
    }

    /**
     * Determines the names of columns that appear in all specs.
     *
     * @param specs specs to check
     * @return column names that appear in all columns
     */
    static String[] getIntersection(final DataTableSpec... specs) {
        final var hash = new LinkedHashSet<String>();
        if (specs.length > 0) {
            for (DataColumnSpec c : specs[0]) {
                hash.add(c.getName());
            }
        }
        final var hash2 = new LinkedHashSet<String>();
        for (var i = 1; i < specs.length; i++) {
            hash2.clear();
            for (DataColumnSpec c : specs[i]) {
                hash2.add(c.getName());
            }
            hash.retainAll(hash2);
        }
        return hash.toArray(new String[hash.size()]);
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {
            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final var noNullList = new ArrayList<RowInput>();
                for (PortInput p : inputs) {
                    if (p != null) {
                        noNullList.add((RowInput)p);
                    }
                }
                RowInput[] rowInputs = noNullList.toArray(new RowInput[noNullList.size()]);
                run(rowInputs, (RowOutput)outputs[0], exec, -1);
            }
        };
    }

    @Override
    public InputPortRole[] getInputPortRoles() {
        final var result = new InputPortRole[getNrInPorts()];
        Arrays.fill(result, InputPortRole.NONDISTRIBUTED_STREAMABLE);
        return result;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_NEW_ROWIDS, m_createNewRowIDs);
        // added in v2.3
        settings.addBoolean(CFG_FAIL_ON_DUPLICATES, m_isFailOnDuplicate);
        settings.addBoolean(CFG_APPEND_SUFFIX, m_isAppendSuffix);
        settings.addBoolean(CFG_INTERSECT_COLUMNS, m_isIntersection);
        if (m_suffix != null) {
            settings.addString(CFG_SUFFIX, m_suffix);
        }
        settings.addBoolean(CFG_HILITING, m_enableHiliting);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getBoolean(CFG_INTERSECT_COLUMNS);
        // added v2.3
        final var isFailOnDuplicate = settings.getBoolean(CFG_FAIL_ON_DUPLICATES, false);
        final var appendSuffix = settings.getBoolean(CFG_APPEND_SUFFIX);
        if (isFailOnDuplicate) {
            // ignore suffix
        } else if (appendSuffix) {
            final var suffix = settings.getString(CFG_SUFFIX);
            if (suffix == null || suffix.equals("")) {
                throw new InvalidSettingsException("Invalid suffix: " + suffix);
            }
        } else { // skip duplicates
            // ignore suffix
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_isIntersection = settings.getBoolean(CFG_INTERSECT_COLUMNS);
        // added in v5.1
        m_createNewRowIDs = settings.getBoolean(CFG_NEW_ROWIDS, false);
        // added in v2.3
        m_isFailOnDuplicate = settings.getBoolean(CFG_FAIL_ON_DUPLICATES, false);
        m_isAppendSuffix = settings.getBoolean(CFG_APPEND_SUFFIX);
        if (m_isAppendSuffix) {
            m_suffix = settings.getString(CFG_SUFFIX);
        } else {
            // may be in there, but must not necessarily
            m_suffix = settings.getString(CFG_SUFFIX, m_suffix);
        }
        m_enableHiliting = settings.getBoolean(CFG_HILITING, false);
    }

    @Override
    protected void reset() {
        for (var t : m_hiliteTranslators) {
            t.setMapper(null);
        }
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (!m_enableHiliting) {
            return;
        }
        try (final var is =
            new GZIPInputStream(new FileInputStream(new File(nodeInternDir, "hilite_mapping.xml.gz")))) {
            final NodeSettingsRO config = NodeSettings.loadFromXML(is);
            if (config.getBoolean("individual_ports", false)) {
                // load hilite mappings
                for (var i = 0; i < m_hiliteTranslators.length; ++i) {
                    final var portConfig = config.getConfig("input_" + i);
                    m_hiliteTranslators[i].setMapper(DefaultHiLiteMapper.load(portConfig));
                }
            } else {
                // old node, needs to be re-executed
                setWarning(Message.fromSummary("Please re-execute the node to enable hiliting."));
            }
        } catch (InvalidSettingsException ise) {
            throw new IOException(ise.getMessage(), ise);
        }
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (!m_enableHiliting) {
            return;
        }
        final var config = new NodeSettings("hilite_mapping");
        config.addBoolean("individual_ports", true);
        for (var i = 0; i < m_hiliteTranslators.length; ++i) {
            final var portConfig = config.addConfig("input_" + i);
            if (m_hiliteTranslators[i].getMapper() instanceof DefaultHiLiteMapper dhm) {
                dhm.save(portConfig);
            }
        }
        try (final var os =
            new GZIPOutputStream(new FileOutputStream(new File(nodeInternDir, "hilite_mapping.xml.gz")))) {
            config.saveToXML(os);
        }
    }

    @Override
    protected void setInHiLiteHandler(final int inIndex, final HiLiteHandler hiLiteHdl) {
        if (m_hiliteTranslators[inIndex] != null) {
            m_hiliteTranslators[inIndex].dispose();
        }
        m_hiliteTranslators[inIndex] = new HiLiteTranslator(hiLiteHdl);
        m_hiliteTranslators[inIndex].addToHiLiteHandler(m_customHiliteHandler);
    }

    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (m_enableHiliting) {
            return m_customHiliteHandler;
        } else {
            return m_dftHiliteHandler;
        }
    }

    private void createHiliteMappings(final ExecutionContext exec, final BufferedDataTable[] tables)
        throws CanceledExecutionException {
        if (m_createNewRowIDs) {
            createNewRowIDHiliteTranslation(exec, tables);
        } else {
            final var appendedTableTMP = new AppendedRowsTable(getDuplicatePolicy(), m_suffix, tables);
            try (final var it = appendedTableTMP.iterator(exec, -1)) {
                while (it.hasNext()) {
                    it.next(); // iterate so that the iterator fills the duplicate map
                }
                final var dupMap = it.getDuplicateNameMapWithIndices();
                createHiliteTranslation(exec, dupMap, tables.length);
            }
        }
    }

    private void createHiliteTranslation(final ExecutionContext exec, final Map<RowKey, TableIndexAndRowKey> dupMap,
        final int n) throws CanceledExecutionException {

        // list because of generic types
        final List<Map<RowKey, Set<RowKey>>> maps = new ArrayList<>();
        for (var i = 0; i < n; ++i) {
            final var map = new HashMap<RowKey, Set<RowKey>>();
            maps.add(map);
        }

        // map of all RowKeys and duplicate RowKeys in the resulting table
        for (Map.Entry<RowKey, TableIndexAndRowKey> e : dupMap.entrySet()) {
            final var outKey = e.getKey();
            final var inKey = e.getValue().key();
            final var inTableIndex = e.getValue().index();
            Set<RowKey> set = Collections.singleton(outKey);
            // put key and original key into map for the specific table index
            maps.get(inTableIndex).put(inKey, set);
            exec.checkCanceled();
        }

        for (var i = 0; i < n; ++i) {
            m_hiliteTranslators[i].setMapper(new DefaultHiLiteMapper(maps.get(i)));
        }

    }

    private void createNewRowIDHiliteTranslation(final ExecutionContext exec, final BufferedDataTable[] tables)
        throws CanceledExecutionException {
        var rowIndex = 0l;
        var tableIndex = 0;
        for (var table : tables) {
            Map<RowKey, Set<RowKey>> translation = new HashMap<>();
            try (var iterator = table.filter(TableFilter.materializeCols()).iterator()) {
                while (iterator.hasNext()) {
                    translation.put(iterator.next().getKey(), Set.of(RowKey.createRowKey(rowIndex)));
                    ++rowIndex;
                    exec.checkCanceled();
                }
            }
            m_hiliteTranslators[tableIndex].setMapper(new DefaultHiLiteMapper(translation));
            ++tableIndex;
        }
    }
}
