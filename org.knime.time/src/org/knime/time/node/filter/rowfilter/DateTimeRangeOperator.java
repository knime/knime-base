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
 *   5 Mar 2026 (Paul Bärnreuther): created
 */
package org.knime.time.node.filter.rowfilter;

import java.util.function.Predicate;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparatorDelegator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValidationUtil;
import org.knime.time.node.filter.rowfilter.DateTimeRangeFilterParameters.ResolvedRange;

/**
 * Range operator for date/time types. Filters rows whose value falls within a range defined by start and end bounds,
 * each of which can be inclusive or exclusive.
 *
 * @param <P> the concrete range parameters type
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public class DateTimeRangeOperator<P extends DateTimeRangeFilterParameters<?, ?, ?>> implements FilterOperator<P> {

    private final DataType m_dataType;

    private final Class<P> m_parametersClass;

    /**
     * @param dataType the data type this operator filters
     * @param parametersClass the concrete parameters class
     */
    public DateTimeRangeOperator(final DataType dataType, final Class<P> parametersClass) {
        m_dataType = dataType;
        m_parametersClass = parametersClass;
    }

    @Override
    public String getId() {
        return "RANGE";
    }

    @Override
    public String getLabel() {
        return "Within range";
    }

    @Override
    public Class<P> getNodeParametersClass() {
        return m_parametersClass;
    }

    @Override
    public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
        final DataType configureColumnType, final P filterParameters) throws InvalidSettingsException {
        final var type = runtimeColumnSpec.getType();
        if (!type.isCompatible(m_dataType.getPreferredValueClass())) {
            throw FilterValidationUtil.createInvalidSettingsException(builder -> builder
                .withSummary("Operator \"%s\" for column \"%s\" expects data of type \"%s\", but got \"%s\""
                    .formatted(getLabel(), runtimeColumnSpec.getName(), m_dataType.getName(), type.getName()))
                .addResolutions("Select a different operator that is compatible with the column's data type \"%s\"."
                    .formatted(type.getName())));
        }

        final ResolvedRange range = filterParameters.resolve();
        final var comparator = new DataValueComparatorDelegator<>(m_dataType.getComparator());

        return dv -> {
            final var cmpStart = comparator.compare(dv, range.start());
            final var cmpEnd = comparator.compare(dv, range.end());
            final var afterStart = range.startInclusive() ? cmpStart >= 0 : cmpStart > 0;
            final var beforeEnd = range.endInclusive() ? cmpEnd <= 0 : cmpEnd < 0;
            return afterStart && beforeEnd;
        };
    }

}
