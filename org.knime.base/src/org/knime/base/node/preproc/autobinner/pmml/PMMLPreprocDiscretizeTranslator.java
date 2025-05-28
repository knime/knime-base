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
 *   Jun 2, 2025 (david): created
 */
package org.knime.base.node.preproc.autobinner.pmml;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.IntervalDocument;
import org.dmg.pmml.IntervalDocument.Interval.Closure.Enum;
import org.dmg.pmml.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml.TransformationDictionaryDocument.TransformationDictionary;
import org.knime.core.node.port.pmml.preproc.PMMLPreprocTranslator;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 *
 * @since 5.5
 */
public class PMMLPreprocDiscretizeTranslator implements PMMLPreprocTranslator {

    private final Configuration m_config;

    public PMMLPreprocDiscretizeTranslator(final Configuration config) {
        m_config = config;
    }

    @Override
    public List<Integer> initializeFrom(final DerivedField[] derivedFields) {
        // this transformation doesn't need any input derived fields, so return an empty list.
        return Collections.emptyList();
    }

    @Override
    public TransformationDictionary exportToTransDict() {
        var dict = TransformationDictionary.Factory.newInstance();

        for (var columnDiscretization : m_config.getDiscretizations().entrySet()) {
            var newDerivedField = dict.addNewDerivedField();
            newDerivedField.setName(columnDiscretization.getKey());
            var newDiscretize = newDerivedField.addNewDiscretize();

            for (var binSpec : columnDiscretization.getValue()) {
                var newBin = newDiscretize.addNewDiscretizeBin();
                newBin.setBinValue(columnDiscretization.getKey());

                // PMML bins can have multiple intervals, so add them all
                for (var interval : binSpec.intervals()) {
                    var newInterval = newBin.addNewInterval();
                    newInterval.setLeftMargin(interval.leftMargin());
                    newInterval.setRightMargin(interval.rightMargin());
                    newInterval.setClosure(interval.closure().m_pmmlClosure);
                }
            }
        }

        return dict;
    }

    /**
     * @return the config
     */
    public Configuration getConfig() {
        return m_config;
    }

    @Override
    public LocalTransformations exportToLocalTrans() {
        return null;
    }

    public static class Configuration {

        private final Map<String, List<Bin>> m_binsByColumnName;

        public Configuration(final Map<String, List<Bin>> binsByColumnName) {
            m_binsByColumnName = binsByColumnName;
        }

        /**
         * Get the names of the columns that are discretized and their respective discretizations.
         *
         * @return the map of column names to bins
         */
        public Map<String, List<Bin>> getDiscretizations() {
            return Collections.unmodifiableMap(m_binsByColumnName);
        }

        public Set<String> getTargetColumnNames() {
            return Collections.unmodifiableSet(m_binsByColumnName.keySet());
        }

        public static enum ClosureStyle {
                OPEN_OPEN(IntervalDocument.Interval.Closure.Enum.forInt(2)), //
                CLOSED_CLOSED(IntervalDocument.Interval.Closure.Enum.forInt(4)), //
                OPEN_CLOSED(IntervalDocument.Interval.Closure.Enum.forInt(1)), //
                CLOSED_OPEN(IntervalDocument.Interval.Closure.Enum.forInt(3));

            private final Enum m_pmmlClosure;

            ClosureStyle(final IntervalDocument.Interval.Closure.Enum pmmlClosure) {
                m_pmmlClosure = pmmlClosure;
            }

            public static ClosureStyle from(final boolean leftClosed, final boolean rightClosed) {
                if (leftClosed && rightClosed) {
                    return CLOSED_CLOSED;
                } else if (leftClosed && !rightClosed) {
                    return CLOSED_OPEN;
                } else if (!leftClosed && rightClosed) {
                    return OPEN_CLOSED;
                } else {
                    return OPEN_OPEN;
                }
            }
        }

        public static record Interval(double leftMargin, double rightMargin, ClosureStyle closure) {

            public Interval(final double leftMargin, final double rightMargin, final ClosureStyle closure) {
                if (leftMargin > rightMargin) {
                    throw new IllegalArgumentException("Left margin must be less than or equal to right margin.");
                }

                this.leftMargin = leftMargin;
                this.rightMargin = rightMargin;
                this.closure = closure;
            }

            public boolean covers(final double value) {
                return switch (closure) {
                    case OPEN_OPEN -> value > leftMargin && value < rightMargin;
                    case CLOSED_CLOSED -> value >= leftMargin && value <= rightMargin;
                    case OPEN_CLOSED -> value > leftMargin && value <= rightMargin;
                    case CLOSED_OPEN -> value >= leftMargin && value < rightMargin;
                };
            }

            @Override
            public String toString() {
                var bottomClosed = closure == ClosureStyle.CLOSED_CLOSED || closure == ClosureStyle.CLOSED_OPEN;
                var topClosed = closure == ClosureStyle.CLOSED_CLOSED || closure == ClosureStyle.OPEN_CLOSED;

                var leftMarginStr = bottomClosed ? "[" : "(";
                var rightMarginStr = topClosed ? "]" : ")";

                return leftMarginStr + leftMargin + ", " + rightMargin + rightMarginStr;
            }
        }

        public static record Bin(String binValue, List<Interval> intervals) {

            public boolean covers(final double value) {
                return intervals.stream().anyMatch(interval -> interval.covers(value));
            }

            @Override
            public final String toString() {
                var formattedIntervals = intervals.size() == 1 ? intervals.get(0).toString() : intervals.toString();
                return "Bin{binValue=" + binValue + ", intervals=" + formattedIntervals + "}";
            }
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final Map<String, List<Bin>> m_binsByColumnName;

            private Builder() {
                m_binsByColumnName = new HashMap<>();
            }

            public Builder addColumn(final String columnName, final List<Bin> bins) {
                m_binsByColumnName.put(columnName, bins);
                return this;
            }

            public Configuration build() {
                return new Configuration(m_binsByColumnName);
            }
        }
    }
}
