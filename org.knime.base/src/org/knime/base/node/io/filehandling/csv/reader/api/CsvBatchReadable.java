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
 *   Mar 26, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.knime.core.columnar.ColumnarSchema;
import org.knime.core.columnar.batch.SequentialBatchReadable;
import org.knime.core.columnar.filter.ColumnSelection;

import com.univocity.parsers.csv.CsvParserSettings;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class CsvBatchReadable implements SequentialBatchReadable {

    // TODO make FSPath once in file handling plugin
    private final Path m_path;

    private final ColumnarSchema m_schema;

    private final CsvParserSettings m_settings;

    private final int m_batchSize;

    CsvBatchReadable(final Path path, final CsvParserSettings settings, final ColumnarSchema schema,
        final int batchSize) {
        m_path = path;
        m_schema = schema;
        m_settings = settings;
        m_batchSize = batchSize;
    }

    // TODO support for Theobald Reader. InputStreamProvider?

    @Override
    public void close() throws IOException {
        // nothing to close (right now)
    }

    @SuppressWarnings("resource") // the caller has to close the batch reader once they are done
    @Override
    public CsvBatchReader createReader(final ColumnSelection selection) {
        try {
            final BufferedReader reader = Files.newBufferedReader(m_path);// NOSONAR, managed by CSVBatchReader
            return new CsvBatchReader(reader, m_settings, selection, m_batchSize);
        } catch (IOException ex) {
            throw new IllegalStateException("An IOException occurred when opening the reader.", ex);
        }
    }

    @Override
    public ColumnarSchema getSchema() {
        return m_schema;
    }

}
