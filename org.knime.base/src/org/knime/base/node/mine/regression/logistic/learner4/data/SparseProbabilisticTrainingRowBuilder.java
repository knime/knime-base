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

import java.util.Collection;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 * Instances of this class can be used to create SparseProbabilisticClassificationTrainingRows
 * from a {@link BufferedDataTable}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SparseProbabilisticTrainingRowBuilder
    extends AbstractSparseClassificationTrainingRowBuilder<NominalDistributionValue> {

    private final String[] m_classes;

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
        super(data, pmmlSpec, sortFactorsCategories, NominalDistributionValue.class);
        Collection<String> values = NominalDistributionValueMetaData.extractFromSpec(targetSpec).getValues();
        Stream<DataCell> valueStream = values.stream().map(StringCell::new);
        if (sortTargetCategories) {
            valueStream = valueStream.sorted(StringCell.TYPE.getComparator());
        }
        if (targetReferenceCategory != null) {
            // targetReferenceCategory must be the last element
            valueStream = Stream.concat(valueStream.filter(c -> !c.equals(targetReferenceCategory)),
                Stream.of(targetReferenceCategory));
        }
        m_classes = valueStream.map(DataCell::toString).toArray(String[]::new);
        CheckUtils.checkSetting(m_classes.length == values.size(),
            "The target reference category (\"%s\") is not found in the target column", targetReferenceCategory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTargetDimension() {
        return m_classes.length - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ClassificationTrainingRow create(final float[] values, final int[] nonZeroFeatures, final int id,
        final NominalDistributionValue targetValue) {
        final double[] classProbabilities = new double[m_classes.length];
        int maxIdx = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < classProbabilities.length; i++) {
            final double p = targetValue.getProbability(m_classes[i]);
            classProbabilities[i] = p;
            if (p > max) {
                max = p;
                maxIdx = i;
            }
        }
        return new SparseProbabilisticClassificationTrainingRow(values, nonZeroFeatures, id, maxIdx,
            classProbabilities);
    }

}
