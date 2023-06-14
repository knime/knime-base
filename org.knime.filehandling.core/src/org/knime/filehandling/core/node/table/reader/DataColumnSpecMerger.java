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
 *   Jun 14, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataColumnSpecCreator.MergeOptions;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.util.CheckUtils;

/**
 * Merges {@link DataColumnSpec DataColumnSpecs}.
 * Differs from {@link DataColumnSpecCreator#merge(DataColumnSpec, Set)} in the following ways
 * <ul>
 * <li>If only one bound is null, the other bound is used
 * <li>The possible values are dropped if they exceed the default {@link DataContainerSettings#getMaxDomainValues()}
 * </ul>
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DataColumnSpecMerger {

    private DataColumnSpecCreator m_specCreator;

    private DataCell m_lowerBound;

    private DataCell m_upperBound;

    private Set<DataCell> m_possibleValues = new LinkedHashSet<>();

    private final int m_maxPossibleValues;

    private final EnumSet<MergeOptions> m_mergeOptions;

    DataColumnSpecMerger(final EnumSet<MergeOptions> mergeOptions) {
        m_mergeOptions = mergeOptions;
        m_maxPossibleValues = DataContainerSettings.getDefault().getMaxDomainValues();
    }

    DataColumnSpecMerger merge(final DataColumnSpec columnSpec) {
        if (m_specCreator == null) {
            m_specCreator = new DataColumnSpecCreator(columnSpec);
            mergeDomain(columnSpec.getDomain());
        } else {
            // the spec merge has to happen first to update the type
            m_specCreator.merge(columnSpec, m_mergeOptions);
            mergeDomain(columnSpec.getDomain());
        }
        return this;
    }

    private void mergeDomain(final DataColumnDomain domain) {
        var comparator = m_specCreator.getType().getComparator();
        m_lowerBound = takeLarger(m_lowerBound, domain.getLowerBound(), comparator.reversed());
        m_upperBound = takeLarger(m_upperBound, domain.getUpperBound(), comparator);
        mergePossibleValues(domain.getValues());
    }

    private static DataCell takeLarger(final DataCell left, final DataCell right,
        final Comparator<DataCell> comparator) {
        if (left == null || right == null) {
            // if only one is null, use the other
            return left == null ? right : left;
        } else {
            return comparator.compare(left, right) >= 0 ? left : right;
        }
    }

    private void mergePossibleValues(final Set<DataCell> possibleValues) {
        if (m_possibleValues == null || possibleValues == null) {
            // null indicates that there are too many possible values
            m_possibleValues = null;
        } else {
            m_possibleValues.addAll(possibleValues);
            if (m_possibleValues.size() > m_maxPossibleValues) {
                m_possibleValues = null;
            }
        }
    }

    DataColumnSpec createMergedSpec() {
        CheckUtils.checkState(m_specCreator != null, "No columns have been merged.");
        var mergedDomain = new DataColumnDomainCreator(m_possibleValues, m_lowerBound, m_upperBound).createDomain();
        m_specCreator.setDomain(mergedDomain);
        return m_specCreator.createSpec();
    }

}