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
 *   Oct 8, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.data.filter.row.dialog.panel;

import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.knime.base.data.filter.row.dialog.component.ColumnRole;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;

/**
 * Utility class for initializing JSpinners for {@link DataType DataTypes} and {@link ColumnRole ColumnRoles}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class NumericFieldUtil {

    /**
     * This is a static utility class
     */
    private NumericFieldUtil() {
    }

    static void initSpinner(final JSpinner spinner, final ColumnSpec columnSpec, final String value) {
        final DataType columnType = columnSpec.getType();
        if (columnType.isCompatible(IntValue.class)) {
            initInt(spinner, value);
        } else if (columnType.isCompatible(LongValue.class)) {
            initLong(spinner, columnSpec, value);
        } else if (columnType.isCompatible(DoubleValue.class)) {
            initDouble(spinner, value);
        } else {
            throw new IllegalArgumentException("Unsupported data type for spinners: " + columnType);
        }
    }

    private static void initDouble(final JSpinner spinner, final String value) {
        spinner.setModel(new SpinnerNumberModel(0.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.1));
        try {
            spinner.setValue(value == null ? Double.valueOf(0.0) : Double.parseDouble(value));
        } catch (NumberFormatException e) {
            spinner.setValue(Double.valueOf(0.0));
        }
    }

    private static void initLong(final JSpinner spinner, final ColumnSpec columnSpec, final String value) {
        initLongModel(spinner, columnSpec);
        setLongValue(spinner, value);
    }

    private static void setLongValue(final JSpinner spinner, final String value) {
        try {
            spinner.setValue(value == null ? Long.valueOf(0L) : Long.parseLong(value));
        } catch (NumberFormatException e) {
            spinner.setValue(Long.valueOf(0L));
        }
    }

    private static void initLongModel(final JSpinner spinner, final ColumnSpec columnSpec) {
        if (columnSpec.getRole() == ColumnRole.ROW_INDEX) {
            spinner.setModel(new SpinnerNumberModel(0L, (Comparable<Long>)Long.valueOf(0L), Long.MAX_VALUE, 1L));
        } else {
            spinner.setModel(new SpinnerNumberModel(0L, (Comparable<Long>)Long.MIN_VALUE, Long.MAX_VALUE, 1L));
        }
    }

    private static void initInt(final JSpinner spinner, final String value) {
        spinner.setModel(new SpinnerNumberModel(1, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        try {
            spinner.setValue(value == null ? Integer.valueOf(0) : Integer.parseInt(value));
        } catch (NumberFormatException e) {
            spinner.setValue(Integer.valueOf(0));
        }
    }
}
