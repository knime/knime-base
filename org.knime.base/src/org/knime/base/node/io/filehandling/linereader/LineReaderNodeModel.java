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
 *   14.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.linereader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class LineReaderNodeModel extends NodeModel {

    private final LineReaderConfig m_config = new LineReaderConfig();

    /**
     *
     */
    public LineReaderNodeModel() {
        super(new PortType[] {FileSystemPortObject.TYPE_OPTIONAL}, new PortType[] {BufferedDataTable.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        //FIXME
        return new PortObjectSpec[]{null};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
        throws Exception {
        Optional<FSConnection> fs = FileSystemPortObject.getFileSystemConnection(inData, 0);
        final List<Path> pathList = m_config.getPaths(fs);

        if (pathList.isEmpty()) {
            throw new InvalidSettingsException("No files selected");
        }

        final DataTableSpec spec = createOutputSpec(pathList);
        final BufferedDataContainer container = exec.createDataContainer(spec);
        try {
            boolean skipHeader = m_config.getReadColHeader();
            final Iterator<Path> paths = pathList.iterator();
            while (paths.hasNext()) {

                exec.checkCanceled();

                final Path path = paths.next();
                final String rowPrefix = m_config.getRowPrefix();
                final int limit = m_config.getLimitRowCount();

                final RowBuilder lineConsumer = new RowBuilder(rowPrefix, container);

                if (m_config.getLimitLines()) {

                    readLimited(limit - container.size(), path, lineConsumer, skipHeader);

                } else {
                    readAllLines(path, lineConsumer, skipHeader);
                }

                //read the first file, no more lines to skip
                skipHeader = false;
            }

        } finally {
            container.close();
        }

        return new BufferedDataTable[]{container.getTable()};

    }

    private static class RowBuilder implements Consumer<String> {

        private final String m_rowPrefix;

        private final BufferedDataContainer m_container;

        /**
         * @param rowPrefix
         * @param container
         */
        public RowBuilder(final String rowPrefix, final BufferedDataContainer container) {
            m_rowPrefix = rowPrefix;
            m_container = container;
        }

        @Override
        public void accept(final String line) {

            final RowKey key = new RowKey(m_rowPrefix + (m_container.size()));
            final DefaultRow row = new DefaultRow(key, new StringCell(line));
            m_container.addRowToTable(row);
        }

    }

    /**
     * @param l
     * @param path
     * @throws IOException
     */
    private void readLimited(final long l, final Path path, final RowBuilder rowbuilder, final boolean skipFirst)
        throws IOException {
        try (Stream<String> lineStream = Files.lines(path)) {
            lineStream.filter(this::empytFilter).skip(skipFirst ? 1 : 0).limit(l).filter(this::regExMatch)
                .forEachOrdered(rowbuilder);
        }
    }

    private void readAllLines(final Path path, final RowBuilder rowbuilder, final boolean skipFirst)
        throws IOException {
        try (Stream<String> lineStream = Files.lines(path)) {
            lineStream.filter(this::empytFilter).skip(skipFirst ? 1 : 0).filter(this::regExMatch)
                .forEachOrdered(rowbuilder);
        }
    }

    private boolean regExMatch(final String s) {
        return !m_config.getUseRegex() || s.matches(m_config.getRegex());
    }

    private boolean empytFilter(final String s) {
        return !m_config.getSkipEmptyLines() || !s.trim().isEmpty();
    }

    private DataTableSpec createOutputSpec(final List<Path> pathList) throws IOException {
        final String tableName = getTableName(pathList);
        final Path path = pathList.get(0);
        try (Stream<String> currentLines = Files.lines(path)) {
            String colName;

            if (m_config.getReadColHeader()) {

                final Optional<String> optColName =
                    currentLines.filter(s -> (!m_config.getSkipEmptyLines() || !s.trim().isEmpty())).findFirst();

                if (!optColName.isPresent() || optColName.get().trim().isEmpty()) {
                    // if top line or all lines are blank in file use a default non-empty string
                    colName = "<empty>";
                } else {
                    colName = optColName.get();
                }
            } else {
                colName =
                    CheckUtils.checkNotNull(m_config.getColumnHeader(), "column header in config must not be null");
            }

            final DataColumnSpecCreator creator = new DataColumnSpecCreator(colName, StringCell.TYPE);

            return new DataTableSpec(tableName, creator.createSpec());
        }
    }

    /**
     * @param pathes
     * @return
     */
    private static String getTableName(final List<Path> pathes) {
        Path namePath = pathes.get(0);

        if (pathes.size() > 1) {
            namePath = namePath.getParent();
        }
        String tableName;
        if(namePath == null || namePath.getFileName() == null) {
            tableName = "LineReader output";
        } else {
            tableName = namePath.getFileName().toString();
        }

        return tableName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // none

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // none

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveConfiguration(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadConfiguration(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

}
