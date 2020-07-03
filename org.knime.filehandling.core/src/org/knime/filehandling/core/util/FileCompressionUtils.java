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
 *   May 12, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSFiles;

/**
 * A static utility class for crating a compression aware {@link InputStream} based on file extension.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public final class FileCompressionUtils {
    /**
     * This is a static utility class
     */
    private FileCompressionUtils() {

    }

    /**
     * A convenience method that returns either a {@link GZIPInputStream} or a regular {@link InputStream} depending on
     * the file path extension. {@link GZIPInputStream} is returned if the file path ends with '.gz' but not '.tar.gz'.
     * If the provided file path ends with '.tar.gz', an {@code IllegalArgumentException} will be thrown. The caller of
     * this method is responsible for closing the underlying stream.
     *
     * @param path the path to the file to open
     * @param options options specifying how the file is opened
     * @return either a {@link GZIPInputStream} or a regular {@link InputStream}
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if an invalid combination of options is specified
     */
    public static InputStream createInputStream(final Path path, final OpenOption... options) throws IOException {
        // path.endsWith() method has a different meaning
        if (path.toString().endsWith(".gz")) {
            CheckUtils.checkArgument(!path.toString().endsWith(".tar.gz"),
                "Cannot create a GZIPInputStream directly from a tar archive (%s).", path.toString());
            InputStream inStream = FSFiles.newInputStream(path, options);
            try {
                return new GZIPInputStream(inStream);
            } catch (ZipException ex) {
                // Unable to create a GZIPInputStream. We will close the already opened and probably utilized stream
                // and fallback to a regular InputStream (i.e., the return statement at the end of the method).
                inStream.close();
            }
        }
        return FSFiles.newInputStream(path, options);
    }
}
