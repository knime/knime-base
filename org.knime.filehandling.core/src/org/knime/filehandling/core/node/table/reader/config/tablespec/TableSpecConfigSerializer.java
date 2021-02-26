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
 *   Feb 3, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import java.util.EnumMap;
import java.util.Optional;

import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;

/**
 * Serializer for {@link TableSpecConfig} objects.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> type used to identify external data types
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface TableSpecConfigSerializer<T> extends NodeSettingsSerializer<TableSpecConfig<T>> {

    /**
     * Creates a {@link TableSpecConfigSerializer} for settings created with KNIME AP 4.4 or later.
     *
     * @param <T> the type used to identify external data types
     * @param productionPathSerializer used to serializes {@link ProductionPath ProductionPaths}
     * @param configIDLoader loads the {@link ConfigID}
     * @param typeSerializer serializes the external type
     * @return a {@link TableSpecConfigSerializer} that supports settings created with KNIME AP 4.4 and onwards
     */
    static <T> TableSpecConfigSerializer<T> createStartingV44(final ProductionPathSerializer productionPathSerializer,
        final ConfigIDLoader configIDLoader, final NodeSettingsSerializer<T> typeSerializer) {
        final EnumMap<TableSpecConfigSerializerVersion, TableSpecConfigSerializer<T>> serializers =
            new EnumMap<>(TableSpecConfigSerializerVersion.class);
        serializers.put(TableSpecConfigSerializerVersion.V4_4,
            new V44TableSpecConfigSerializer<>(productionPathSerializer, configIDLoader, typeSerializer));
        return new VersionedTableSpecConfigSerializer<>(serializers, TableSpecConfigSerializerVersion.V4_4);
    }

    /**
     * Creates a {@link TableSpecConfigSerializer} for settings created with KNIME AP 4.3 or later.
     *
     * @param <T> the type used to identify external data types
     * @param productionPathSerializer used to serializes {@link ProductionPath ProductionPaths}
     * @param configIDLoader loads the {@link ConfigID}
     * @param typeSerializer serializes the external type
     * @return a {@link TableSpecConfigSerializer} that supports settings created with KNIME AP 4.3 and onwards
     */
    static <T> TableSpecConfigSerializer<T> createStartingV43(final ProductionPathSerializer productionPathSerializer,
        final ConfigIDLoader configIDLoader, final NodeSettingsSerializer<T> typeSerializer) {
        final EnumMap<TableSpecConfigSerializerVersion, TableSpecConfigSerializer<T>> serializers =
            new EnumMap<>(TableSpecConfigSerializerVersion.class);
        serializers.put(TableSpecConfigSerializerVersion.V4_4,
            new V44TableSpecConfigSerializer<>(productionPathSerializer, configIDLoader, typeSerializer));
        serializers.put(TableSpecConfigSerializerVersion.V4_3,
            new V43TableSpecConfigSerializer<>(productionPathSerializer));
        return new VersionedTableSpecConfigSerializer<>(serializers, TableSpecConfigSerializerVersion.V4_4);
    }

    /**
     * Creates a {@link TableSpecConfigSerializer} for settings created with KNIME AP 4.2 or later.
     *
     * @param <T> the type used to identify external data types
     * @param producerRegistry the {@link ProducerRegistry} used to serialize {@link ProductionPath ProductionPaths}
     * @param configIDLoader loads the {@link ConfigID}
     * @param typeSerializer serializes the external type
     * @param mostGenericType the most generic external type the reader supports, typically String
     * @return a {@link TableSpecConfigSerializer} that supports settings created with KNIME AP 4.2 and onwards
     */
    static <T> TableSpecConfigSerializer<T> createStartingV42(final ProducerRegistry<T, ?> producerRegistry,
        final ConfigIDLoader configIDLoader, final NodeSettingsSerializer<T> typeSerializer, final T mostGenericType) {
        final ProductionPathSerializer productionPathSerializer = new DefaultProductionPathSerializer(producerRegistry);
        final EnumMap<TableSpecConfigSerializerVersion, TableSpecConfigSerializer<T>> serializers =
            new EnumMap<>(TableSpecConfigSerializerVersion.class);
        serializers.put(TableSpecConfigSerializerVersion.V4_4,
            new V44TableSpecConfigSerializer<>(productionPathSerializer, configIDLoader, typeSerializer));
        serializers.put(TableSpecConfigSerializerVersion.V4_3,
            new V43TableSpecConfigSerializer<>(productionPathSerializer));
        serializers.put(TableSpecConfigSerializerVersion.V4_2,
            new V42TableSpecConfigSerializer<>(producerRegistry, mostGenericType));
        return new VersionedTableSpecConfigSerializer<>(serializers, TableSpecConfigSerializerVersion.V4_4);
    }

    /**
     * Load method that allows to pass additional parameters such as the skip empty columns option.<br>
     * If you don't want or need to pass any additional parameters, use {@link #load(NodeSettingsRO)} instead.
     *
     * @param settings to load from
     * @param additionalParameters holds the additional parameters
     * @return the loaded {@link TableSpecConfig}
     * @throws InvalidSettingsException if the settings are invalid e.g. of the wrong version
     * @see #load(NodeSettingsRO)
     */
    TableSpecConfig<T> load(final NodeSettingsRO settings, final AdditionalParameters additionalParameters)
        throws InvalidSettingsException;

    /**
     * Enum of versions where the TableSpecConfig serialization changed.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    enum TableSpecConfigSerializerVersion {
            /**
             * Version 4.4.0. This is the version where this enum was first introduced. This version also introduced the
             * skipEmptyColumns option and separated storing of the ProductionPaths from storing the individual specs.
             */
            V4_4,
            /**
             * Version 4.3.0. This version introduced TableTransformations
             */
            V4_3,
            /**
             * Version 4.2.0. Introduced TableSpecConfig.
             */
            V4_2;
    }

    /**
     * Encapsulates additional parameters for the
     * {@link TableSpecConfigSerializer#load(NodeSettingsRO, AdditionalParameters)} method.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class AdditionalParameters {

        private ColumnFilterMode m_columnFilterMode = null;

        private AdditionalParameters() {
        }

        public static AdditionalParameters create() {
            return new AdditionalParameters();
        }

        Optional<ColumnFilterMode> getColumnFilterMode() {
            return Optional.ofNullable(m_columnFilterMode);
        }

        /**
         * Sets the {@link ColumnFilterMode} to use.</br>
         * Only needed and used for loading nodes that were saved in KNIME AP 4.2.x.
         *
         * @param columnFilterMode the {@link ColumnFilterMode} to use
         * @return this builder
         */
        public AdditionalParameters withColumnFilterMode(final ColumnFilterMode columnFilterMode) {
            m_columnFilterMode = columnFilterMode;
            return this;
        }

    }

}