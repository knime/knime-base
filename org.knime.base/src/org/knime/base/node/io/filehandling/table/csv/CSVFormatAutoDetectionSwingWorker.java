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
 *   23 Apr 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.table.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.defaultnodesettings.FileChooserHelper;
import org.knime.filehandling.core.util.BomEncodingUtils;
import org.knime.filehandling.core.util.FileCompressionUtils;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * Swing worker to automatically detect column delimiter, row delimiter, quote char and quote escape char of a csv-file.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
final class CSVFormatAutoDetectionSwingWorker extends SwingWorkerWithContext<CsvFormat, Double> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CSVFormatAutoDetectionSwingWorker.class);

    private final Optional<FSConnection> m_fsConnection;

    private final CSVTableReaderNodeDialog m_dialog;

    public CSVFormatAutoDetectionSwingWorker(final Optional<FSConnection> fsConnection,
        final CSVTableReaderNodeDialog dialog) {
        m_dialog = dialog;
        m_fsConnection = fsConnection;
    }

    @Override
    protected CsvFormat doInBackgroundWithContext() throws IOException, InvalidSettingsException, InterruptedException {
        final FileChooserHelper fch = new FileChooserHelper(m_fsConnection, m_dialog.getFileChooserSettingsModel());
        final List<Path> paths = fch.getPaths();
        final CsvParser csvParser =
            new CsvParser(getCsvParserSettings(m_dialog.getCommentStart(), m_dialog.getBufferSize()));

        if (!paths.isEmpty()) {
            // use only first file for parsing
            try (final InputStream in = FileCompressionUtils.createInputStream(paths.get(0));
                    final BufferedReader reader =
                        BomEncodingUtils.createBufferedReader(in, m_dialog.getSelectedCharset())) {
                if (m_dialog.getSkipLines()) {
                    skipLines(reader, m_dialog.getNumLinesToSkip());
                }

                csvParser.beginParsing(reader);
                csvParser.stopParsing();
            }
            return csvParser.getDetectedFormat();
        } else {
            throw new InvalidSettingsException("Please specify a file before running format autodetection");
        }
    }

    @Override
    protected void doneWithContext() {
        m_dialog.resetUIafterAutodetection();

        try {
            final CsvFormat detectedFormat = get();
            m_dialog.updateAutodetectionFields(detectedFormat);
            m_dialog.setStatus("Successfully autodetected!", null, SharedIcons.SUCCESS.get());

        } catch (final ExecutionException e) {
            m_dialog.setStatus("Error during autodetection! See log file for details.", "See log file for details",
                SharedIcons.ERROR.get());
            LOGGER.warn(e.getMessage(), e);
        } catch (InterruptedException | CancellationException ex) {
            // ignore
        }
    }

    private static CsvParserSettings getCsvParserSettings(final String comment, final int inputBufferSize) {
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

    private static void skipLines(final BufferedReader reader, final long n) throws IOException {
        for (int i = 0; i < n; i++) {
            reader.readLine();
        }
    }
}
