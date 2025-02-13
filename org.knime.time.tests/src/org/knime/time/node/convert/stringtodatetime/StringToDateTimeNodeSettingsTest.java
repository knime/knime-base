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
 *   25 Jan 2024 (albrecht): created
 */
package org.knime.time.node.convert.stringtodatetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;
import org.knime.testing.node.dialog.updates.DialogUpdateSimulator;
import org.knime.testing.util.TableTestUtil;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings({"restriction", "static-method"})
final class StringToDateTimeNodeSettingsTest extends DefaultNodeSettingsSnapshotTest { // NOSONAR

    private Locale m_defaultLocale;

    private MockedStatic<ZonedDateTime> m_mockedStaticLocalZonedDateTime;

    private MockedStatic<DateTimeFormatStringHistoryManager> m_mockedStaticDateTimeFormatStringHistoryManager;

    private static final ZoneId MOCKED_ZONE_ID = ZoneId.of("Europe/Berlin");

    private static final List<String> MOCKED_RECENT_FORMATS = List.of("yyyy", "HH:ss");

    /**
     * We need to mock 'now' because we use it for generating examples for each date/time format.
     */
    private static final ZonedDateTime MOCKED_NOW =
        LocalDateTime.of(LocalDate.of(1, 1, 1), LocalTime.of(14, 00)).atZone(MOCKED_ZONE_ID);

    static final PortObjectSpec[] TEST_TABLE_SPECS = new PortObjectSpec[]{
        new DataTableSpec(new String[]{"test"}, new DataType[]{DataType.getType(StringCell.class)})};

    protected StringToDateTimeNodeSettingsTest() {
        super(getConfig());
    }

    @BeforeEach
    void setDefaultLocaleAndMockStatics() {
        m_defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);

