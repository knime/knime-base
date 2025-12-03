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
 *   Sep 30, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.probability.nominal.creator;

import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.node.parameters.widget.choices.Label;

/**
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
enum ColumnType implements ButtonGroupEnumInterface {
        @Label(value = "Numeric columns", description = """
                Creates probability distributions from one or more numeric columns containing probability values.
                """)
        NUMERIC_COLUMN("Create probability distribution from numeric columns", null, true),
        @Label(value = "String column", description = """
                Creates a one-hot encoded probability distribution from a single string column.
                """)
        STRING_COLUMN("Create probability distribution from string column", null, false);

    private final String m_text;
    private final String m_toolTip;
    private final boolean m_numericColumn;


    private ColumnType(final String text, final String toolTip, final boolean numericColumn) {
        m_text = text;
        m_toolTip = toolTip;
        m_numericColumn = numericColumn;
    }

    static ColumnType getFromValue(final String value) throws InvalidSettingsException {
        for (final ColumnType columnType : values()) {
            if (columnType.name().equals(value)) {
                return columnType;
            }
        }
        throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
    }

    private static String createInvalidSettingsExceptionMessage(final String name) {
        var values = List.of(NUMERIC_COLUMN.name(), STRING_COLUMN.name()).stream().collect(Collectors.joining(", "));
        return String.format("Invalid value '%s'. Possible values: %s", name, values);
    }

    public boolean isNumericColumn() {
        return m_numericColumn;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return m_text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getActionCommand() {
        return name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTip() {
        return m_toolTip;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDefault() {
        return NUMERIC_COLUMN == this;
    }

    /**
     * @return the default column type to use
     */
    public static ColumnType getDefault() {
        ColumnType[] values = values();
        for (final ColumnType type : values) {
            if (type.isDefault()) {
                return type;
            }
        }
        //return the first if none is default
        return values[0];
    }
}
