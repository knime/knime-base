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
 *   27.04.2019 (Adrian): created
 */
package org.knime.base.node.mine.regression.glmnet.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.base.node.mine.regression.glmnet.data.Feature.FeatureIterator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ModularDataBuilder {

    private final List<FeatureBuilder> m_featureBuilders;

    private FeatureBuilder m_targetBuilder;

    private FeatureBuilder m_weightBuilder;

    private final BufferedDataTable m_table;


    /**
     * Constructor for a ModularDataBuilder without a weight column.
     *
     * @param table
     * @param targetIdx
     */
    public ModularDataBuilder(final BufferedDataTable table, final int targetIdx) {
        this(table, targetIdx, -1);
    }

    /**
     * @param table table spec containing only relevant columns i.e. all features and the target column
     * @param targetIdx the idx of the target column in <b>tableSpec</b>
     * @param weightIdx the idx of the weight column in <b>tableSpec</b>
     *
     */
    public ModularDataBuilder(final BufferedDataTable table, final int targetIdx, final int weightIdx) {
        m_featureBuilders = new ArrayList<>();
        final int numRows = getLongAsInt(table.size());
        m_table = table;
        final DataTableSpec tableSpec = table.getDataTableSpec();
        for (int i = 0; i < tableSpec.getNumColumns(); i++) {
            final DataColumnSpec colSpec = tableSpec.getColumnSpec(i);
            final DataType type = colSpec.getType();
            if (i == targetIdx) {
                CheckUtils.checkState(type.isCompatible(DoubleValue.class),
                    "Only numeric targets are currently supported.");
                m_targetBuilder = createBuilder(type, i, numRows);
            } else if (i == weightIdx) {
                CheckUtils.checkState(type.isCompatible(DoubleValue.class),
                    "Only numeric columns can be used as weights.");
                m_weightBuilder = createBuilder(type, i, numRows);
            } else {
                m_featureBuilders.add(createBuilder(type, i, numRows));
            }
        }
    }

    private static int getLongAsInt(final long size) {
        CheckUtils.checkArgument(size <= Integer.MAX_VALUE,
            "Only tables with up to Integer.MAX_VALUE rows are supported.");
        return (int)size;
    }

    private static FeatureBuilder createBuilder(final DataType type, final int colIdx, final int numRows) {
        if (type.isCompatible(DoubleValue.class)) {
            return new NumericFeatureBuilder(colIdx, numRows);
        } else if (type.isCompatible(NominalValue.class)) {
            return new NominalFeatureBuilder(colIdx, numRows);
        } else {
            throw new IllegalArgumentException("The data type '" + type + "' is not supported.");
        }
    }

    /**
     * Builds the data from the table provided in the constructor.
     * @return the {@link Data} object
     */
    public Data build() {
        feedBuilders();
        return new DefaultData(getFeatures(), getTarget(), getWeightContainer());
    }

    private Feature[] getFeatures() {
        return m_featureBuilders.stream().flatMap(b -> b.build().stream()).toArray(Feature[]::new);
    }

    private double[] getTarget() {
        final Collection<Feature> targets = m_targetBuilder.build();
        // TODO extend to multiple targets
        CheckUtils.checkState(targets.size() == 1, "Only exactly one target is supported.");
        final Feature targetFeature = targets.iterator().next();
        return toArray(targetFeature);
    }

    private double[] toArray(final Feature feature) {
        final double[] array = new double[(int)m_table.size()];
        final FeatureIterator iter = feature.getIterator();
        while (iter.next()) {
            array[iter.getRowIdx()] = iter.getValue();
        }
        return array;
    }

    private WeightContainer getWeightContainer() {
        if (m_weightBuilder != null) {
            return new VariableWeightContainer(getWeights(), false);
        } else {
            return new ConstantWeightContainer((int)m_table.size());
        }
    }

    private double[] getWeights() {
        final Collection<Feature> weights = m_weightBuilder.build();
        CheckUtils.checkState(weights.size() == 1, "Only exactly one weight column is supported.");
        final double[] weightArray = toArray(weights.iterator().next());
        float totalWeight = 0.0f;
        for (int i = 0; i < weightArray.length; i++) {
            totalWeight += weightArray[i];
        }
        CheckUtils.checkArgument(totalWeight > 0, "Weights must be larger than 0");
        for (int i = 0; i < weightArray.length; i++) {
            // we also check above if this is satisfied and throw an exception if not
            assert totalWeight > 0;
            weightArray[i] /= totalWeight;
        }
        return weightArray;
    }

    /**
     *
     */
    private void feedBuilders() {
        for (final DataRow row : m_table) {
            if (m_weightBuilder != null) {
                m_weightBuilder.accept(row);
            }
            m_targetBuilder.accept(row);
            m_featureBuilders.forEach(b -> b.accept(row));
        }
    }

}
