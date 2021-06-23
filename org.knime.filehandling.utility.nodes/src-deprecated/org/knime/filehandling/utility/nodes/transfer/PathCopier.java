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
package org.knime.filehandling.utility.nodes.transfer;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCell;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.utility.nodes.transfer.policy.TransferFunction;
import org.knime.filehandling.utility.nodes.utils.FileStatus;

/**
 * Copies files and folders from a source path to a destination path and creates the respective rows for an output
 * table.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @deprecated since 4.3.3
 */
@Deprecated
final class PathCopier {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PathCopier.class);

    private static final String ERROR_MESSAGE =
        "Something went wrong during the copying / moving process. See log for further details.";

    private static final int NUMBER_OF_COLS = 4;

    private static final int SOURCE_COL_IDX = 0;

    private static final int DESTINATION_COL_IDX = 1;

    private static final int IS_DIR_COL_IDX = 2;

    private static final int STATUS_COL_IDX = 3;

    private final Consumer<DataRow> m_rowConsumer;

    private final TransferFilesNodeConfig m_config;

    private final SimpleFSLocationCellFactory m_sourceFSLocationCellFactory;

    private final SimpleFSLocationCellFactory m_destinationFSLocationCellFactory;

    private final FileOverwritePolicy m_fileOverWritePolicy;

    private final TransferFunction m_copyFunction;

    /**
     * Constructor.
     *
     * @param rowConsumer the {@link Consumer} of {@link DataRow}
     * @param config the {@link TransferFilesNodeConfig}
     * @param numberOfCols the number of columns in the output spec
     */
    PathCopier(final Consumer<DataRow> rowConsumer, final TransferFilesNodeConfig config) {
        m_rowConsumer = rowConsumer;
        m_config = config;
        m_sourceFSLocationCellFactory =
            new SimpleFSLocationCellFactory(m_config.getSourceFileChooserModel().getLocation());
        m_destinationFSLocationCellFactory =
            new SimpleFSLocationCellFactory(m_config.getDestinationFileChooserModel().getLocation());
        m_fileOverWritePolicy = m_config.getDestinationFileChooserModel().getFileOverwritePolicy();
        m_copyFunction = getCopyFunction(m_fileOverWritePolicy);
    }

    /**
     * Returns a {@link TransferFunction} based on the passed {@link FileOverwritePolicy}.
     *
     * @param fileOverWritePolicy the overwrite policy
     * @return the {@link TransferFunction}
     */
    private static TransferFunction getCopyFunction(final FileOverwritePolicy fileOverWritePolicy) {
        return (s, d) -> {//NOSONAR
            final boolean exists = FSFiles.exists(d);
            if (fileOverWritePolicy == FileOverwritePolicy.OVERWRITE) {
                return getOverwriteCopyFunction(s, d, exists);
            } else if (fileOverWritePolicy == FileOverwritePolicy.FAIL) {
                return getFailCopyFunction(s, d, exists);
            } else if (fileOverWritePolicy == FileOverwritePolicy.IGNORE) {
                return getIgnoreCopyFunction(s, d, exists);
            } else {
                throw new IllegalArgumentException(
                    String.format("Unsupported FileOverwritePolicy '%s' encountered.", fileOverWritePolicy));
            }
        };
    }

    /**
     * The {@link TransferFunction} for the {@link FileOverwritePolicy#OVERWRITE}.
     *
     * @param source the source {@link Path}
     * @param dest the destination {@link Path}
     * @return the {@link FileStatus}
     * @throws IOException
     */
    private static FileStatus getIgnoreCopyFunction(final Path source, final Path dest, final boolean exists)
        throws IOException {
        if (exists) {
            return FileStatus.UNMODIFIED;
        } else {
            Files.copy(source, dest);
            return FileStatus.CREATED;
        }
    }

    /**
     * The {@link TransferFunction} for the {@link FileOverwritePolicy#FAIL}.
     *
     * @param source the source {@link Path}
     * @param dest the destination {@link Path}
     * @return the {@link FileStatus}
     * @throws IOException
     */
    private static FileStatus getFailCopyFunction(final Path source, final Path dest, final boolean exists)
        throws IOException {
        if (exists) {
            throw new FileAlreadyExistsException(dest.toString());
        }
        Files.copy(source, dest);
        return FileStatus.CREATED;
    }

    /**
     * The {@link TransferFunction} for the {@link FileOverwritePolicy#OVERWRITE}.
     *
     * @param source the source {@link Path}
     * @param dest the destination {@link Path}
     * @return the {@link FileStatus}
     * @throws IOException
     */
    private static FileStatus getOverwriteCopyFunction(final Path source, final Path dest, final boolean exists)
        throws IOException {
        final FileStatus fileStatus = exists ? FileStatus.OVERWRITTEN : FileStatus.CREATED;
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return fileStatus;
    }

    /**
     * Creates the directory of the passed path if it do not exist already.
     *
     * @param path the path to the directory which needs to be created
     * @throws IOException
     * @return exists returns if the path already existed or not
     */
    static boolean createDirectories(final Path path) throws IOException {
        final boolean exists = FSFiles.exists(path);
        if (!exists) {
            try {
                FSFiles.createDirectories(path);
            } catch (IOException e) {
                LOGGER.warn(ERROR_MESSAGE, e);
                throw e;
            }
        }
        return exists;
    }

    /**
     * Copies a file or folder from a source to a specified destination.
     *
     * @param sourcePath the source {@link Path}
     * @param destinationPath the destination {@link Path}
     * @param rowIdx the current row index
     * @throws IOException
     */
    void copyPath(final FSPath sourcePath, final FSPath destinationPath, final long rowIdx) throws IOException {
        if (FSFiles.isDirectory(sourcePath)) {
            copyDirectories(sourcePath, destinationPath, rowIdx);
        } else {
            copyFiles(sourcePath, destinationPath, rowIdx);
        }
    }

    /**
     * This method is used in the folder mode to create an entry in the output table with the source and target folder
     * {@link Path}.
     *
     * @param sourcePath the source {@link Path}
     * @param destinationPath the destination {@link Path}
     * @param rowIdx the current row index
     * @param destDirExists indicates whether the destination directory already exists or not.
     */
    void handleSourceTargetPath(final Path sourcePath, final Path destinationPath, final long rowIdx,
        final boolean destDirExists) {
        pushRow(rowIdx, m_sourceFSLocationCellFactory.createCell(sourcePath.toString()),
            m_destinationFSLocationCellFactory.createCell(destinationPath.toString()), true,
            destDirExists ? FileStatus.ALREADY_EXISTED.getText() : FileStatus.CREATED.getText());
    }

    /**
     * Copies a directory.
     *
     * @param sourcePath the source {@link Path}
     * @param destinationPath the destination {@link Path}
     * @param rowIdx the current row index
     * @throws IOException
     */
    private void copyDirectories(final Path sourcePath, final Path destinationPath, final long rowIdx)
        throws IOException {
        final boolean existed = createDirectories(destinationPath);
        final FileStatus fileStatus = existed ? FileStatus.ALREADY_EXISTED : FileStatus.CREATED;

        pushRow(rowIdx, m_sourceFSLocationCellFactory.createCell(sourcePath.toString()),
            m_destinationFSLocationCellFactory.createCell(destinationPath.toString()), true, fileStatus.getText());
    }

    /**
     * Copies a file.
     *
     * @param sourcePath the source {@link Path}
     * @param destinationPath the destination {@link Path}
     * @param rowIdx the current row index
     * @throws IOException
     */
    private void copyFiles(final FSPath sourcePath, final FSPath destinationPath, final long rowIdx) throws IOException {
        createDirectories(destinationPath.getParent());
        try {
            final FileStatus fileStatus = m_copyFunction.apply(sourcePath, destinationPath);
            pushRow(rowIdx, m_sourceFSLocationCellFactory.createCell(sourcePath.toString()),
                m_destinationFSLocationCellFactory.createCell(destinationPath.toString()), false, fileStatus.getText());
        } catch (FileAlreadyExistsException e) {
            if (m_fileOverWritePolicy == FileOverwritePolicy.FAIL) {
                throw new IOException(
                    "Output file '" + destinationPath + "' exists and must not be overwritten due to user settings.",
                    e);
            }
            //should not occur since we always check if a file exists already
            LOGGER.warn("Unexpected FileAlreadyExistsException has been thrown. See log for further details.", e);
        } catch (IOException e) {
            LOGGER.warn(ERROR_MESSAGE, e);
            throw e;
        }
    }

    /**
     * Creates a new row and adds it via the {@link Consumer} of {@link DataRow}.
     *
     * @param rowIdx the current row index
     * @param sourcePathCell the {@link SimpleFSLocationCell} for the source path
     * @param destinationPathCell the {@link SimpleFSLocationCell} for the destination path
     * @param isDirectory the flag whether the path points to a folder or not
     * @param deleted the flag whether the file has been deleted or not
     */
    private void pushRow(final long rowIdx, final DataCell sourcePathCell, final DataCell destinationPathCell,
        final boolean isDirectory, final String status) {
        final DataCell[] cells = new DataCell[NUMBER_OF_COLS];
        cells[SOURCE_COL_IDX] = sourcePathCell;
        cells[DESTINATION_COL_IDX] = destinationPathCell;
        cells[IS_DIR_COL_IDX] = BooleanCellFactory.create(isDirectory);
        cells[STATUS_COL_IDX] = StringCellFactory.create(status);

        m_rowConsumer.accept(new DefaultRow(RowKey.createRowKey(rowIdx), cells));
    }

    /**
     * Creates the output spec of the output of the node.
     *
     * @param config the {@link TransferFilesNodeConfig}
     * @return the output {@link DataTableSpec}
     */
    static DataTableSpec createOutputSpec(final TransferFilesNodeConfig config) {
        final ArrayList<DataColumnSpec> columnSpecs = new ArrayList<>();
        columnSpecs.add(createMetaColumnSpec(config.getSourceFileChooserModel().getLocation(), "Source Path"));
        columnSpecs
            .add(createMetaColumnSpec(config.getDestinationFileChooserModel().getLocation(), "Destination Path"));
        columnSpecs.add(new DataColumnSpecCreator("Directory", BooleanCell.TYPE).createSpec());
        columnSpecs.add(new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec());
        return new DataTableSpec(columnSpecs.toArray(new DataColumnSpec[0]));
    }

    /**
     * Creates a {@link DataColumnSpec} for the source or destination path which includes the meta data.
     *
     * @param locationsSpec the location spec specifying the file system
     * @param columnName the name of the column
     * @return the {@link DataColumnSpec}
     */
    private static DataColumnSpec createMetaColumnSpec(final FSLocation locationsSpec, final String columnName) {
        final FSLocationValueMetaData metaData = new FSLocationValueMetaData(locationsSpec.getFileSystemCategory(),
            locationsSpec.getFileSystemSpecifier().orElse(null));
        final DataColumnSpecCreator fsLocationSpec =
            new DataColumnSpecCreator(columnName, SimpleFSLocationCellFactory.TYPE);
        fsLocationSpec.addMetaData(metaData, true);
        return fsLocationSpec.createSpec();
    }
}
