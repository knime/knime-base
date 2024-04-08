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
 *   3 Apr 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3;

import java.util.function.UnaryOperator;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

import com.google.common.collect.Range;

/**
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui framework
interface FilterBounds<C extends Comparable<C>> extends DefaultNodeSettings {

    final class DecimalBounds implements FilterBounds<Double> {
        @Widget(title = "Lower bound (incl.)", description = "TODO")
        double m_lowerBound;

        @Widget(title = "Upper bound (incl.)", description = "TODO")
        double m_upperBound;

        @Override
        public Double getLowerBound() {
            return m_lowerBound;
        }

        @Override
        public Double getUpperBound() {
            return m_upperBound;
        }

        @Override
        public void validate() throws InvalidSettingsException {
            CheckUtils.checkSetting(m_lowerBound <= m_upperBound,
                    "Lower bound \"%d\" must not be larger than upper bound \"%d\"", m_lowerBound,
                    m_upperBound);
        }
    }

    final class IntegralBounds implements FilterBounds<Long> {
        @Widget(title = "Lower bound (incl.)", description = "TODO")
        long m_lowerBound;

        @Widget(title = "Upper bound (incl.)", description = "TODO")
        long m_upperBound;

        @Override
        public Long getLowerBound() {
            return m_lowerBound;
        }

        @Override
        public Long getUpperBound() {
            return m_upperBound;
        }

        @Override
        public void validate() throws InvalidSettingsException {
            CheckUtils.checkSetting(m_lowerBound <= m_upperBound,
                    "Lower bound \"%d\" must not be larger than upper bound \"%d\"", m_lowerBound,
                    m_upperBound);
        }
    }

    final class RowNumberBounds implements FilterBounds<Long> {
        @Widget(title = "From row number (incl.)", description = "TODO")
        @NumberInputWidget(min = 1)
        long m_lowerBound = 1;

        @Widget(title = "To row number (incl.)", description = "TODO")
        @NumberInputWidget(min = 1)
        long m_upperBound = 1;

        @Override
        public Long getLowerBound() {
            return m_lowerBound;
        }

        @Override
        public Long getUpperBound() {
            return m_upperBound;
        }

        @Override
        public void validate() throws InvalidSettingsException {
            CheckUtils.checkSetting(m_lowerBound <= m_upperBound,
                    "Lower bound \"%d\" must not be larger than upper bound \"%d\"", m_lowerBound,
                    m_upperBound);
        }
    }

    void validate() throws InvalidSettingsException;

    C getLowerBound();

    C getUpperBound();

    default Range<C> rangeClosed() {
        return Range.closed(getLowerBound(), getUpperBound());
    }

    default Range<C> rangeClosed(final UnaryOperator<C> fn) {
        return Range.closed(fn.apply(getLowerBound()), fn.apply(getUpperBound()));
    }
}
