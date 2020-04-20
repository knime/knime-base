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
 *   2 Apr 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

/**
 * This static utility class provides methods to create readers that handle the BOM for UTF encoded files.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class BomEncodingUtils {

    /**
     * This is a static utility class
     */
    private BomEncodingUtils() {

    }

    /**
     * Creates a {@link BufferedReader} for the given path that omits the BOM for UTF encoded files. The caller of
     * this method is responsible for closing the reader and therefore the underlying stream.
     *
     * @param path the path to the file to open
     * @param charset the used charset
     * @param options options specifying how the file is opened
     *
     * @return a {@code BufferedReader}
     *
     * @throws IllegalArgumentException if an invalid combination of options is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the
     *             {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the
     *             file.
     */
    public static BufferedReader createBufferedReader(final Path path, final Charset charset,
        final OpenOption... options) throws IOException {
        return new BufferedReader(createReader(path, charset, options));
    }

    /**
     * Creates a {@link InputStreamReader} for the given path that omits the BOM for UTF encoded files. The caller of
     * this method is responsible for closing the reader and therefore the underlying stream.
     *
     * @param path the path to the file to open
     * @param charset the used {@link Charset}
     * @param options options specifying how the file is opened
     *
     * @return an {@code InputStreamReader}
     *
     * @throws IllegalArgumentException if an invalid combination of options is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is installed, the
     *             {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read access to the
     *             file.
     */
    public static InputStreamReader createReader(final Path path, final Charset charset, final OpenOption... options)
        throws IOException {
        return createReader(Files.newInputStream(path, options), charset);
    }

    /**
     * Creates a {@link InputStreamReader} for the given stream that omits the BOM for UTF encoded files. The caller of
     * this method is responsible for closing the reader and therefore the underlying stream.
     *
     * @param input the {@link InputStream} to read from
     * @param charset the used {@link Charset}
     * @return an {@code InputStreamReader}
     *
     * @throws IllegalArgumentException if an invalid combination of options is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public static InputStreamReader createReader(final InputStream input, final Charset charset) {
        return new InputStreamReader(getBomDecodedInputStream(input, charset), charset);
    }

    /**
     * Creates a {@link BufferedReader} for the given path that omits the BOM for UTF encoded files. The caller of
     * this method is responsible for closing the reader and therefore the underlying stream.
     *
     * @param input the {@link InputStream} to read from
     * @param charset the used {@link Charset}
     *
     * @return a {@code BufferedReader}
     *
     * @throws IllegalArgumentException if an invalid combination of options is specified
     * @throws UnsupportedOperationException if an unsupported option is specified
     */
    public static BufferedReader createBufferedReader(final InputStream input, final Charset charset) {
        return new BufferedReader(createReader(input, charset));
    }

    private static InputStream getBomDecodedInputStream(final InputStream input, final Charset charset) {
        final ByteOrderMark bom;
        // note that StandardCharsets.UTF_16 automatically detects the BOM itself
        if (charset.name().equals(ByteOrderMark.UTF_8.getCharsetName())) {
            bom = ByteOrderMark.UTF_8;
        } else if (charset.name().equals(ByteOrderMark.UTF_16LE.getCharsetName())) {
            bom = ByteOrderMark.UTF_16LE;
        } else if (charset.name().equals(ByteOrderMark.UTF_16BE.getCharsetName())) {
            bom = ByteOrderMark.UTF_16BE;
        } else if (charset.name().equals(ByteOrderMark.UTF_32LE.getCharsetName())) {
            bom = ByteOrderMark.UTF_32LE;
        } else if (charset.name().equals(ByteOrderMark.UTF_32BE.getCharsetName())) {
            bom = ByteOrderMark.UTF_32BE;
        } else {
            return input;
        }
        return new BOMInputStream(input, bom);

    }
}
