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
 *   Jan 29, 2024 (hornm): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettingsTest.saveLoad;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.CharsetPersistor;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.SkipFirstLinesPersistor;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.IfRowHasLessColumnsOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.IfRowHasLessColumnsOption.IfRowHasLessColumnsOptionPersistor;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption.RowDelimiterPersistor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

/**
 * {@link DefaultNodeSettingsSnapshotTest} for the {@link CSVTableReaderNodeSettings}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
class CSVTableReaderNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
    protected CSVTableReaderNodeSettingsTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(CSVTableReaderNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> createSettings()) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> createSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static CSVTableReaderNodeSettings createSettings() {
        var res = new CSVTableReaderNodeSettings();
        res.m_settings.m_source.m_path = new FSLocation(FSCategory.RELATIVE, "foo");
        return res;
    }

    private static CSVTableReaderNodeSettings readSettings() {
        try {
            var path = getSnapshotPath(CSVTableReaderNodeSettingsTest.class).getParent().resolve("node_settings")
                .resolve("CSVTableReaderNodeSettings.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return DefaultNodeSettings.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    CSVTableReaderNodeSettings.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testIfRowHasLessColumnsOptionPersistor(final IfRowHasLessColumnsOption ifRowHasLessColumnsOption)
        throws InvalidSettingsException {
        final var copy = saveLoad(new IfRowHasLessColumnsOptionPersistor(), ifRowHasLessColumnsOption);
        assertEquals(ifRowHasLessColumnsOption, copy);
    }

    private static Stream<IfRowHasLessColumnsOption> testIfRowHasLessColumnsOptionPersistor() {
        return Stream.of(IfRowHasLessColumnsOption.INSERT_MISSING, IfRowHasLessColumnsOption.FAIL);
    }

    @ParameterizedTest
    @MethodSource
    void testRowDelimiterPersistor(final RowDelimiterOption rowDelimiterOption) throws InvalidSettingsException {
        final var copy = saveLoad(new RowDelimiterPersistor(), rowDelimiterOption);
        assertEquals(rowDelimiterOption, copy);
    }

    private static Stream<RowDelimiterOption> testRowDelimiterPersistor() {
        return Stream.of(RowDelimiterOption.LINE_BREAK, RowDelimiterOption.CUSTOM);
    }

    @ParameterizedTest
    @MethodSource
    void testSkipFirstLinesPersistor(final Long l) throws InvalidSettingsException {
        final var copy = saveLoad(new SkipFirstLinesPersistor(), l);
        assertEquals(l, copy);
    }

    private static Stream<Long> testSkipFirstLinesPersistor() {
        return Stream.of(0l, 1l);
    }

    @ParameterizedTest
    @MethodSource
    void testCharsetPersistor(final Charset charset) throws InvalidSettingsException {
        final var copy = saveLoad(new CharsetPersistor(), charset);
        assertEquals(charset.m_fileEncoding, copy.m_fileEncoding);
        assertEquals(charset.m_customEncoding, copy.m_customEncoding);
    }

    private static Stream<Charset> testCharsetPersistor() {
        return Stream.of(new Charset(FileEncodingOption.DEFAULT), new Charset(FileEncodingOption.UTF_8),
            new Charset(FileEncodingOption.OTHER, "foo"));
    }

}
