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
 *   22 Mar 2024 (jasper): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 * Predicate for filtering rows based on some value extracted from the row. (Probably the row key or some cell contents)
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
abstract class RowPredicate<T> implements Predicate<DataRow> {

    private final Function<DataRow, T> m_extractValue;

    private final Predicate<T> m_predicate;

    protected RowPredicate(final Function<DataRow, T> extractValue, final Predicate<T> predicate) {
        m_extractValue = extractValue;
        m_predicate = predicate;
    }

    /**
     * @param row the row to test
     */
    @Override
    public boolean test(final DataRow row) {
        return m_predicate.test(m_extractValue.apply(row));
    }

    protected static <T> Predicate<Optional<T>> wrapOptional(final Predicate<T> predicate) {
        return o -> o.isPresent() && predicate.test(o.get());
    }

    @SuppressWarnings("restriction")
    public static RowPredicate<?> forSettings(final RowFilter3NodeSettings settings, final DataTableSpec spec)
        throws InvalidSettingsException {
        // Order matters here: Columns that get a "more special" treatment have to be checked first.
        // For example, a BooleanCell implements BooleanValue, but also LongValue and DoubleValue, so we need to rule it
        // out first. RowID and filtering on missing cells are the "most specialised" scenarios, then we check for data
        // types with decreasing specificy
        if (SpecialColumns.ROWID.getId().equals(settings.m_column.getSelected())) {
            return new RowKeyRowPredicate(settings);
        } else if (settings.m_operator == FilterOperator.IS_MISSING) {
            return new IsMissingRowPredicate(colIdx(spec, settings));
        } else {
            return switch (settings.m_compareOn) {
                case BOOLEAN_VALUE -> new BooleanValuePredicate(colIdx(spec, settings), settings);
                case DOUBLE_VALUE -> new DoubleValuePredicate(colIdx(spec, settings), settings);
                case LONG_VALUE -> new LongValuePredicate(colIdx(spec, settings), settings);
                case STRING_VALUE -> new StringValuePredicate(colIdx(spec, settings), settings);
            };
        }
    }

    public static RowPredicate<?> truePredicate() {
        return new RowPredicate<>(row -> null, n -> true) {
        };
    }

    @SuppressWarnings("restriction")
    private static int colIdx(final DataTableSpec spec, final RowFilter3NodeSettings settings)
        throws InvalidSettingsException {
        final var idx = spec.findColumnIndex(settings.m_column.getSelected());
        CheckUtils.checkSetting(idx >= 0, "Column not found: " + settings.m_column.getSelected());
        return idx;
    }

}