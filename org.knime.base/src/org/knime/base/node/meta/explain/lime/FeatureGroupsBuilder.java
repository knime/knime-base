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
 *   Apr 29, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.lime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;

import org.knime.base.node.meta.explain.lime.node.LIMESettings;
import org.knime.base.node.meta.explain.util.DefaultRandomDataGeneratorFactory;
import org.knime.base.node.meta.explain.util.RandomDataGeneratorFactory;
import org.knime.base.node.meta.explain.util.valueaccess.ValueAccessors;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.bytevector.DenseByteVectorCellFactory;
import org.knime.core.node.util.CheckUtils;

/**
 * Allows to build the feature groups for columns of a table.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class FeatureGroupsBuilder {

    private final SamplerFactory m_samplerFactory;

    private final Collection<FeatureGroupBuilder> m_builders;

    private final NormalizerFactory<NumericFeatureStatistic> m_normalizerFactory;

    private final RandomDataGeneratorFactory m_rdgFactory;

    /**
     * Constructs a FeatureGroupsBuilder that can consume an arbitrary number of rows
     * to create FeatureGroups.
     *
     * @param settings node settings
     * @param spec must only contain the feature columns
     */
    FeatureGroupsBuilder(final LIMESettings settings, final DataTableSpec spec) {
        m_rdgFactory = new DefaultRandomDataGeneratorFactory(settings.getSeed());
        m_samplerFactory = new DefaultSamplerFactory(m_rdgFactory, settings.isSampleAroundInstance());
        m_normalizerFactory = DefaultNormalizerFactory.INSTANCE;
        m_builders = createBuilders(spec);
    }

    /**
     * Updates the feature groups with the values in {@link DataRow row}.
     *
     * @param row contains only feature columns
     */
    void consume(final Iterable<DataCell> row) {
        final Iterator<DataCell> cellIterator = row.iterator();
        final Iterator<FeatureGroupBuilder> builderIterator = m_builders.iterator();
        while (cellIterator.hasNext()) {
            CheckUtils.checkState(builderIterator.hasNext(), "More cells than builders detected.");
            final DataCell cell = cellIterator.next();
            final FeatureGroupBuilder builder = builderIterator.next();
            builder.consume(cell);
        }
        CheckUtils.checkState(!builderIterator.hasNext(), "Fewer cells than builders detected.");
    }

    /**
     * Should only be called once the whole sampling table is read.
     *
     * @return the FeatureGroups
     */
    Iterable<FeatureGroup> build() {
        final Collection<FeatureGroup> groups = new ArrayList<>(m_builders.size());
        for (FeatureGroupBuilder builder : m_builders) {
            groups.add(builder.build());
        }
        return groups;
    }

    private Collection<FeatureGroupBuilder> createBuilders(final DataTableSpec spec) {
        final Collection<FeatureGroupBuilder> builders = new ArrayList<>(spec.getNumColumns());
        for (DataColumnSpec colSpec : spec) {
            builders.add(createBuilder(colSpec.getType()));
        }
        return builders;
    }

    private FeatureGroupBuilder createBuilder(final DataType type) {
        if (type.isCompatible(NominalValue.class)) {
            return new NominalFeatureGroupBuilder(m_samplerFactory);
        } else if (type.isCompatible(DoubleValue.class)) {
            return new NumericFeatureGroupBuilder(m_samplerFactory, m_normalizerFactory, getInverseFunction(type),
                DoubleCell::new);
        } else if (type.isCompatible(ByteVectorValue.class)) {
            return new NumericVectorFeatureGroupBuilder(m_samplerFactory, m_normalizerFactory,
                VectorHandlers.getVectorHandler(type), ValueAccessors::getByteVectorValueAccessor,
                FeatureGroupsBuilder::byteVectorInverse, FeatureGroupsBuilder::vectorToData);
        } else if (type.isCompatible(BitVectorValue.class)) {
            return new NumericVectorFeatureGroupBuilder(m_samplerFactory, m_normalizerFactory,
                VectorHandlers.getVectorHandler(type), ValueAccessors::getBitVectorValueAccessor,
                FeatureGroupsBuilder::bitVectorInverse, FeatureGroupsBuilder::vectorToData);
        } else {
            throw createUnsupportedTypeException(type);
        }
    }

    private static DoubleFunction<DataCell> getInverseFunction(final DataType type) {
        if (type.isCompatible(IntValue.class)) {
            return d -> new IntCell((int)Math.round(d));
        } else if (type.isCompatible(LongValue.class)) {
            return d -> new LongCell(Math.round(d));
        } else if (type.isCompatible(DoubleValue.class)) {
            return DoubleCell::new;
        } else {
            throw createUnsupportedTypeException(type);
        }
    }

    private static DataCell byteVectorInverse(final double[] values) {
        final DenseByteVectorCellFactory factory = new DenseByteVectorCellFactory(values.length);
        for (int i = 0; i < values.length; i++) {
            factory.setValue(i, toByte(values[i]));
        }
        return factory.createDataCell();
    }

    private static DataCell bitVectorInverse(final double[] values) {
        final DenseBitVectorCellFactory factory = new DenseBitVectorCellFactory(values.length);
        for (int i = 0; i < values.length; i++) {
            factory.set(i, toBoolean(values[i]));
        }
        return factory.createDataCell();
    }

    private static Iterable<DataCell> vectorToData(final double[] values) {
        return Arrays.stream(values).mapToObj(DoubleCell::new)
            .collect(Collectors.toCollection(ArrayList<DataCell>::new));
    }

    private static int toByte(final double value) {
        long rounded = Math.round(value);
        if (rounded < 0) {
            return 0;
        } else if (rounded > 255) {
            return 255;
        } else {
            return (int)rounded;
        }
    }

    private static boolean toBoolean(final double value) {
        long rounded = Math.round(value);
        return rounded > 0;
    }

    private static IllegalArgumentException createUnsupportedTypeException(final DataType type) {
        return new IllegalArgumentException("Unsupported type '" + type + "'.");
    }

}
