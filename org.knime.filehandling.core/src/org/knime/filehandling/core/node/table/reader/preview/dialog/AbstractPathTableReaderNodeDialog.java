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
 *   Dec 4, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;

/**
 * Convenience layer for reader nodes that read from {@link Path}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig} used by the underlying reader
 * @param <T> the type used to identify external types
 */
public abstract class AbstractPathTableReaderNodeDialog<C extends ReaderSpecificConfig<C>, T>
    extends AbstractTableReaderNodeDialog<Path, C, T> {

    /**
     * Constructor.
     *
     * @param readFactory
     * @param productionPathProvider
     * @param allowsMultipleFiles
     */
    protected AbstractPathTableReaderNodeDialog(final MultiTableReadFactory<Path, C, T> readFactory,
        final ProductionPathProvider<T> productionPathProvider, final boolean allowsMultipleFiles) {
        this(readFactory, productionPathProvider, allowsMultipleFiles, false);
    }

    /**
     * Constructor.
     *
     * @param readFactory
     * @param productionPathProvider
     * @param allowsMultipleFiles
     * @param isDragNDrop
     */
    protected AbstractPathTableReaderNodeDialog(final MultiTableReadFactory<Path, C, T> readFactory,
        final ProductionPathProvider<T> productionPathProvider, final boolean allowsMultipleFiles, final boolean isDragNDrop) {
        super(readFactory, productionPathProvider, allowsMultipleFiles, isDragNDrop);
    }

    @SuppressWarnings("resource") // the ReadPathAccessor is managed by the adapter
    @Override
    protected final GenericItemAccessor<Path> createItemAccessor() {
        return new ReadPathAccessorAdapter(createReadPathAccessor());
    }

    /**
     * Creates a <b>new</b> {@link ReadPathAccessor} that corresponds to the current file selection.</br>
     * It is important to create a new {@link ReadPathAccessor} for every call, otherwise {@link IOException} can occur
     * in the preview.
     *
     * @return the {@link ReadPathAccessor} corresponding to the current file selection
     */
    protected abstract ReadPathAccessor createReadPathAccessor();

    /**
     * Adapter that maps a {@link ReadPathAccessor} to a {@code GenericItemAccessor<Path>}.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private static class ReadPathAccessorAdapter implements GenericItemAccessor<Path> {

        private final ReadPathAccessor m_pathAccessor;

        ReadPathAccessorAdapter(final ReadPathAccessor pathAccessor) {
            m_pathAccessor = pathAccessor;
        }

        @Override
        public void close() throws IOException {
            m_pathAccessor.close();
        }

        @Override
        public List<Path> getItems(final Consumer<StatusMessage> statusMessageConsumer)
            throws IOException, InvalidSettingsException {
            return m_pathAccessor.getPaths(statusMessageConsumer);
        }

        @Override
        public Path getRootItem(final Consumer<StatusMessage> statusMessageConsumer)
            throws IOException, InvalidSettingsException {
            return m_pathAccessor.getRootPath(statusMessageConsumer);
        }

    }

}
