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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.cell.MultiSimpleFSLocationCellFactory;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferEntry;
import org.knime.filehandling.utility.nodes.transfer.iterators.TransferPair;
import org.knime.filehandling.utility.nodes.transfer.policy.TransferPolicy;
import org.knime.filehandling.utility.nodes.utils.FileStatus;

/**
 * Copies files and folders from a source path to a destination path and creates the respective rows for an output
 * table.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class PathCopier2 {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PathCopier2.class);

    private static final String ERROR_MESSAGE =
        "Something went wrong during the copying / moving process. See log for further details.";

    private static final int NUMBER_OF_DEFAULT_COLS = 4;

    private static final int SOURCE_COL_IDX = 0;

    private static final int DESTINATION_COL_IDX = 1;

    private static final int IS_DIR_COL_IDX = 2;

    private static final int STATUS_COL_IDX = 3;

    private static final int DELETE_COL_IDX = 4;

    private final MultiSimpleFSLocationCellFactory m_sourceFSLocationCellFactory;

    private final MultiSimpleFSLocationCellFactory m_destinationFSLocationCellFactory;

    private final TransferPolicy m_transferPolicy;

    private final boolean m_verbose;

    private final boolean m_delete;

    private final boolean m_failOnUnsuccessfulDeletion;

    private final boolean m_failIfSrcDoesNotExist;

    private final int m_failIfSrcDoesNotExistIdx;

    PathCopier2(final TransferPolicy transferPolicy, final boolean verbose, final boolean delete,
        final boolean failOnDeletion, final boolean failIfSrcDoesNotExist) {
        m_sourceFSLocationCellFactory = new MultiSimpleFSLocationCellFactory();
        m_destinationFSLocationCellFactory = new MultiSimpleFSLocationCellFactory();
        m_transferPolicy = transferPolicy;
        m_verbose = verbose;
        m_delete = delete;
        m_failOnUnsuccessfulDeletion = failOnDeletion;
        m_failIfSrcDoesNotExist = failIfSrcDoesNotExist;
        m_failIfSrcDoesNotExistIdx = addDeleteColumn() ? (DELETE_COL_IDX + 1) : DELETE_COL_IDX;
    }

    private boolean addDeleteColumn() {
        return m_delete && !m_failOnUnsuccessfulDeletion;
    }

    DataCell[][] transfer(final ExecutionContext exec, final TransferEntry entry)
        throws IOException, CanceledExecutionException {
        final boolean exists = FSFiles.exists(entry.getSource());
        final DataCell[][] rows;
        if (exists) {
            rows = copyMove(exec, entry);
        } else {
            rows = handleMissingSrc(exec, entry);
        }
        return rows;
    }

    private DataCell[][] handleMissingSrc(final ExecutionContext exec, final TransferEntry entry)
        throws FileNotFoundException {
        if (m_failIfSrcDoesNotExist) {
            throw new FileNotFoundException(
                String.format("The specified file/folder '%s' does not exist", entry.getSource()));
        } else {
            exec.setProgress(1);
            return new DataCell[][]{createSrcDoesNotExistRow(entry.getSource())};
        }
    }

    private DataCell[][] copyMove(final ExecutionContext exec, final TransferEntry entry)
        throws IOException, CanceledExecutionException {
        var result = attemptAtomicCopyMove(exec, entry.getSrcDestPair());
        DataCell[][] rows = result.isPresent() ? result.get() : performRecursiveCopyDelete(exec, entry);

        // add fail if src does not exists col
        if (!m_failIfSrcDoesNotExist) {
            addFailIfSrcDoesNotExistsCol(rows);
        }
        return rows;
    }

    private Optional<DataCell[][]> attemptAtomicCopyMove(final ExecutionContext exec, final TransferPair pair)
        throws IOException {
        var src = pair.getSource();
        var dst = pair.getDestination();

        //It is to make the atomic copy/move behavior consistent with the recursive one.
        //Could be removed once "Create missing filders" option is fixed.
        Files.createDirectories(dst.getParent());

        var dstIsNotEmptyDir = false;
        if (Files.isDirectory(dst)) {
            try (var stream = Files.newDirectoryStream(dst)) {
                dstIsNotEmptyDir = stream.iterator().hasNext();
            }
        }

        if ((Files.isDirectory(src) && m_verbose)
            || (dstIsNotEmptyDir && m_transferPolicy != TransferPolicy.OVERWRITE)) {
            return Optional.empty();
        }

        validatePair(src, dst);

        var rows = Files.isDirectory(src) ? transferDirAtomically(src, dst) : transferFileAtomically(src, dst);

        exec.setProgress(1);
        return Optional.ofNullable(rows);
    }

    private DataCell[][] transferDirAtomically(final FSPath src, final FSPath dst) throws IOException {
        FileStatus status = Files.exists(dst) ? FileStatus.UNMODIFIED : FileStatus.CREATED;

        try {
            if (m_delete) {
                FSFiles.move(src, dst, StandardCopyOption.ATOMIC_MOVE);
            } else {
                FSFiles.copyAtomically(src, dst);
            }

            var row = buildRow(src, dst, status);
            if (m_delete) {
                row = appendDeleteCol(row);
            }

            return new DataCell[][]{row};
        } catch (UnsupportedOperationException | AtomicMoveNotSupportedException e) {//NOSONAR
            return null;//NOSONAR
        }
    }

    private DataCell[][] transferFileAtomically(final FSPath src, final FSPath dst) throws IOException {
        if (!m_delete) {
            //atomic copy is only for directories
            return null;//NOSONAR
        }

        try {
            var status = transferFile(src, dst, true);
            var row = appendDeleteCol(buildRow(src, dst, status));

            return (new DataCell[][]{row});
        } catch (UnsupportedOperationException | AtomicMoveNotSupportedException e) {//NOSONAR
            return null;//NOSONAR
        }
    }

    private static DataCell[] appendDeleteCol(final DataCell[] row) {
        final DataCell existsFlag = BooleanCellFactory.create(true);
        return ArrayUtils.addAll(row, existsFlag);
    }

    private DataCell[][] performRecursiveCopyDelete(final ExecutionContext exec, final TransferEntry entry)
        throws IOException, CanceledExecutionException {
        final List<TransferPair> paths = entry.getPathsToCopy();
        var rows = new DataCell[!m_verbose ? 1 : (1 + paths.size())][];
        final ListIterator<TransferPair> listIterator = paths.listIterator();
        ExecutionContext subExec = exec.createSubExecutionContext(m_delete ? 0.5 : 1);
        final int entriesToProcess = paths.size() + 1;
        // copy
        copy(subExec, rows, 0, entry.getSrcDestPair(), true, entriesToProcess);
        copy(subExec, rows, 1, listIterator, entriesToProcess);

        // delete it if necessary
        if (m_delete) {
            subExec = exec.createSubExecutionContext(0.5);
            delete(subExec, rows, listIterator, entriesToProcess);
            delete(subExec, rows, 0, entry.getSrcDestPair().getSource(), true, entriesToProcess);
        }
        return rows;
    }

    private void copy(final ExecutionContext exec, final DataCell[][] rows, int idx,
        final ListIterator<TransferPair> listIterator, final double entriesToProcess)
        throws CanceledExecutionException, IOException {
        while (listIterator.hasNext()) {
            exec.checkCanceled();
            idx = copy(exec, rows, idx, listIterator.next(), m_verbose, entriesToProcess);
        }
    }

    private int copy(final ExecutionContext exec, final DataCell[][] rows, final int idx, final TransferPair p,
        final boolean verbose, final double entriesToProcess) throws IOException, CanceledExecutionException {
        exec.checkCanceled();
        exec.setProgress((idx + 1) / entriesToProcess, () -> String.format("Copying '%s'", p.getSource()));
        final DataCell[] row = copyPath(p.getSource(), p.getDestination());
        if (verbose) {
            rows[idx] = row;
        }
        return idx + 1;
    }

    private void delete(final ExecutionContext exec, final DataCell[][] rows,
        final ListIterator<TransferPair> listIterator, final int entriesToProcess)
        throws CanceledExecutionException, IOException {
        int idx = entriesToProcess - 1;
        while (listIterator.hasPrevious()) {
            final FSPath source = listIterator.previous().getSource();
            idx = delete(exec, rows, idx, source, m_verbose, entriesToProcess);
        }
    }

    private int delete(final ExecutionContext exec, final DataCell[][] rows, final int idx, final FSPath source,
        final boolean verbose, final double entriesToProcess) throws IOException, CanceledExecutionException {
        exec.checkCanceled();
        exec.setProgress((entriesToProcess - idx) / entriesToProcess, () -> String.format("Deleting '%s'", source));
        final DataCell delete = delete(source);
        if (verbose && addDeleteColumn()) {
            rows[idx] = ArrayUtils.addAll(rows[idx], delete);
        }
        return idx - 1;
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

        final FileStatus status;
        if (FSFiles.isDirectory(src)) {
            status = createDirectory(dest);
        } else {
            status = transferFile(src, dest, false);
        }

        return buildRow(src, dest, status);
    }

    private DataCell[] buildRow(final FSPath src, final FSPath dst, final FileStatus status) {
        var cells = new DataCell[NUMBER_OF_DEFAULT_COLS];
        cells[SOURCE_COL_IDX] = m_sourceFSLocationCellFactory.createCell(src.toFSLocation());
        cells[DESTINATION_COL_IDX] = m_destinationFSLocationCellFactory.createCell(dst.toFSLocation());
        cells[IS_DIR_COL_IDX] = BooleanCellFactory.create(Files.isDirectory(dst));
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
    private FileStatus transferFile(final FSPath src, final FSPath dest, final boolean move) throws IOException {
        if (dest.getParent() != null) {
            createDirectories(dest.getParent());
        }
        try {
            return m_transferPolicy.apply(src, dest, move);
        } catch (FileAlreadyExistsException e) {
            if (m_transferPolicy == TransferPolicy.FAIL) {
                throw new IOException(
                    "Output file '" + dest + "' exists and must not be overwritten due to user settings.", e);
            }
            //should not occur since we always check if a file exists already
            LOGGER.debug("Unexpected FileAlreadyExistsException has been thrown. See log for further details.", e);
            throw (e);
        } catch (IOException e) {
            LOGGER.debug(ERROR_MESSAGE, e);
            throw ExceptionUtil.wrapIOException(e);
        }
    }

    /**
     * Creates the directory of the passed path if it do not exist already.
     *
     * @param path the path to the directory which needs to be created
     * @throws IOException
     * @return exists returns if the path already existed or not
     */
    private static boolean createDirectories(final Path path) throws IOException {
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

    private static void addFailIfSrcDoesNotExistsCol(final DataCell[][] rows) {
        final DataCell existsFlag = BooleanCellFactory.create(true);
        for (int i = 0; i < rows.length; i++) {
            rows[i] = ArrayUtils.addAll(rows[i], existsFlag);
        }
    }

    /**
     * Creates all data cells required to create a row for a non-existent source path.
     *
     * @param src the non-existent source path
     * @return the default row for non-existent source paths
     */
    private DataCell[] createSrcDoesNotExistRow(final FSPath src) {
        final DataCell[] cells = new DataCell[m_failIfSrcDoesNotExistIdx + 1];
        Arrays.fill(cells, DataType.getMissingCell());
        cells[SOURCE_COL_IDX] = m_sourceFSLocationCellFactory.createCell(src.toFSLocation());
        if (addDeleteColumn()) {
            cells[DELETE_COL_IDX] = BooleanCellFactory.create(false);
        }
        cells[m_failIfSrcDoesNotExistIdx] = BooleanCellFactory.create(false);
        return cells;
    }

}