        // Mock 'now' to a fixed date/time
        m_mockedStaticLocalZonedDateTime = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS);
        m_mockedStaticLocalZonedDateTime.when(ZonedDateTime::now).thenReturn(MOCKED_NOW);

        m_mockedStaticDateTimeFormatStringHistoryManager =
            Mockito.mockStatic(DateTimeFormatStringHistoryManager.class, Mockito.CALLS_REAL_METHODS);
        m_mockedStaticDateTimeFormatStringHistoryManager
            .when(() -> DateTimeFormatStringHistoryManager.getRecentFormats()).thenReturn(MOCKED_RECENT_FORMATS);

    }

    @AfterEach
    void resetDefaultLocaleAndStaticMocks() {
        Locale.setDefault(m_defaultLocale);

        m_mockedStaticLocalZonedDateTime.close();
        m_mockedStaticDateTimeFormatStringHistoryManager.close();
    }

    @Test
    void testThatGuessButtonWorks() {
        var inputSpec = new TableTestUtil.SpecBuilder().addColumn("datetimes", StringCellFactory.TYPE).build();
        var inputTable = new TableTestUtil.TableBuilder(inputSpec).addRow("2024-01-25 14:00").build().get();
        var fakeContext = DefaultNodeSettingsContext.createDefaultNodeSettingsContext( //
            new PortType[]{BufferedDataTable.TYPE}, //
            new PortObjectSpec[]{inputSpec}, //
            null, //
            null, //
            new PortObject[]{inputTable});

        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("", FormatTemporalType.DATE_TIME);
        settings.m_columnFilter = new ColumnFilter(new String[]{"datetimes"});

        var sim = new DialogUpdateSimulator(Map.of( //
            SettingsType.MODEL, settings //
        ), fakeContext);

        var result = sim.simulateButtonClick(StringToDateTimeNodeSettings.AutoGuessFormatButtonRef.class);

        assertEqualFormats("yyyy-MM-dd HH:mm", result.getValueUpdateAt("format"));
    }

    @Test
    void testThatGuessButtonFailsWhenNoColSelected() {
        var inputSpec = new TableTestUtil.SpecBuilder().addColumn("datetimes", StringCellFactory.TYPE).build();
        var inputTable = new TableTestUtil.TableBuilder(inputSpec).addRow("2024-01-25 14:00").build().get();
        var fakeContext = DefaultNodeSettingsContext.createDefaultNodeSettingsContext( //
            new PortType[]{BufferedDataTable.TYPE}, //
            new PortObjectSpec[]{inputSpec}, //
            null, //
            null, //
            new PortObject[]{inputTable});

        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("", FormatTemporalType.DATE_TIME);
        settings.m_columnFilter = new ColumnFilter();

        var sim = new DialogUpdateSimulator(Map.of( //
            SettingsType.MODEL, settings //
        ), fakeContext);

        var thrown = assertThrows(WidgetHandlerException.class,
            () -> sim.simulateButtonClick(StringToDateTimeNodeSettings.AutoGuessFormatButtonRef.class));
        assertTrue(thrown.getMessage().contains("no selected"));
    }

    @Test
    void testThatGuessButtonFailsWhenNoCommonFormat() {
        var inputSpec = new TableTestUtil.SpecBuilder().addColumn("datetimes", StringCellFactory.TYPE).build();
        var inputTable = new TableTestUtil.TableBuilder(inputSpec) //
            .addRow("2024-01-25") //
            .addRow("14:00") //
            .build().get();
        var fakeContext = DefaultNodeSettingsContext.createDefaultNodeSettingsContext( //
            new PortType[]{BufferedDataTable.TYPE}, //
            new PortObjectSpec[]{inputSpec}, //
            null, //
            null, //
            new PortObject[]{inputTable});

        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("", FormatTemporalType.DATE_TIME);
        settings.m_columnFilter = new ColumnFilter(new String[]{"datetimes"});

        var sim = new DialogUpdateSimulator(Map.of( //
            SettingsType.MODEL, settings //
        ), fakeContext);

        assertThrowsWithMessageContaining(WidgetHandlerException.class,
            () -> sim.simulateButtonClick(StringToDateTimeNodeSettings.AutoGuessFormatButtonRef.class),
            "Expected exception to be thrown", "no common format");
    }

    @Test
    void testThatGuessButtonFailsOnEmptyColumn() {
        var inputSpec = new TableTestUtil.SpecBuilder().addColumn("datetimes", StringCellFactory.TYPE).build();
        var inputTable = new TableTestUtil.TableBuilder(inputSpec).build().get();
        var fakeContext = DefaultNodeSettingsContext.createDefaultNodeSettingsContext( //
            new PortType[]{BufferedDataTable.TYPE}, //
            new PortObjectSpec[]{inputSpec}, //
            null, //
            null, //
            new PortObject[]{inputTable});

        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("", FormatTemporalType.DATE_TIME);
        settings.m_columnFilter = new ColumnFilter(new String[]{"datetimes"});

        var sim = new DialogUpdateSimulator(Map.of( //
            SettingsType.MODEL, settings //
        ), fakeContext);

        assertThrowsWithMessageContaining(WidgetHandlerException.class,
            () -> sim.simulateButtonClick(StringToDateTimeNodeSettings.AutoGuessFormatButtonRef.class),
            "Expected exception to be thrown", "no non-missing rows");
    }

    @Test
    void testThatGuessButtonFailsOnMissingValueColumn() {
        var inputSpec = new TableTestUtil.SpecBuilder().addColumn("datetimes", StringCellFactory.TYPE).build();
        var inputTable = new TableTestUtil.TableBuilder(inputSpec).addRow(DataType.getMissingCell()).build().get();
        var fakeContext = DefaultNodeSettingsContext.createDefaultNodeSettingsContext( //
            new PortType[]{BufferedDataTable.TYPE}, //
            new PortObjectSpec[]{inputSpec}, //
            null, //
            null, //
            new PortObject[]{inputTable});

        var settings = new StringToDateTimeNodeSettings();
        settings.m_format = new TemporalFormat("", FormatTemporalType.DATE_TIME);
        settings.m_columnFilter = new ColumnFilter(new String[]{"datetimes"});

        var sim = new DialogUpdateSimulator(Map.of( //
            SettingsType.MODEL, settings //
        ), fakeContext);

        assertThrowsWithMessageContaining(WidgetHandlerException.class,
            () -> sim.simulateButtonClick(StringToDateTimeNodeSettings.AutoGuessFormatButtonRef.class),
            "Expected exception to be thrown", "no non-missing rows");
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(TEST_TABLE_SPECS) //
            .testJsonFormsForModel(StringToDateTimeNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static StringToDateTimeNodeSettings readSettings() {
        try {
            var path = getSnapshotPath(StringToDateTimeNodeSettings.class).getParent().resolve("node_settings")
                .resolve("StringToDateTimeNodeSettings.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return DefaultNodeSettings.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    StringToDateTimeNodeSettings.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

    static <T extends Throwable> T assertThrowsWithMessageContaining(final Class<T> thrownClass,
        final Executable runnable, final String assertionMessage, final String... contains) {
        T thrown = assertThrows(thrownClass, runnable, assertionMessage);
        for (String contain : contains) {
            assertTrue(thrown.getMessage().contains(contain),
                "Expected message to contain '%s' but was '%s'".formatted(contain, thrown.getMessage()));
        }
        return thrown;
    }

    static void assertEqualFormats(final String expectedFormat, final Object actual) {
        assertTrue(actual instanceof TemporalFormat, "Expected TemporalFormat but was %s".formatted(actual.getClass()));
        assertEquals(expectedFormat, ((TemporalFormat)actual).format());
    }
}
