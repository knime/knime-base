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
 *   Aug 30, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.probability.ProbabilityDistributionValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SparseProbabilisticTrainingRowBuilder
    extends AbstractSparseClassificationTrainingRowBuilder<ProbabilityDistributionValue> {

    private final Map<String, Integer> m_classIdxMap = new HashMap<>();

    /**
     * @param data
     * @param pmmlSpec
     * @param targetReferenceCategory
     * @param sortTargetCategories
     * @param sortFactorsCategories
     * @param targetSpec the spec of the target column
     * @throws InvalidSettingsException
     */
    public SparseProbabilisticTrainingRowBuilder(final BufferedDataTable data, final PMMLPortObjectSpec pmmlSpec,
        final DataCell targetReferenceCategory, final boolean sortTargetCategories, final boolean sortFactorsCategories,
        final DataColumnSpec targetSpec) throws InvalidSettingsException {
        super(data, pmmlSpec, sortFactorsCategories, ProbabilityDistributionValue.class);
        final List<String> elementNames = targetSpec.getElementNames();
        CheckUtils.checkArgument(elementNames != null && !elementNames.isEmpty(),
            "A probability distribution column must always specify its classes.");
        @SuppressWarnings("null") // explicitly checked above
        final List<DataCell> classes = elementNames.stream().map(StringCell::new).collect(Collectors.toList());
        if (sortTargetCategories) {
            Collections.sort(classes, StringCell.TYPE.getComparator());
        }
        if (targetReferenceCategory != null) {
            // targetReferenceCategory must be the last element
            boolean removed = classes.remove(targetReferenceCategory);
            CheckUtils.checkSetting(removed, "The target reference category (\"%s\") is not found in the target column",
                targetReferenceCategory);
            classes.add(targetReferenceCategory);
        }
        classes.stream().map(c -> ((StringCell)c).getStringValue())
            .forEachOrdered(c -> m_classIdxMap.put(c, m_classIdxMap.size()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTargetDimension() {
        return m_classIdxMap.size() - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ClassificationTrainingRow create(final float[] values, final int[] nonZeroFeatures, final int id,
        final ProbabilityDistributionValue targetValue) {
        CheckUtils.checkArgument(targetValue.size() <= m_classIdxMap.size(),
            "Probability distribution with more than the expected number of classes encountered.");
        final double[] probabilities = new double[m_classIdxMap.size()];
        for (int i = 0; i < targetValue.size(); i++) {
            probabilities[i] = targetValue.getProbability(i);
        }
        return new SparseProbabilisticClassificationTrainingRow(values, nonZeroFeatures, id, probabilities);
    }

}
