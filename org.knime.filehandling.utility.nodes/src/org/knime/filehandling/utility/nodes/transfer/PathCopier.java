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
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.cell.MultiSimpleFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferEntry;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferPair;
import org.knime.filehandling.utility.nodes.utils.FileStatus;

/**
 * Copies files and folders from a source path to a destination path and creates the respective rows for an output
 * table.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class PathCopier {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PathCopier.class);

    private static final String ERROR_MESSAGE =
        "Something went wrong during the copying / moving process. See log for further details.";

    private static final int NUMBER_OF_COLS = 4;

    private static final int SOURCE_COL_IDX = 0;

    private static final int DESTINATION_COL_IDX = 1;

    private static final int IS_DIR_COL_IDX = 2;

    private static final int STATUS_COL_IDX = 3;

    private final MultiSimpleFSLocationCellFactory m_sourceFSLocationCellFactory;

    private final MultiSimpleFSLocationCellFactory m_destinationFSLocationCellFactory;

    private final FileOverwritePolicy m_fileOverwritePolicy;

    private final TransferFunction m_copyFunction;

    private final boolean m_verbose;

    private final boolean m_delete;

    private final boolean m_failOnUnsuccessfulDeletion;

    PathCopier(final FileOverwritePolicy overwritePolicy, final boolean verbose, final boolean delete,
        final boolean failOnDeletion) {
        m_sourceFSLocationCellFactory = new MultiSimpleFSLocationCellFactory();
        m_destinationFSLocationCellFactory = new MultiSimpleFSLocationCellFactory();
        m_fileOverwritePolicy = overwritePolicy;
        m_copyFunction = getCopyFunction(m_fileOverwritePolicy);
        m_verbose = verbose;
        m_delete = delete;
        m_failOnUnsuccessfulDeletion = failOnDeletion;
    }

    DataCell[][] copyDelete(final ExecutionContext exec, final TransferEntry entry)
        throws IOException, CanceledExecutionException {
        final List<TransferPair> paths = entry.getPathsToCopy();
        final DataCell[][] rows = new DataCell[!m_verbose ? 1 : (1 + paths.size())][];
        final ListIterator<TransferPair> listIterator = paths.listIterator();

        // copy
        copy(exec, rows, 0, entry.getSrcDestPair(), true);
        copy(exec, rows, 1, listIterator);

        // delete it if necessary
        if (m_delete) {
            delete(exec, rows, listIterator);
            delete(exec, rows, 0, entry.getSrcDestPair().getSource(), true);
        }
        return rows;
    }

    private void copy(final ExecutionContext exec, final DataCell[][] rows, int idx,
        final ListIterator<TransferPair> listIterator) throws CanceledExecutionException, IOException {
        while (listIterator.hasNext()) {
            exec.checkCanceled();
            idx = copy(exec, rows, idx, listIterator.next(), m_verbose);
        }
    }

    private int copy(final ExecutionContext exec, final DataCell[][] rows, int idx, final TransferPair p,
        final boolean verbose) throws IOException, CanceledExecutionException {
        exec.checkCanceled();
        final DataCell[] row = copyPath(p.getSource(), p.getDestination());
        if (verbose) {
            rows[idx] = row;
            idx++;
        }
        return idx;
    }

    private void delete(final ExecutionContext exec, final DataCell[][] rows,
        final ListIterator<TransferPair> listIterator) throws CanceledExecutionException, IOException {
        int idx = rows.length - 1;
        while (listIterator.hasPrevious()) {
            final FSPath source = listIterator.previous().getSource();
            idx = delete(exec, rows, idx, source, m_verbose);
        }
    }

    private int delete(final ExecutionContext exec, final DataCell[][] rows, int idx, final FSPath source,
        final boolean verbose) throws IOException, CanceledExecutionException {
        exec.checkCanceled();
        final DataCell delete = delete(source);
        if (verbose) {
            rows[idx] = ArrayUtils.addAll(rows[idx], delete);
            --idx;
        }
        return idx;
    }

    private DataCell delete(final FSPath source) throws IOException {
        try {
            Files.delete(source);
            return BooleanCellFactory.create(true);
        } catch (IOException e) {
            if (!m_failOnUnsuccessfulDeletion) {
                LOGGER.debug(String.format("Unable to delete file %s", source.toString()), e);
                return BooleanCellFactory.create(false);
            } else {
                throw e;
            }
        }
    }

    /**
     * Copies a file or folder from a source to a specified destination.
     *
     * @param src the source {@link FSPath}
     * @param dest the destination {@link FSPath}
     * @param rowIdx the current row index
     * @throws IOException - If something went wrong while copying the file or creating the folder
     */
    private DataCell[] copyPath(final FSPath src, final FSPath dest) throws IOException {
        validatePair(src, dest);

        final DataCell[] cells = new DataCell[NUMBER_OF_COLS];
        cells[SOURCE_COL_IDX] = m_sourceFSLocationCellFactory.createCell(src.toFSLocation());
        cells[DESTINATION_COL_IDX] = m_destinationFSLocationCellFactory.createCell(dest.toFSLocation());
        final boolean isDirectory = FSFiles.isDirectory(src);
        cells[IS_DIR_COL_IDX] = BooleanCellFactory.create(isDirectory);

        final FileStatus status;
        if (isDirectory) {
            status = createDirectory(dest);
        } else {
            status = copyFile(src, dest);
        }
        cells[STATUS_COL_IDX] = StringCellFactory.create(status.getText());

        return cells;
    }

    /**
     * Make sure that if the destination exist that both source and destination are either files or folders.
     *
     * @param src the source {@link FSPath}
     * @param dest the destination {@link FSPath}
     * @throws IOException - If something went wrong during validation
     */
    private static void validatePair(final FSPath src, final FSPath dest) throws IOException {
        if (FSFiles.exists(dest)) {
            final boolean srcIsDir = FSFiles.isDirectory(src);
            final boolean destIsDir = FSFiles.isDirectory(dest);
            if (srcIsDir && !destIsDir) {
                throw new IOException(
                    String.format("Unable to replace the non-folder '%s' by a folder '%s'", dest, src));
            } else if (!srcIsDir && destIsDir) {
                throw new IOException(
                    String.format("Unable to replace the folder '%s' by a non-folder '%s'", dest, src));
            }
        }
    }

    /**
     * Copies a directory.
     *
     * @param dest the destination {@link Path}
     * @param rowIdx the current row index
     * @throws IOException
     */
    private static FileStatus createDirectory(final FSPath dest) throws IOException {
        final boolean existed = createDirectories(dest);
        return existed ? FileStatus.ALREADY_EXISTED : FileStatus.CREATED;
    }

    /**
     * Copies a file.
     *
     * @param src the source {@link Path}
     * @param dest the destination {@link Path}
     * @param rowIdx the current row index
     * @throws IOException
     */
    private FileStatus copyFile(final FSPath src, final FSPath dest) throws IOException {
        createDirectories(dest.getParent());
        try {
            return m_copyFunction.apply(src, dest);
        } catch (FileAlreadyExistsException e) {
            if (m_fileOverwritePolicy == FileOverwritePolicy.FAIL) {
                throw new IOException(
                    "Output file '" + dest + "' exists and must not be overwritten due to user settings.", e);
            }
            //should not occur since we always check if a file exists already
            LOGGER.debug("Unexpected FileAlreadyExistsException has been thrown. See log for further details.", e);
            throw (e);
        } catch (IOException e) {
            LOGGER.debug(ERROR_MESSAGE, e);
            throw e;
        }
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
     * Returns a {@link TransferFunction} based on the passed {@link FileOverwritePolicy}.
     *
     * @param fileOverWritePolicy the overwrite policy
     * @return the {@link TransferFunction}
     */
    private static TransferFunction getCopyFunction(final FileOverwritePolicy fileOverwritePolicy) {
        if (fileOverwritePolicy == FileOverwritePolicy.OVERWRITE) {
            return getOverwriteCopyFunction();
        } else if (fileOverwritePolicy == FileOverwritePolicy.FAIL) {
            return getFailCopyFunction();
        } else if (fileOverwritePolicy == FileOverwritePolicy.IGNORE) {
            return getIgnoreCopyFunction();
        } else {
            throw new IllegalArgumentException(
                String.format("Unsupported FileOverwritePolicy '%s' encountered.", fileOverwritePolicy));
        }
    }

    /**
     * Returns the {@link TransferFunction} for the {@link FileOverwritePolicy#IGNORE}.
     *
     * @return the {@link TransferFunction} for the {@link FileOverwritePolicy#IGNORE}
     */
    private static TransferFunction getIgnoreCopyFunction() {
        return new TransferFunction() {

            @Override
            public FileStatus apply(final Path src, final Path dest) throws IOException {
                if (FSFiles.exists(dest)) {
                    return FileStatus.UNMODIFIED;
                } else {
                    Files.copy(src, dest);
                    return FileStatus.CREATED;
                }
            }
        };
    }

    /**
     * Returns the {@link TransferFunction} for the {@link FileOverwritePolicy#FAIL}.
     *
     * @return the {@link TransferFunction} for the {@link FileOverwritePolicy#FAIL}
     */
    private static TransferFunction getFailCopyFunction() {
        return new TransferFunction() {

            @Override
            public FileStatus apply(final Path src, final Path dest) throws IOException {
                if (FSFiles.exists(dest)) {
                    throw new FileAlreadyExistsException(dest.toString());
                }
                Files.copy(src, dest);
                return FileStatus.CREATED;
            }
        };
    }

    /**
     * Returns the {@link TransferFunction} for the {@link FileOverwritePolicy#OVERWRITE}.
     *
     * @return the {@link TransferFunction} for the {@link FileOverwritePolicy#OVERWRITE}
     */
    private static TransferFunction getOverwriteCopyFunction() {
        return new TransferFunction() {

            @Override
            public FileStatus apply(final Path src, final Path dest) throws IOException {
                final FileStatus fileStatus = FSFiles.exists(dest) ? FileStatus.OVERWRITTEN : FileStatus.CREATED;
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                return fileStatus;
            }
        };

    }
}