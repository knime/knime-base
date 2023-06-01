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
 *   May 31, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReader;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;

/**
 * Contains unit tests for the {@link CSVTableReader}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class CSVTableReaderTest {

    @TempDir
    private Path m_tempFolder;

    private CSVTableReader m_reader = new CSVTableReader();

    @Test
    void testParallelReadEndsExactlyOnBoundary() throws Exception {
        var firstRow = "0,a,4.5";
        var secondRow = "1,b,1.7";
        var thirdRow = "2,c,3.9";
        var fourthRow = "3,d,7.6";

        var filePath = m_tempFolder.resolve("tmp.csv");
        Files.writeString(filePath, firstRow + "\n" + secondRow + "\n" + thirdRow + "\n" + fourthRow + "\n");

        var fsLocation = new FSLocation(FSCategory.LOCAL, filePath.toAbsolutePath().toString());
        var connection = DefaultFSConnectionFactory.createLocalFSConnection();
        var fsPath = connection.getFileSystem().getPath(fsLocation);

        var csvReaderConfig = new CSVTableReaderConfig();
        csvReaderConfig.setMinChunkSizeInBytes(firstRow.toCharArray().length + secondRow.toCharArray().length + 2);
        csvReaderConfig.noRowDelimitersInQuotes(true);
        csvReaderConfig.setLineSeparator("\n");
        var tableReaderConfig = new DefaultTableReadConfig<>(csvReaderConfig);
        tableReaderConfig.setUseColumnHeaderIdx(false);
        var reads = m_reader.multiRead(fsPath, tableReaderConfig);
        try {
            assertEquals(2, reads.size(), "Expected exactly two reads to be generated.");
            var read = reads.get(0);
            assertNotNull(read.next(), "The first read should read the first row.");
            assertNotNull(read.next(), "The first read should read the second row.");
            assertNotNull(read.next(), "The first read should read the third row.");
            assertNull(read.next(), "The first read should read only the first three rows.");
            read = reads.get(1);
            assertNotNull(read.next(), "The second read should read the fourth row.");
            assertNull(read.next());
        } finally {
            for (var read : reads) {
                read.close();
            }
        }



    }
}
