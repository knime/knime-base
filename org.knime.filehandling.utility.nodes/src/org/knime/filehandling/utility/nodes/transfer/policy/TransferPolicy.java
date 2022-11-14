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
 *   Mar 15, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer.policy;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.utility.nodes.utils.FileStatus;

/**
 * Policy how to proceed when output file exists (overwrite, fail, ignore).
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public enum TransferPolicy implements ButtonGroupEnumInterface, TransferFunction {

        /** Overwrite existing file. */
        OVERWRITE(TransferPolicy::overwrite, FileOverwritePolicy.OVERWRITE.getText()),

        /** Overwrite existing file. */
        OVERWRITE_IF_NEWER(TransferPolicy::overwriteIfNewer, "overwrite if newer"),

        /** Ignore existing files i.e. don't overwrite or append but also don't fail. */
        IGNORE(TransferPolicy::ignore, FileOverwritePolicy.IGNORE.getText()),

        /** Fail during execution. Neither overwrite nor append. */
        FAIL(TransferPolicy::fail, FileOverwritePolicy.FAIL.getText());

    private final TransferFunction m_transferFunction;

    private final String m_description;

    private TransferPolicy(final TransferFunction transferFunction, final String description) {
        m_transferFunction = transferFunction;
        m_description = description;
    }

    @Override
    public FileStatus apply(final FSPath t, final FSPath s) throws IOException {
        return m_transferFunction.apply(t, s);
    }

    @Override
    public String getText() {
        return m_description;
    }

    @Override
    public String getActionCommand() {
        return name();
    }

    @Override
    public String getToolTip() {
        return m_description;
    }

    @Override
    public boolean isDefault() {
        return this == getDefault();
    }

    /**
     * Returns the default {@link TransferPolicy}.
     *
     * @return the default {@link TransferPolicy}
     */
    public static TransferPolicy getDefault() {
        return TransferPolicy.FAIL;
    }

    private static FileStatus overwrite(final FSPath src, final FSPath dest) throws IOException {
        if (isSameFileOrFolder(src, dest)) {
            return FileStatus.UNMODIFIED;
        }
        final FileStatus fileStatus = FSFiles.exists(dest) ? FileStatus.OVERWRITTEN : FileStatus.CREATED;
        FSFiles.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        return fileStatus;
    }

    private static boolean isSameFileOrFolder(final FSPath src, final FSPath dest) { // check with other FS
        FSLocation srcLoc = ((FSPath)src.toAbsolutePath().normalize()).toFSLocation();
        FSLocation desttLoc = ((FSPath)dest.toAbsolutePath().normalize()).toFSLocation();
        return srcLoc.equals(desttLoc);
    }

    private static FileStatus fail(final FSPath src, final FSPath dest) throws IOException {
        if (FSFiles.exists(dest)) {
            throw new FileAlreadyExistsException(dest.toString());
        }
        FSFiles.copy(src, dest);
        return FileStatus.CREATED;
    }

    private static FileStatus ignore(final FSPath src, final FSPath dest) throws IOException {
        if (FSFiles.exists(dest)) {
            return FileStatus.UNMODIFIED;
        } else {
            FSFiles.copy(src, dest);
            return FileStatus.CREATED;
        }
    }

    private static FileStatus overwriteIfNewer(final FSPath src, final FSPath dest) throws IOException {
        final boolean exists = FSFiles.exists(dest);
        final FileStatus fileStatus;
        if (!exists) {
            fileStatus = FileStatus.CREATED;
        } else {
            if (isNewer(src, dest)) {
                fileStatus = FileStatus.OVERWRITTEN;
            } else {
                fileStatus = FileStatus.UNMODIFIED;
            }
        }
        if (fileStatus != FileStatus.UNMODIFIED) {
            FSFiles.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return fileStatus;
    }

    private static boolean isNewer(final FSPath src, final FSPath dest) throws IOException {
        return getLastModifiedTime(src).compareTo(getLastModifiedTime(dest)) > 0;
    }

    private static FileTime getLastModifiedTime(final FSPath path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime();
    }

}
