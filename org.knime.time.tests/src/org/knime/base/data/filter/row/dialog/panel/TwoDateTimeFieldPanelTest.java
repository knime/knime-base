/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.filter.row.dialog.panel;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.knime.base.data.filter.row.dialog.OperatorPanelParameters;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SuppressWarnings("javadoc")
public class TwoDateTimeFieldPanelTest {

    private TwoDateTimeFieldsPanel m_panel;

    @Mock
    private OperatorPanelParameters m_parameters;

    @Rule
    public MockitoRule m_mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() {
        m_panel = new TwoDateTimeFieldsPanel();
    }

    @Test
    public void canSetCurrentDate_whenInitPanel_andNoValue() {
        // given
        when(m_parameters.getColumnSpec()).thenReturn(new ColumnSpec("date", LocalDateCellFactory.TYPE));
        when(m_parameters.getValues()).thenReturn(new String[0]);

        // when
        m_panel.init(m_parameters);

        // then
        final String[] result = m_panel.getValues();
        assertThat(result, Matchers.arrayWithSize(2));
        assertThat(result[0], Matchers.equalTo(LocalDate.now().toString()));
        assertThat(result[1], Matchers.equalTo(LocalDate.now().toString()));
    }

    @Test
    public void canSetDate_whenInitPanel() {
        // given
        when(m_parameters.getColumnSpec()).thenReturn(new ColumnSpec("date", LocalDateCellFactory.TYPE));
        when(m_parameters.getValues()).thenReturn(new String[]{"2016-05-23", "2017-01-01"});

        // when
        m_panel.init(m_parameters);

        // then
        final String[] result = m_panel.getValues();
        assertThat(result, Matchers.arrayWithSize(2));
        assertThat(result[0], Matchers.equalTo("2016-05-23"));
        assertThat(result[1], Matchers.equalTo("2017-01-01"));
    }

    @Test
    public void canSetDateTime_whenInitPanel() {
        // given
        when(m_parameters.getColumnSpec()).thenReturn(new ColumnSpec("date_time", LocalDateTimeCellFactory.TYPE));
        when(m_parameters.getValues()).thenReturn(new String[]{"2016-05-23T06:30:00", "2018-02-13T15:12:20"});

        // when
        m_panel.init(m_parameters);

        // then
        final String[] result = m_panel.getValues();
        assertThat(result, Matchers.arrayWithSize(2));
        assertThat(result[0], Matchers.equalTo("2016-05-23T06:30"));
        assertThat(result[1], Matchers.equalTo("2018-02-13T15:12:20"));
    }

    @Test
    public void canSetTime_whenInitPanel() {
        // given
        when(m_parameters.getColumnSpec()).thenReturn(new ColumnSpec("time", LocalTimeCellFactory.TYPE));
        when(m_parameters.getValues()).thenReturn(new String[]{"04:10:00", "12:15:05"});

        // when
        m_panel.init(m_parameters);

        // then
        final String[] result = m_panel.getValues();
        assertThat(result, Matchers.arrayWithSize(2));
        assertThat(result[0], Matchers.equalTo("04:10"));
        assertThat(result[1], Matchers.equalTo("12:15:05"));
    }
}
