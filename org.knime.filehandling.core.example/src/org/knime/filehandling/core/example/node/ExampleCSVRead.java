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
 *   28 Jun 2021 Moditha Hewasinghage: created
 */
package org.knime.filehandling.core.example.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalLong;

import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleUtils;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.util.BomEncodingUtils;
import org.knime.filehandling.core.util.CompressionAwareCountingInputStream;

/**
 * Class for the example csv reader which implements {@link Read}. We read as
 * {@link String} tokens.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 * 
 */
final class ExampleCSVRead implements Read<String> {

    private static final String COLUMN_DELIMITER = ",";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExampleCSVRead.class);

    private final BufferedReader m_reader;

    private final CompressionAwareCountingInputStream m_compressionAwareStream;

    private final long m_size;

    /**
     * Constructor.
     *
     * @param path
     *            the {@link Path} to the file
     * @param config
     *            the {@link TableReadConfig} of the node
     * @throws IOException
     */
    ExampleCSVRead(final FSPath path, final TableReadConfig<ExampleCSVReaderConfig> config) throws IOException { //NOSONAR not using config yet

        m_size = Files.size(path);

        m_compressionAwareStream = new CompressionAwareCountingInputStream(path);

        final Charset charset = Charset.defaultCharset();
        m_reader = BomEncodingUtils.createBufferedReader(m_compressionAwareStream, charset);
    }

    /**
     * Reads and returns the next row
     */
    @Override
    public RandomAccessible<String> next() throws IOException {
        final String line = m_reader.readLine();
        if (line == null) {
            // no more lines to read
            return null;
        } else {
            return RandomAccessibleUtils.createFromArray(line.split(COLUMN_DELIMITER));
        }
    }

    @Override
    public void close() throws IOException {
        try {
            m_reader.close();
        } catch (IOException e) {
            LOGGER.error("Something went wrong while closing the BufferedReader. "
                    + "For further details please have a look into the log.", e);
        }
        m_compressionAwareStream.close();
    }

    @Override
    public OptionalLong getMaxProgress() {
        return OptionalLong.of(m_size);
    }

    @Override
    public long getProgress() {
        return m_compressionAwareStream.getCount();
    }
}
