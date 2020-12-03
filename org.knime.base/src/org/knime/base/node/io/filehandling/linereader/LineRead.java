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
 *   5 Nov 2020 (lars.schweikardt): created
 */
package org.knime.base.node.io.filehandling.linereader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Pattern;

import org.knime.base.node.io.filehandling.streams.CompressionAwareCountingInputStream;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessibleUtils;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.util.BomEncodingUtils;

/**
 * Class for the line reader which implements {@link Read}.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class LineRead implements Read<Path, String> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LineRead.class);

    private final Path m_path;

    private final BufferedReader m_reader;

    private final CompressionAwareCountingInputStream m_compressionAwareStream;

    private final long m_size;

    private final TableReadConfig<LineReaderConfig2> m_config;

    private final LineReaderConfig2 m_lineReaderConfig;

    private final Pattern m_regexPattern;

    private final boolean m_limitRows;

    private final boolean m_replaceEmpty;

    private final String m_emptyLineReplacement;

    private final long m_maxRows;

    private long m_linesRead;

    /**
     * Constructor.
     *
     * @param path the {@link Path} to the file
     * @param config the {@link TableReadConfig} of the node
     * @throws IOException
     */
    LineRead(final Path path, final TableReadConfig<LineReaderConfig2> config) throws IOException {
        m_config = config;
        m_lineReaderConfig = m_config.getReaderSpecificConfig();

        m_path = path;
        m_size = Files.size(m_path);

        m_compressionAwareStream = new CompressionAwareCountingInputStream(path);

        final String charSetName = config.getReaderSpecificConfig().getCharSetName();
        final Charset charset = charSetName == null ? Charset.defaultCharset() : Charset.forName(charSetName);
        m_reader = BomEncodingUtils.createBufferedReader(m_compressionAwareStream, charset);
        m_regexPattern = Pattern.compile(config.getReaderSpecificConfig().getRegex());
        m_linesRead = m_config.useColumnHeaderIdx() ? -1 : 0;
        m_limitRows = m_config.limitRows();
        m_maxRows = m_config.getMaxRows();
        m_replaceEmpty = m_lineReaderConfig.getReplaceEmptyMode() == EmptyLineMode.REPLACE_EMPTY;
        m_emptyLineReplacement = m_lineReaderConfig.getEmptyLineReplacement();
    }

    @Override
    public RandomAccessible<String> next() throws IOException {
        RandomAccessible<String> nextRow;
        //First row used as column header
        if (m_config.useColumnHeaderIdx() && m_linesRead == -1) {
            nextRow = RandomAccessibleUtils.createFromArray(m_reader.readLine());
        } else {
            nextRow = getNextLine();
        }
        m_linesRead++;
        return nextRow;
    }

    /**
     * Returns the next line.
     *
     * @return the next line
     * @throws IOException
     */
    private RandomAccessible<String> getNextLine() throws IOException {
        RandomAccessible<String> row;
        if (!m_limitRows || m_linesRead < m_maxRows) {
            row = getLine();
        } else {
            //When limit rows true and it exceeds the set limit
            row = null;
        }

        return row;
    }

    /**
     * Returns the next line.
     *
     * @return the next {@link RandomAccessible}
     * @throws IOException
     */
    private RandomAccessible<String> getLine() throws IOException {
        boolean matches = false;
        String line;
        do {
            line = m_reader.readLine();
            if (line == null) {
                //no more lines to read
                return null;
            }

            if (line.trim().isEmpty()) {
                if (m_config.skipEmptyRows()) {
                    continue;
                } else if (m_replaceEmpty) {
                    line = m_emptyLineReplacement;
                } else {
                    return createRandomAccessible(null);
                }
            }

            if (m_lineReaderConfig.useRegex()) {
                matches = m_regexPattern.matcher(line).matches();
            } else {
                matches = true;
            }
        } while (!matches);

        return createRandomAccessible(line);
    }

    /**
     * Creates a {@link RandomAccessible} with a row id and a line.
     *
     * @param line the content of a line
     * @return a {@link RandomAccessible}
     */
    private static RandomAccessible<String> createRandomAccessible(final String line) {
        return RandomAccessibleUtils.createFromArray(line);
    }

    @Override
    public Optional<Path> getItem() {
        return Optional.of(m_path);
    }

    @Override
    public void close() throws IOException {
        try {
            m_reader.close();
        } catch (IOException e) {
            LOGGER.error(e);
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
