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
 *   Mar 26, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 *
 * @author Paul Bärnreuther
 */
public final class CSVFormatAutoDetectionUtil {

    private CSVFormatAutoDetectionUtil() {
        // UTILITY
    }

    /**
     * @param comment
     * @param inputBufferSize
     * @return
     */
    public static CsvParserSettings getCsvParserSettings(final String comment, final int inputBufferSize) {
        final CsvFormat defaultFormat = new CsvFormat();
        final char charComment = !comment.isEmpty() ? comment.charAt(0) : '\0';
        defaultFormat.setComment(charComment);

        final CsvParserSettings settings = new CsvParserSettings();
        settings.setInputBufferSize(inputBufferSize);
        settings.setReadInputOnSeparateThread(false);
        settings.setFormat(defaultFormat);
        settings.detectFormatAutomatically();

        return settings;
    }

    /**
     * @param reader
     * @param n
     * @throws IOException
     */
    public static void skipLines(final BufferedReader reader, final long n) throws IOException {
        for (int i = 0; i < n; i++) {
            reader.readLine();
        }
    }

    /**
     *
     * @author Paul Bärnreuther
     */
    public static class FullyBufferedReader extends BufferedReader {

        /**
         * @param in
         */
        public FullyBufferedReader(final Reader in) {
            super(in);
        }

        /**
         * Ensures that the buffer gets completely filled even if {@link Reader#ready()} evaluates to {@code false}.
         * <br/>
         * This fixes a bug in the univocity lib that occurs when auto guessing the csv's format.
         *
         * @param cbuf Destination buffer
         * @param off Offset at which to start storing characters
         * @param len Maximum number of characters to read
         * @return The number of characters read, or -1 if the end of the stream has been reached
         *
         */
        @Override
        public int read(final char[] cbuf, final int off, final int len) throws IOException {
            int remaining = len;
            int n = super.read(cbuf, off, len);
            if (n <= 0) {
                return n;
            }
            remaining -= n;
            int curOff = off + n;
            while ((n = super.read(cbuf, curOff, remaining)) > 0) {
                curOff += n;
                remaining -= n;
            }
            return curOff - off;
        }
    }

}
