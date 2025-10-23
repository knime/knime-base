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
package org.knime.time.node.convert.datetimetostring;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCell;
import org.knime.core.data.time.localdatetime.LocalDateTimeCell;
import org.knime.core.data.time.localtime.LocalTimeCell;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.history.DateTimeFormatStringHistoryManager;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;
import org.knime.time.util.LocaleStateProviderTest;
import org.knime.time.util.LocaleStateProviderTest.TestLocaleStateProvider;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class DateTimeToStringNodeSettingsTest extends DefaultNodeSettingsSnapshotTest { // NOSONAR

    private Locale m_defaultLocale;

    static final PortObjectSpec[] TEST_TABLE_SPECS = new PortObjectSpec[]{new DataTableSpec( //
        new String[]{ //
            "localTimeTest", //"
            "localDateTest", //
            "localDateTimeTest", //
            "zonedDateTimeTest"},
        new DataType[]{ //
            DataType.getType(LocalTimeCell.class), //
            DataType.getType(LocalDateCell.class), //
            DataType.getType(LocalDateTimeCell.class), //
            DataType.getType(ZonedDateTimeCell.class)} //
            )};

    protected DateTimeToStringNodeSettingsTest() {
        super(getConfig());
    }

    private static final ZoneId MOCKED_ZONE_ID = ZoneId.of("Europe/Berlin");

    private static final ZonedDateTime MOCKED_NOW =
        LocalDateTime.of(LocalDate.of(1, 1, 1), LocalTime.of(14, 00)).atZone(MOCKED_ZONE_ID);

    private static final List<String> MOCKED_RECENT_FORMATS = List.of("yyyy", "HH:ss");

    private MockedStatic<ZonedDateTime> m_mockedStaticLocalZonedDateTime;

    private MockedStatic<ZoneId> m_mockedStaticZoneId;

    private MockedStatic<DateTimeFormatStringHistoryManager> m_mockedStaticDateTimeFormatStringHistoryManager;

    private TestLocaleStateProvider m_mockedLocaleStateProvider;

    @BeforeEach
    void setDefaults() {
        m_defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        m_mockedStaticLocalZonedDateTime = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS);
        m_mockedStaticLocalZonedDateTime.when(ZonedDateTime::now).thenReturn(MOCKED_NOW);

        m_mockedStaticZoneId = Mockito.mockStatic(ZoneId.class, Mockito.CALLS_REAL_METHODS);
        m_mockedStaticZoneId.when(ZoneId::systemDefault).thenReturn(MOCKED_ZONE_ID);

        m_mockedStaticDateTimeFormatStringHistoryManager =
            Mockito.mockStatic(DateTimeFormatStringHistoryManager.class, Mockito.CALLS_REAL_METHODS);
        m_mockedStaticDateTimeFormatStringHistoryManager
            .when(() -> DateTimeFormatStringHistoryManager.getRecentFormats()).thenReturn(MOCKED_RECENT_FORMATS);
        m_mockedLocaleStateProvider = LocaleStateProviderTest.withMockedLocales();
    }

    @AfterEach
    void resetDefaults() {
        Locale.setDefault(m_defaultLocale);
        m_mockedStaticLocalZonedDateTime.close();
        m_mockedStaticZoneId.close();
        m_mockedStaticDateTimeFormatStringHistoryManager.close();
        m_mockedLocaleStateProvider.close();
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(TEST_TABLE_SPECS) //
            .testJsonFormsForModel(DateTimeToStringNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static DateTimeToStringNodeSettings readSettings() {
        try {
            var path = getSnapshotPath(DateTimeToStringNodeSettings.class).getParent().resolve("node_settings")
                .resolve("DateTimeToStringNodeSettings.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    DateTimeToStringNodeSettings.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }
}
