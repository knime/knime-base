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
 *   Sep 20, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.webui.reader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TableSpecSettings;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.RawSpecFactory;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec.TypedReaderTableSpecBuilder;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class ReaderSpecific {

    public interface ConfigAndReader<C extends ReaderSpecificConfig<C>, T> {

        /**
         * ??? We need to use the AbstractMultiTableReadConfig as a type here because the MultiTableReadConfig does not
         * allow to set the TableReadConfig generic.
         */
        <S extends AbstractMultiTableReadConfig<C, DefaultTableReadConfig<C>, T, S>>
            AbstractMultiTableReadConfig<C, DefaultTableReadConfig<C>, T, S> getMultiTableReadConfig();

        <V> TableReader<C, T, V> getTableReader();
    }

    interface ExternalDataTypeSerializer<S, T> {

        S toSerializableType(T externalType);

        T toExternalType(S serializedType);

    }

    static <S, T> Map<String, TypedReaderTableSpec<T>> toSpecMap(final ExternalDataTypeSerializer<S, T> serializer,
        final List<TableSpecSettings<S>> specs) {
        final var individualSpecs = new LinkedHashMap<String, TypedReaderTableSpec<T>>();
        for (final var tableSpec : specs) {
            final TypedReaderTableSpecBuilder<T> specBuilder = TypedReaderTableSpec.builder();
            for (final var colSpec : tableSpec.m_spec) {
                specBuilder.addColumn(colSpec.m_name, serializer.toExternalType(colSpec.m_type), true);
            }
            final var spec = specBuilder.build();
            individualSpecs.put(tableSpec.m_sourceId, spec);
        }
        return individualSpecs;
    }

    public interface ProductionPathProviderAndTypeHierarchy<T> {

        ProductionPathProvider<T> getProductionPathProvider();

        TypeHierarchy<T, T> getTypeHierarchy();

        default RawSpec<T> toRawSpec(final Map<String, TypedReaderTableSpec<T>> spec) {
            if (spec.isEmpty()) {
                final var emptySpec = new TypedReaderTableSpec<T>();
                return new RawSpec<>(emptySpec, emptySpec);
            }
            return new RawSpecFactory<>(getTypeHierarchy()).create(spec.values());
        }
    }

    private ReaderSpecific() {
        // Utility class
    }
}
