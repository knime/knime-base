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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import javax.swing.JPanel;

import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.time.util.DialogComponentDateTimeSelection;
import org.knime.time.util.DialogComponentDateTimeSelection.DisplayOption;
import org.knime.time.util.SettingsModelDateTime;

/**
 * DateTime UI component for Row Filter node.
 *
 * @author Viktor Buria
 * @since 3.8
 */
public class DateTimeField {

    // Date
    private final SettingsModelDateTime m_dateModel = new SettingsModelDateTime("date", null, null, null);

    private final DialogComponentDateTimeSelection m_dateComponent =
        new DialogComponentDateTimeSelection(m_dateModel, "", DisplayOption.SHOW_DATE_ONLY);

    // DateTime
    private final SettingsModelDateTime m_dateTimeModel = new SettingsModelDateTime("date_time", null, null, null);

    private final DialogComponentDateTimeSelection m_dateTimeComponent =
        new DialogComponentDateTimeSelection(m_dateTimeModel, "", DisplayOption.SHOW_DATE_AND_TIME);

    // Time
    private final SettingsModelDateTime m_timeModel = new SettingsModelDateTime("time", null, null, null);

    private final DialogComponentDateTimeSelection m_timeComponent =
        new DialogComponentDateTimeSelection(m_timeModel, "", DisplayOption.SHOW_TIME_ONLY);

    private final JPanel m_panel;

    private DataType m_type;

    /**
     * Constructs a {@link DateTimeField} object.
     *
     * @param panel the {@link JPanel} where current component should be shown.
     * @param type the current {@link DataType} to set
     */
    public DateTimeField(final JPanel panel, final DataType type) {
        m_panel = Objects.requireNonNull(panel, "panel");

        m_panel.add(m_dateComponent.getComponentPanel());
        m_panel.add(m_dateTimeComponent.getComponentPanel());
        m_panel.add(m_timeComponent.getComponentPanel());

        setType(Objects.requireNonNull(type, "type"));
    }

    /**
     * Sets data type for the DateTime field.
     *
     * @param type the {@link DataType}
     */
    public void setType(final DataType type) {
        m_type = type;

        if (LocalDateCellFactory.TYPE.equals(type)) {
            m_dateComponent.getComponentPanel().setVisible(true);
            m_dateTimeComponent.getComponentPanel().setVisible(false);
            m_timeComponent.getComponentPanel().setVisible(false);

        } else if (LocalDateTimeCellFactory.TYPE.equals(type)) {
            m_dateComponent.getComponentPanel().setVisible(false);
            m_dateTimeComponent.getComponentPanel().setVisible(true);
            m_timeComponent.getComponentPanel().setVisible(false);

        } else if (LocalTimeCellFactory.TYPE.equals(type)) {
            m_dateComponent.getComponentPanel().setVisible(false);
            m_dateTimeComponent.getComponentPanel().setVisible(false);
            m_timeComponent.getComponentPanel().setVisible(true);

        } else {
            throw new IllegalArgumentException(
                "Only LocalDate, LocalTime and LocalDateTime types are acceptable, but got " + type);
        }
    }

    /**
     * Gets date time value.
     *
     * @return the current value
     */
    public String getValue() {
        final String result;
        if (LocalDateCellFactory.TYPE.equals(m_type)) {
            result = m_dateModel.getLocalDate().toString();

        } else if (LocalDateTimeCellFactory.TYPE.equals(m_type)) {
            result = m_dateTimeModel.getLocalDateTime().toString();
        } else {
            result = m_timeModel.getLocalTime().toString();
        }
        return result;
    }

    /**
     * Sets date time value.
     *
     * @param value the new value
     */
    public void setValue(final String value) {

        if (LocalDateCellFactory.TYPE.equals(m_type)) {
            try {
                m_dateModel.setLocalDate(value == null ? LocalDate.now() : LocalDate.parse(value));
            } catch (DateTimeParseException e) {
                m_dateModel.setLocalDate(LocalDate.now());
            }
        } else if (LocalDateTimeCellFactory.TYPE.equals(m_type)) {
            try {
                m_dateTimeModel.setLocalDateTime(value == null ? LocalDateTime.now() : LocalDateTime.parse(value));
            } catch (DateTimeParseException e) {
                m_dateTimeModel.setLocalDate(LocalDate.now());
            }
        } else {
            try {
                m_timeModel.setLocalTime(value == null ? LocalTime.now() : LocalTime.parse(value));
            } catch (DateTimeParseException e) {
                m_timeModel.setLocalDate(LocalDate.now());
            }
        }
    }

}
