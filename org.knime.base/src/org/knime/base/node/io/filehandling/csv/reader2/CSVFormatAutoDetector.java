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
package org.knime.base.node.io.filehandling.csv.reader2;

import static org.knime.base.node.io.filehandling.csv.reader.CSVFormatAutoDetectionUtil.getCsvParserSettings;
import static org.knime.base.node.io.filehandling.csv.reader.CSVFormatAutoDetectionUtil.skipLines;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.knime.base.node.io.filehandling.csv.reader.CSVFormatAutoDetectionUtil.FullyBufferedReader;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.filehandling.core.util.BomEncodingUtils;
import org.knime.filehandling.core.util.FileCompressionUtils;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;

/**
 * Provides a {@link #detectFormat} method for auto-guessing the format from the implementation of its abstract methods.
 *
 * <p>
 * Originally taken from
 * {@link org.knime.base.node.io.filehandling.csv.reader.CSVFormatAutoDetectionSwingWorker#doInBackgroundWithContext
 * CSVFormatAutoDetectionSwingWorker#doInBackgroundWithContext} /
 * {@link org.knime.base.node.io.filehandling.csv.reader.CSVFormatAutoDetectionSwingWorker#detectFormat
 * CSVFormatAutoDetectionSwingWorker#detectFormat}
 * </p>
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
abstract class CSVFormatAutoDetector {

    protected CsvFormat detectFormat() throws IOException, InvalidSettingsException {

        try (final FileChooserPathAccessor accessor = new FileChooserPathAccessor(getFileChooser())) {
            final List<Path> paths = accessor.getPaths(s -> {});
            final CsvParser csvParser = new CsvParser(getCsvParserSettings(getCommentStart(), getBufferSize()));

            if (!paths.isEmpty()) {
                return detectFormat(paths, csvParser);
            } else {
                throw new InvalidSettingsException("Please specify a file before running format autodetection");
            }
        }
    }

    private CsvFormat detectFormat(final List<Path> paths, final CsvParser csvParser)
        throws IOException, InvalidSettingsException {
        // use only first file for parsing
        try (final InputStream in = FileCompressionUtils.createInputStream(paths.get(0));
                final BufferedReader reader = BomEncodingUtils.createBufferedReader(in, getSelectedCharset())) {
            if (isSkipLines()) {
                skipLines(reader, getNumLinesToSkip());
            }
            // Fixes a bug where the univocity library does not fill the buffer used for auto-guessing correctly
            try (final FullyBufferedReader fullyBufferedReader = new FullyBufferedReader(reader)) {
                csvParser.beginParsing(fullyBufferedReader);
                csvParser.stopParsing();
            }
        }
        return csvParser.getDetectedFormat();
    }

    protected abstract long getNumLinesToSkip();

    protected abstract boolean isSkipLines();

    protected abstract Charset getSelectedCharset() throws InvalidSettingsException;

    protected abstract int getBufferSize();

    protected abstract String getCommentStart();

    protected abstract FileChooser getFileChooser();

}
