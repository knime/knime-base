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
 *   Sep 14, 2020 (lars.schweikardt): created
 */
package org.knime.base.node.io.filehandling.util.copymovefiles;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.FSLocationCell;
import org.knime.filehandling.core.data.location.cell.FSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.SettingsModelFileSystem;

/**
 * Copies files from a source path to a destination path and creates the respective rows for an output table.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class FileCopier {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(FileCopier.class);

    private static final int NUMBER_OF_COLS = 5;

    private static final int SOURCE_COL_IDX = 0;

    private static final int DESTINATION_COL_IDX = 1;

    private static final int COPIED_COL_IDX = 2;

    private static final int DELETED_COL_IDX = 3;

    private static final int STATUS_COL_IDX = 4;

    private final Consumer<DataRow> m_rowConsumer;

    private final CopyMoveFilesNodeConfig m_config;

    private final FSLocationCellFactory m_sourceFSLocationCellFactory;

    private final FSLocationCellFactory m_destinationFSLocationCellFactory;

    private final FileOverwritePolicy m_fileOverWritePolicy;

    private final boolean m_isDeleteMode;

    private final OpenOption[] m_openOptions;

    /**
     * Constructor.
     *
     * @param rowConsumer the {@link Consumer} of {@link DataRow}
     * @param config the {@link CopyMoveFilesNodeConfig}
     * @param fileStoreFactory the {@link FileStoreFactory}
     * @param numberOfCols the number of columns in the output spec
     */
    FileCopier(final Consumer<DataRow> rowConsumer, final CopyMoveFilesNodeConfig config,
        final FileStoreFactory fileStoreFactory) {
        m_rowConsumer = rowConsumer;
        m_config = config;
        m_isDeleteMode = config.getDeleteSourceFilesModel().getBooleanValue();
        m_sourceFSLocationCellFactory =
            new FSLocationCellFactory(fileStoreFactory, m_config.getSourceFileChooserModel().getLocation());
        m_destinationFSLocationCellFactory =
            new FSLocationCellFactory(fileStoreFactory, m_config.getDestinationFileChooserModel().getLocation());
        m_fileOverWritePolicy = m_config.getDestinationFileChooserModel().getFileOverwritePolicy();
        m_openOptions = m_fileOverWritePolicy.getOpenOptions();
    }

    /**
     * Copies the files from the source path to the destination path.
     *
     * @param sourcePath the source path of the file
     * @param destinationPath the destination path of the file
     * @param rowIdx the current row index
     * @throws IOException
     */
    void copy(final Path sourcePath, final Path destinationPath, final long rowIdx) throws IOException {

        //Create directories if they do not exist
        createOutputDirectories(destinationPath.getParent());
        final FSLocationCell sourcePathCell = m_sourceFSLocationCellFactory.createCell(sourcePath.toString());
        final FSLocationCell destinationPathCell =
            m_destinationFSLocationCellFactory.createCell(destinationPath.toString());
        final boolean destinationPathExists = FSFiles.exists(destinationPath);

        // If file exists and FileOverwritePolicy.IGNORE is chosen, then the file will not be copied
        if (destinationPathExists && m_fileOverWritePolicy == FileOverwritePolicy.IGNORE) {
            pushRow(rowIdx, sourcePathCell, destinationPathCell, false, false, "unmodified");
            return;
        }

        try (final OutputStream outputStream = Files.newOutputStream(destinationPath, m_openOptions)) {

            Files.copy(sourcePath, outputStream);

            //Use of Files.deleteIfExists to mimic the behavior of Files.move
            if (m_isDeleteMode) {
                Files.deleteIfExists(sourcePath);
            }

            pushRow(rowIdx, sourcePathCell, destinationPathCell, true, m_isDeleteMode,
                (m_fileOverWritePolicy == FileOverwritePolicy.OVERWRITE && destinationPathExists) ? "overwritten"
                    : "created");
        } catch (FileAlreadyExistsException e) {
            if (m_fileOverWritePolicy == FileOverwritePolicy.FAIL) {
                throw new IOException(
                    "Output file '" + destinationPath + "' exists and must not be overwritten due to user settings.",
                    e);
            }
            //should not occur since we always check if a file exists already
            LOGGER.warn("Unexpected FileAlreadyExistsException has been thrown. See log for further details.", e);
        } catch (IOException e) {
            LOGGER.warn("Something went wrong during the copying / moving process. See log for further details.", e);
        }
    }

    /**
     * Creates the directory of the passed path if it do not exist already.
     *
     * @param path the path to the directory which needs to be created
     * @throws IOException
     */
    static void createOutputDirectories(final Path path) throws IOException {
        if (!FSFiles.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Creates a new row and adds it via the {@link Consumer} of {@link DataRow}.
     *
     * @param rowIdx the current row index
     * @param sourcePathCell the {@link FSLocationCell} for the source path
     * @param destinationPathCell the {@link FSLocationCell} for the destination path
     * @param copied the flag whether the file has been copied or not
     * @param deleted the flag whether the file has been deleted or not
     */
    private void pushRow(final long rowIdx, final FSLocationCell sourcePathCell, final DataCell destinationPathCell,
        final boolean copied, final boolean deleted, final String status) {
        final DataCell[] cells = new DataCell[NUMBER_OF_COLS];
        cells[SOURCE_COL_IDX] = sourcePathCell;
        cells[DESTINATION_COL_IDX] = destinationPathCell;
        cells[COPIED_COL_IDX] = BooleanCellFactory.create(copied);
        cells[DELETED_COL_IDX] = BooleanCellFactory.create(deleted);
        cells[STATUS_COL_IDX] = StringCellFactory.create(status);

        m_rowConsumer.accept(new DefaultRow(RowKey.createRowKey(rowIdx), cells));
    }

    /**
     * Creates the output spec of the output of the node.
     *
     * @param config the {@link CopyMoveFilesNodeConfig}
     * @return the output {@link DataTableSpec}
     */
    static DataTableSpec createOutputSpec(final CopyMoveFilesNodeConfig config) {
        final ArrayList<DataColumnSpec> columnSpecs = new ArrayList<>();
        columnSpecs.add(createMetaColumnSpec(config.getSourceFileChooserModel().getLocation(), "Source Path"));
        columnSpecs.add(createMetaColumnSpec(config.getDestinationFileChooserModel().getLocation(), "Destination Path"));
        columnSpecs.add(new DataColumnSpecCreator("Copied", BooleanCell.TYPE).createSpec());
        columnSpecs.add(new DataColumnSpecCreator("Source Deleted", BooleanCell.TYPE).createSpec());
        columnSpecs.add(new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec());
        return new DataTableSpec(columnSpecs.toArray(new DataColumnSpec[0]));
    }

    /**
     * Creates a {@link DataColumnSpec} for the source or destination path which includes the meta data.
     *
     * @param settingsModelFS the {@link SettingsModelFileSystem}
     * @param columnName the name of the column
     * @return the {@link DataColumnSpec}
     */
    private static DataColumnSpec createMetaColumnSpec(final FSLocation locationsSpec, final String columnName) {
        final FSLocationValueMetaData metaData = new FSLocationValueMetaData(locationsSpec.getFileSystemCategory(),
            locationsSpec.getFileSystemSpecifier().orElse(null));
        final DataColumnSpecCreator fsLocationSpec = new DataColumnSpecCreator(columnName, FSLocationCellFactory.TYPE);
        fsLocationSpec.addMetaData(metaData, true);
        return fsLocationSpec.createSpec();
    }
}
