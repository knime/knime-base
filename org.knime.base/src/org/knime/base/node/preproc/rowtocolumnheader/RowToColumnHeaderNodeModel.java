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
 *   Sep 8, 2022 (leonard.woerteler): created
 */
package org.knime.base.node.preproc.rowtocolumnheader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVGuessableType;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.convert.map.CellValueProducerFactory;
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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;
import org.knime.filehandling.core.node.table.reader.DefaultProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.spec.DefaultExtractColumnHeaderRead;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecGuesser;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * This is the model implementation of the "Row To Column Header" node.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public final class RowToColumnHeaderNodeModel extends NodeModel {

    /**
     * Internal interface for functions that extract information from rows and may throw exceptions.
     *
     * @param <T> type of the value extracted from the row
     * @param <E> type of the exceptions to throw
     */
    interface RowConverter<T, E extends Exception> {
        /**
         * Converts the given row into some object.
         *
         * @param key row key
         * @param row randomly accessible row object
         * @return converted value
         * @throws Exception any exception throws during the conversion
         */
        T convert(final String key, final RandomAccessible<String> row) throws E;
    }

    /** Number of processed rows after which progress should be updated. */
    private static final int PROGRESS_UPDATE_INTERVAL = 1000;

    /** Configuration key for the header row index. */
    static final String HEADER_ROW_INDEX = "headerRowIndex";
    /** Settings model for the header row index. */
    private final SettingsModelInteger m_headerRowIndex;

    /** Configuration key for the flag indicating whether rows before the header should be skipped. */
    static final String DISCARD_BEFORE = "discardBefore";
    /** Settings model for the flag indicating whether rows before the header should be skipped. */
    private final SettingsModelBoolean m_discardBefore;

    /** Configuration key for the flag indicating whether data types should be detected. */
    static final String DETECT_TYPES = "detectTypes";
    /** Settings model for the flag indicating whether data types should be detected. */
    private final SettingsModelBoolean m_detectTypes;

    /** Table specification of the second outport. */
    private static final DataTableSpec RENAMINGS_SPEC = new DataTableSpecCreator().addColumns(
            new DataColumnSpecCreator("Old column name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("New column name", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("New column type", StringCell.TYPE).createSpec()).createSpec();

    private final HiLiteHandler m_hiliteHandler = new HiLiteHandler();

    /**
     * Constructor for the node model.
     */
    protected RowToColumnHeaderNodeModel() {
        super(1, 2);
        m_headerRowIndex = createHeaderRowIndex();
        m_discardBefore = createDiscardBefore();
        m_detectTypes = createDetectTypes();

    }

    /* ############################################ Streaming Execution ############################################# */

    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[] { InputPortRole.NONDISTRIBUTED_NONSTREAMABLE };
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[] { OutputPortRole.NONDISTRIBUTED, OutputPortRole.NONDISTRIBUTED };
    }

    @Override
    public StreamableOperatorInternals createInitialStreamableOperatorInternals() {
        return new RowToColumnHeaderOperator.Internals();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // we can't know the spec of the first outport because it is determined by the contents of the input
        return new PortObjectSpec[]{ null, RENAMINGS_SPEC };
    }

    @Override
    public boolean iterate(final StreamableOperatorInternals internals) {
        // one iteration is needed to find the output spec
        return !((RowToColumnHeaderOperator.Internals) internals).hasDetectedSpec();
    }

    @Override
    public PortObjectSpec[] computeFinalOutputSpecs(final StreamableOperatorInternals internals,
            final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec inSpec = (DataTableSpec) inSpecs[0];

        final var nodeInternals = (RowToColumnHeaderOperator.Internals) internals;
        final var columnNames = nodeInternals.getColumnNames();
        final var columnTypes = nodeInternals.getColumnTypes();
        final DataTableSpec outSpec = assembleOutSpec(inSpec, columnNames, columnTypes);

        return new PortObjectSpec[] { outSpec, RENAMINGS_SPEC };
    }

    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
            final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final var skipRows = m_discardBefore.getBooleanValue();
        final var detectTypes = m_detectTypes.getBooleanValue();
        final var headerRowIndex = m_headerRowIndex.getIntValue();
        return new RowToColumnHeaderOperator(skipRows, detectTypes, headerRowIndex);
    }

    /**
     * Streamable operator for streaming execution of this node.
     */
    private static final class RowToColumnHeaderOperator extends StreamableOperator {

        private final boolean m_skipRows;

        private final boolean m_detectTypes;

        private final int m_headerRowIndex;

        private String[] m_columnNames;

        private Class<?>[] m_columnTypes;

        private RowToColumnHeaderOperator(final boolean skipRows, final boolean detectTypes, final int headerRowIndex) {
            if (headerRowIndex < 0) {
                throw new IllegalArgumentException("Header row index cannot be negative, found " + headerRowIndex);
            }
            m_skipRows = skipRows;
            m_detectTypes = detectTypes;
            m_headerRowIndex = headerRowIndex;
        }

        @Override
        public void loadInternals(final StreamableOperatorInternals internals) {
            final var opInternals = (Internals) internals;
            m_columnNames = opInternals.m_columnNames;
            m_columnTypes = opInternals.m_columnTypes;
        }

        @Override
        public StreamableOperatorInternals saveInternals() {
            final var internals = new Internals();
            internals.m_columnNames = m_columnNames;
            internals.m_columnTypes = m_columnTypes;
            return internals;
        }

        @Override
        public void runIntermediate(final PortInput[] inputs, final ExecutionContext exec) throws Exception {
            final BufferedDataTable inTable = getInputTable(inputs);
            final var inSpec = inTable.getDataTableSpec();
            final long startOfData = m_skipRows ? (m_headerRowIndex + 1) : 0;
            final long safeSkip = Math.min(startOfData, m_headerRowIndex);
            final var cfg = tableReadConfig(m_headerRowIndex - safeSkip, 0);

            final Pair<DataTableSpec, Class<?>[]> result;
            try (final var cursor = inTable.cursor(TableFilter.filterRowsFromIndex(safeSkip));
                    final var tblRead = new ConvertingCursorRead<>(cursor, inTable.size() - safeSkip,
                            RowToColumnHeaderNodeModel::valueToString)) {
                result = computeNewSpec(m_detectTypes, cfg, inSpec, tblRead, exec.createSubExecutionContext(0.5));
            }

            m_columnNames = result.getFirst().getColumnNames();
            m_columnTypes = result.getSecond();
        }

        @Override
        public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
            final BufferedDataTable dataTable = getInputTable(inputs);
            final var inSpec = dataTable.getDataTableSpec();
            final var outSpec = assembleOutSpec(inSpec, m_columnNames, m_columnTypes);

            final RowOutput out1 = (RowOutput) outputs[1];
            for (final var row : renamingsTableRows(inSpec, outSpec)) {
                out1.push(row);
            }
            out1.close();

            // simple adapter from data row to RandomAccessible<String>
            final var rowRef = new AtomicReference<DataRow>();
            final var rowRA = new RandomAccessible<String>() {
                @Override
                public int size() {
                    return outSpec.getNumColumns();
                }

                @Override
                public String get(final int idx) {
                    return valueToString(rowRef.get().getCell(idx));
                }
            };

            final RowOutput out0 = (RowOutput) outputs[0];
            final var converter =
                    m_detectTypes ? createConverter(exec, m_columnTypes, new CSVTableReaderConfig()) : null;
            final long startRow = m_skipRows ? (m_headerRowIndex + 1) : 0;
            try (final var iter = dataTable.filter(TableFilter.filterRowsFromIndex(startRow)).iterator()) {
                for (long idx = startRow; iter.hasNext(); idx++) {
                    final DataRow row = iter.next();
                    if (idx == m_headerRowIndex) {
                        // skip header row
                        continue;
                    }

                    final DataRow outRow;
                    if (converter != null) {
                        rowRef.set(row);
                        outRow = converter.convert(row.getKey().getString(), rowRA);
                    } else {
                        outRow = row;
                    }
                    out0.push(outRow);
                }
            }
            out0.close();
        }

        /**
         * Gets the {@link BufferedDataTable} from the provided {@link PortInput port inputs}.
         *
         * @param inputs provided port inputs
         * @return buffered data table
         */
        private static BufferedDataTable getInputTable(final PortInput[] inputs) {
            // input port role is NONDISTRIBUTED_NONSTREAMABLE, so the input is guaranteed to be a PortObjectInput
            final var input = ((PortObjectInput) inputs[0]).getPortObject();
            if (!(input instanceof BufferedDataTable)) {
                // is this the right thing to do?
                throw new IllegalStateException("Unexpected port object type: " + input.getClass());
            }

            return (BufferedDataTable) input;
        }

        /**
         * Internals of an execution of the {@link RowToColumnHeaderNodeModel} node for streaming execution.
         */
        public static final class Internals extends StreamableOperatorInternals {

            /** New column names, {@code null} if they haven't been detected yet. */
            private String[] m_columnNames;

            /** Detected types, {@code null} if type detection is turned off or hasn't run yet. */
            private Class<?>[] m_columnTypes;

            /**
             * Checks whether these internals already contain the detected new table specification.
             *
             * @return {@code true} if new spec has already been detected, {@code false} otherwise
             */
            public boolean hasDetectedSpec() {
                return m_columnNames != null;
            }

            /**
             * Gets the extracted column headers from the internals.
             *
             * @return extracted column names, or {@code null} if they have not been extracted yet
             */
            public String[] getColumnNames() {
                return m_columnNames;
            }

            public Class<?>[] getColumnTypes() {
                return m_columnTypes;
            }

            @Override
            public void load(final DataInputStream input) throws IOException {
                m_columnNames = null;
                m_columnTypes = null;

                final var numNames = input.readInt();
                if (numNames < 0) {
                    // neither names nor types
                    return;
                }

                final var columnNames = new String[numNames];
                for (var i = 0; i < numNames; i++) {
                    columnNames[i] = input.readUTF();
                }
                m_columnNames = columnNames;

                final var numTypes = input.readInt();
                if (numTypes < 0) {
                    // no types
                    return;
                }

                final var columnTypes = new Class[numNames];
                for (var i = 0; i < numNames; i++) {
                    final String className = input.readUTF();
                    try {
                        columnTypes[i] = Class.forName(className);
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalStateException("Unexpected class name: " + className, e);
                    }
                }
                m_columnTypes = columnTypes;
            }

            @Override
            public void save(final DataOutputStream output) throws IOException {
                if (m_columnNames == null) {
                    output.writeInt(-1);
                    return;
                }
                output.writeInt(m_columnNames.length);
                for (final String str : m_columnNames) {
                    output.writeUTF(str);
                }

                if (m_columnTypes == null) {
                    output.writeInt(-1);
                    return;
                }
                output.writeInt(m_columnTypes.length);
                for (final Class<?> clazz : m_columnTypes) {
                    output.writeUTF(clazz.getName());
                }
            }
        }
    }

    /* ############################################ Blocking Execution ############################################## */

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        // we can't know the spec of the first outport because it is determined by the contents of the input
        return new DataTableSpec[]{ null, RENAMINGS_SPEC };
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {
        final BufferedDataTable inTable = inData[0];
        final var inSpec = inTable.getSpec();
        final long rows = inTable.size();
        final long numInRows = rows;

        final var skipRows = m_discardBefore.getBooleanValue();
        final var detectTypes = m_detectTypes.getBooleanValue();
        final var headerRowIndex = m_headerRowIndex.getIntValue();
        if (headerRowIndex < 0 || headerRowIndex >= numInRows) {
            final String rowsPlural = rows > 1 ? "s" : "";
            throw new IllegalArgumentException(String.format(
                "%d rows expected before header row, but table has only %d row%s.", headerRowIndex, rows, rowsPlural));
        }

        final long startOfData = skipRows ? (headerRowIndex + 1) : 0;
        final long safeSkip = Math.min(startOfData, headerRowIndex);
        final long numOutRows = numInRows - 1 - (skipRows ? headerRowIndex : 0);
        final var cfg = tableReadConfig(headerRowIndex - safeSkip, 0);

        final Pair<DataTableSpec, Class<?>[]> result;
        try (final var cursor = inTable.cursor(TableFilter.filterRowsFromIndex(safeSkip));
                final var tblRead = new ConvertingCursorRead<>(cursor, rows - safeSkip,
                        RowToColumnHeaderNodeModel::valueToString)) {
            result = computeNewSpec(detectTypes, cfg, inSpec, tblRead, exec.createSubExecutionContext(0.5));
        }

        final DataTableSpec outSpec = result.getFirst();
        final Class<?>[] guessedTypes = result.getSecond();

        final var renamingsBuilder = exec.createDataContainer(RENAMINGS_SPEC);
        for (final var row : renamingsTableRows(inSpec, outSpec)) {
            renamingsBuilder.addRowToTable(row);
        }
        renamingsBuilder.close();
        final BufferedDataTable renamings = renamingsBuilder.getTable();

        final var builder = exec.createDataContainer(outSpec);
        final var subExec = exec.createSubExecutionContext(0.5);
        subExec.setMessage("Producing output table");
        final var rowRanges = computeRowRanges(headerRowIndex, rows, skipRows);
        if (detectTypes) {
            convertTable(subExec, inTable, builder, rowRanges, guessedTypes, cfg.getReaderSpecificConfig());
        } else {
            long processed = 0;
            for (final var range : rowRanges) {
                // add progress/cancellations
                final var filter = TableFilter.filterRangeOfRows(range.getFirst(), range.getSecond());
                processed = copyRows(subExec, inTable, builder, processed, numOutRows, filter);
            }
        }
        builder.close();

        return new BufferedDataTable[]{ builder.getTable(), renamings };
    }

    /**
     * @param table
     * @param headerRowIndex
     * @param skipRows whether to skip the rows preceding the header
     * @return the ranges of rows that contain the table data. Could be <ul>
     * <li>0, if the header is in the last row and skip rows is on</li>
     * <li>1, if the header is in the first row or in the last row and skip rows is off</li>
     * <li>2, if the header is in the middle and skip rows is off</li></ul>
     */
    private static List<Pair<Long, Long>> computeRowRanges(final long headerRowIndex, final long numRows,
        final boolean skipRows) {
        final List<Pair<Long, Long>> rowRanges = new ArrayList<>();
        boolean headerIsFirst = headerRowIndex == 0;
        boolean headerIsLast = headerRowIndex == numRows - 1;
        if (!skipRows && !headerIsFirst) {
            // there are data rows before the header, process those first
            rowRanges.add(Pair.create(0L, headerRowIndex - 1));
        }

        if(!headerIsLast) {
            // process all data rows after the header
            rowRanges.add(Pair.create(headerRowIndex + 1, numRows - 1));
        }

        return rowRanges;
    }

    /**
     * Copies input rows straight to the output table.
     *
     * @param exec progress monitor
     * @param inTable input table
     * @param builder output table builder
     * @param processedBefore number of rows processed up to this point
     * @param numRows number of rows to process
     * @param filter table filter specifying the rows to be copied
     * @return number of rows processed
     * @throws CanceledExecutionException if the node was cancelled
     */
    private static long copyRows(final ExecutionMonitor exec, final BufferedDataTable inTable,
        final BufferedDataContainer builder, final long processedBefore, final long numRows, final TableFilter filter)
            throws CanceledExecutionException {
        long processed = processedBefore;
        for (final var row : inTable.filter(filter)) {
            builder.addRowToTable(row);
            processed++;
            progressTick(exec, processed, numRows);
        }
        return processed;
    }

    /**
     * Creates the converted output table by converting one row at a time.
     *
     * @param exec progress monitor
     * @param inTable input table
     * @param builder output table builder
     * @param rowRanges ranges of rows to convert
     * @param outTypes classes of the output types
     * @param cfg table read configuration
     * @throws Exception any exception
     */
    private static void convertTable(final ExecutionContext exec, final BufferedDataTable inTable,
            final BufferedDataContainer builder, final List<Pair<Long, Long>> rowRanges,
            final Class<?>[] outTypes, final CSVTableReaderConfig config) throws Exception {

        final var converter = createConverter(exec, outTypes, config);
        final long numRows = rowRanges.stream().mapToLong(p -> p.getSecond() - p.getFirst() + 1).sum();
        long processed = 0;
        for (final var range : rowRanges) {
            final var filter = TableFilter.filterRangeOfRows(range.getFirst(), range.getSecond());
            try (final var read = new ConvertingCursorRead<>(inTable.cursor(filter), range.getSecond(),
                    RowToColumnHeaderNodeModel::valueToString)) {

                for (ConvertingCursorRead<String>.WrappedRow row; (row = read.next()) != null;) {
                    builder.addRowToTable(converter.convert(row.getRowKey().toString(), row));
                    processed++;
                    progressTick(exec, processed, numRows);
                }
            }
        }
    }

    /**
     * Tick function that is called after every row that is processed.
     *
     * @param exec execution monitor for cancellation check and progress reporting
     * @param processed number of rows processed
     * @param numRows number of rows to process overall
     * @throws CanceledExecutionException if the execution has been canceled
     */
    private static void progressTick(final ExecutionMonitor exec, final long processed, final long numRows)
            throws CanceledExecutionException {
        exec.checkCanceled();
        if (processed % PROGRESS_UPDATE_INTERVAL == 1) { //NOSONAR processed is never negative
            exec.setProgress(1.0 * processed / numRows);
        }
    }

    /* ############################################### Common Logic ################################################# */

    /**
     * Assembles the new output spec.
     *
     * @param inSpec input spec to copy the column types from if type guessing is disabled
     * @param columnNames new column names
     * @param columnTypes new column type classes, or {@code null} if type guessing is disabled
     * @return table spec for outport 1
     */
    private static DataTableSpec assembleOutSpec(final DataTableSpec inSpec, final String[] columnNames,
        final Class<?>[] columnTypes) {
        final var builder = new DataTableSpecCreator();
        for (var i = 0; i < columnNames.length; i++) {
            final var inColSpec = inSpec.getColumnSpec(i);
            final var outType = columnTypes == null ? inColSpec.getType()
                : StringReadAdapterFactory.INSTANCE.getDefaultType(columnTypes[i]);
            builder.addColumns(new DataColumnSpecCreator(columnNames[i], outType).createSpec());
        }
        return builder.createSpec();
    }

    /**
     * Computes the output table spec by inspecting rows of the input table.
     *
     * @param detectTypes flag indicating whether or not column types should be detected
     * @param safeSkip number of rows that can be skipped at the start of the input table
     * @param config read configuration
     * @param inTable input table
     * @param exec execution context
     * @return output table spec and class objects of the detected column types
     * @throws IOException exception from inside the {@link Read} internals
     */
    private static Pair<DataTableSpec, Class<?>[]> computeNewSpec(final boolean detectTypes,
            final TableReadConfig<CSVTableReaderConfig> config, final DataTableSpec inSpec, final Read<String> read,
            final ExecutionContext exec) throws IOException {

        final var hierarchy = detectTypes ? CSVGuessableType.createHierarchy(config.getReaderSpecificConfig())
            : CSVGuessableType.stringOnlyHierarchy();
        final TableSpecGuesser<Object, Class<?>, String> guesser = new TableSpecGuesser<>(hierarchy, Object::toString);

        final TypedReaderTableSpec<Class<?>> guessedSpec;
        try (final var extractRead = new DefaultExtractColumnHeaderRead<>(read, config)) {
            guessedSpec = MultiTableUtils.assignNamesIfMissing(guesser.guessSpec(extractRead, config, exec, read));
        }

        // assemble output spec data
        final int numColumns = inSpec.getNumColumns();
        final var names = new String[numColumns];
        final var guessedTypes = detectTypes ? new Class[numColumns] : null;
        for (var i = 0; i < numColumns; i++) {
            final var guessedCol = guessedSpec.getColumnSpec(i);
            names[i] = guessedCol.getName().orElseThrow(); // we've created names above
            if (guessedTypes != null) {
                guessedTypes[i] = guessedCol.getType();
            }
        }
        return Pair.create(assembleOutSpec(inSpec, names, guessedTypes), guessedTypes);
    }

    /**
     * Creates the converter producing the output rows.
     *
     * @param exec execution context
     * @param outTypes detected output types
     * @param cfg table read configuration
     * @return row converter
     */
    private static RowConverter<DataRow, Exception> createConverter(final ExecutionContext exec,
            final Class<?>[] outTypes, final CSVTableReaderConfig config) {

        final var provider = new DefaultProductionPathProvider<>(StringReadAdapterFactory.INSTANCE);
        final List<RowConverter<DataCell, Exception>> converters = new ArrayList<>();
        for (var i = 0; i < outTypes.length; i++) {
            final var outType = outTypes[i];
            final ReadAdapterParams<ReadAdapter<Class<?>, String>, CSVTableReaderConfig> idx =
                    new ReadAdapterParams<>(i, config);
            final var prodPath = provider.getDefaultProductionPath(outType);
            @SuppressWarnings("unchecked")
            final var producerFactory = (CellValueProducerFactory<ReadAdapter<Class<?>, String>, Class<?>, Object,
                    ReadAdapterParams<ReadAdapter<Class<?>, String>, CSVTableReaderConfig>>)
                prodPath.getProducerFactory();
            final var valueProducer = producerFactory.create();
            final var cellConverter = prodPath.getConverterFactory().create(exec);
            final var readAdapter = StringReadAdapterFactory.INSTANCE.createReadAdapter();
            converters.add((key, row) -> {
                readAdapter.setSource(row);
                return cellConverter.convertUnsafe(valueProducer.produceCellValue(readAdapter, idx));
            });
        }

        // buffer is reused
        final var cellBuffer = new DataCell[outTypes.length];
        return (key, row) -> {
            for (var i = 0; i < cellBuffer.length; i++) {
                cellBuffer[i] = converters.get(i).convert(key, row);
            }
            return new DefaultRow(key, cellBuffer.clone());
        };
    }

    /**
     * Creates the rows of the output table recording the renamings and type conversions applied by this node.
     *
     * @param inSpec input spec
     * @param outSpec output spec
     */
    private static DataRow[] renamingsTableRows(final DataTableSpec inSpec, final DataTableSpec outSpec) {
        return IntStream.range(0, inSpec.getNumColumns()).mapToObj(i -> {
            final var inColSpec = inSpec.getColumnSpec(i);
            final var outColSpec = outSpec.getColumnSpec(i);
            return new DefaultRow("Column " + i, inColSpec.getName(), outColSpec.getName(),
                outColSpec.getType().toString());
        }).toArray(DataRow[]::new);
    }

    /**
     * Creates a reader configuration for the type guesser.
     *
     * @param headerRowIndex index of the row containing the headers
     * @return configuration
     */
    private static TableReadConfig<CSVTableReaderConfig> tableReadConfig(final long headerRowIndex,
            final long skipRows) {
        final var csvConfig = new CSVTableReaderConfig();
        final DefaultTableReadConfig<CSVTableReaderConfig> cfg = new DefaultTableReadConfig<>(csvConfig);
        cfg.setUseColumnHeaderIdx(true);
        cfg.setColumnHeaderIdx(headerRowIndex);
        cfg.setLimitRowsForSpec(false);
        if (skipRows > 0) {
            cfg.setSkipRows(true);
            cfg.setNumRowsToSkip(skipRows);
        }
        return cfg;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_headerRowIndex.saveSettingsTo(settings);
        m_discardBefore.saveSettingsTo(settings);
        m_detectTypes.saveSettingsTo(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_headerRowIndex.loadSettingsFrom(settings);
        m_discardBefore.loadSettingsFrom(settings);
        m_detectTypes.loadSettingsFrom(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_headerRowIndex.validateSettings(settings);
        m_discardBefore.validateSettings(settings);
        m_detectTypes.validateSettings(settings);
    }

    @Override
    protected void reset() {
        // no internals
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
            return super.getOutHiLiteHandler(0);
        case 1:
            return m_hiliteHandler;
        default:
            throw new IndexOutOfBoundsException("Invalid port: " + outIndex);
        }
    }

    /** @return settings model for the header row index */
    static SettingsModelInteger createHeaderRowIndex() {
        return new SettingsModelInteger(HEADER_ROW_INDEX, 0) {

            @Override
            protected void validateValue(final int value) throws InvalidSettingsException {
                CheckUtils.checkSetting(value >= 0, "The number of rows before the header must be non-negative.");
            }
        };
    }

    /** @return settings model for the flag indicating whether rows before the header should be skipped */
    static SettingsModelBoolean createDiscardBefore() {
        return new SettingsModelBoolean(DISCARD_BEFORE, true);
    }

    /** @return settings model for the flag indicating whether data types should be detected */
    static SettingsModelBoolean createDetectTypes() {
        return new SettingsModelBoolean(DETECT_TYPES, true);
    }

    private static String valueToString(final DataValue value) {
        final var str = value.toString();
        return str.isEmpty() ? null : str;
    }
}
