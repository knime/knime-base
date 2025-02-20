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
 *   Dec 4, 2024 (Tobias Kampmann): created
 */
package org.knime.time.node.manipulate.datetimeshift;

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesStateProvider;

/**
 *
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
final class DateTimeShiftSettingsUtils {

    DateTimeShiftSettingsUtils() {
        // utility class
    }

    static void validateDurationColumn(final String columnName, final DataTableSpec spec)
        throws InvalidSettingsException {
        validateColumn(columnName, spec, "duration", DateTimeShiftSettingsUtils::isDurationColumn);
    }

    static void validatePeriodColumn(final String columnName, final DataTableSpec spec)
        throws InvalidSettingsException {
        validateColumn(columnName, spec, "period", DateTimeShiftSettingsUtils::isPeriodColumn);
    }

    static void validateNumericalColumn(final String columnName, final DataTableSpec spec)
        throws InvalidSettingsException {
        validateColumn(columnName, spec, "numerical", DateTimeShiftSettingsUtils::isWholeNumberColumn);
    }

    static boolean isWholeNumberColumn(final DataColumnSpec column) {
        return column.getType().isCompatible(LongValue.class) || column.getType().isCompatible(IntValue.class);
    }

    static boolean isDurationColumn(final DataColumnSpec column) {
        return column.getType().isCompatible(DurationValue.class);
    }

    static boolean isPeriodColumn(final DataColumnSpec column) {
        return column.getType().isCompatible(PeriodValue.class);
    }

    static final class NumberColumnProvider implements ColumnChoicesStateProvider {
        @Override
        public void init(final StateProviderInitializer initializer) {
            ColumnChoicesStateProvider.super.init(initializer);
        }

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0)//
                .map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .filter(DateTimeShiftSettingsUtils::isWholeNumberColumn)//
                .toArray(DataColumnSpec[]::new);
        }

        static String getFirstNumberColumn(final DataTableSpec spec) {
            if (spec == null) {
                return null;
            }
            return spec.stream() //
                .filter(DateTimeShiftSettingsUtils::isWholeNumberColumn) //
                .map(DataColumnSpec::getName) //
                .findFirst() //
                .orElse(null);
        }
    }

    private static void validateColumn(final String columnName, final DataTableSpec spec, final String typeName,
        final Predicate<DataColumnSpec> typePredicate) throws InvalidSettingsException {
        if (columnName == null || columnName.isEmpty()) {
            throw new InvalidSettingsException("No " + typeName + " column selected.");
        }

        var column = spec.getColumnSpec(columnName);
        if (column == null) {
            throw new InvalidSettingsException("The column '" + columnName
                + "' is no longer present in the input table. Choose a different column or adapt the input.");
        }

        if (!typePredicate.test(column)) {
            throw new InvalidSettingsException(
                "The column '" + columnName + "' is not compatible to a " + typeName + " value.");
        }
    }

}
