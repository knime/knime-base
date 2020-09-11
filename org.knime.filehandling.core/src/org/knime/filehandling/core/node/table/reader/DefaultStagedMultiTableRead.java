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
 *   Aug 3, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.selector.TransformationModel;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapping;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMappingFactory;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 * Default implementation of a {@link StagedMultiTableRead}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type representing external types
 * @param <V> the type representing values
 */
final class DefaultStagedMultiTableRead<C extends ReaderSpecificConfig<C>, T, V> implements StagedMultiTableRead<T> {

    private final Map<Path, ? extends ReaderTableSpec<?>> m_individualSpecs;

    private final String m_rootPath;

    private final TypedReaderTableSpec<T> m_rawSpec;

    private final RowKeyGeneratorContextFactory<V> m_rowKeyGenFactory;

    private final TypeMappingFactory<C, T, V> m_defaultTypeMapping;

    private final TableReadConfig<C> m_tableReadConfig;

    private final TransformationModel<T> m_defaultTransformation;

    private final TableReader<C, T, V> m_reader;

    DefaultStagedMultiTableRead(final TableReader<C, T, V> reader, final String rootPath,
        final Map<Path, ? extends ReaderTableSpec<?>> individualSpecs,
        final RowKeyGeneratorContextFactory<V> rowKeyGenFactory, final TypeMappingFactory<C, T, V> typeMappingFactory,
        final TransformationModel<T> defaultTransformation, final TableReadConfig<C> tableReadConfig) {
        m_rawSpec = defaultTransformation.getRawSpec();
        m_rootPath = rootPath;
        m_individualSpecs = individualSpecs;
        m_rowKeyGenFactory = rowKeyGenFactory;
        m_tableReadConfig = tableReadConfig;
        m_defaultTransformation = defaultTransformation;
        m_defaultTypeMapping = typeMappingFactory;
        m_reader = reader;
    }

    @Override
    public MultiTableRead withoutTransformation() {
        return withTransformation(m_defaultTransformation);
    }

    @Override
    public MultiTableRead withTransformation(final TransformationModel<T> transformationModel) {
        final TypedReaderTableSpec<T> filtered = transformRawSpec(transformationModel);
        final TypeMapping<V> typeMapping =
            m_defaultTypeMapping.create(filtered, m_tableReadConfig.getReaderSpecificConfig(), transformationModel);
        final DataTableSpec knimeSpec = typeMapping.map(filtered);
        final Map<Path, IndexMapper> indexMappers = m_individualSpecs.entrySet()//
            .stream()//
            .collect(Collectors.toMap(//
                Entry::getKey, //
                e -> MultiTableUtils.createIndexMapper(knimeSpec, e.getValue(), m_tableReadConfig), //
                (x, y) -> x, // never needed
                LinkedHashMap::new));
        final TableSpecConfig tableSpecConfig =
            DefaultTableSpecConfig.createFromTransformationModel(m_rootPath, m_individualSpecs, transformationModel);
        return new DefaultMultiTableRead<>(p -> createRead(p, m_tableReadConfig), tableSpecConfig, typeMapping,
            m_rowKeyGenFactory.createContext(m_tableReadConfig), indexMappers);
    }

    // The rawRead will be closed by the decoratedRead which in turn needs to be closed by the caller
    @SuppressWarnings("resource")
    private Read<V> createRead(final Path path, final TableReadConfig<C> config) throws IOException {
        final Read<V> rawRead = m_reader.read(path, config);
        return ReadUtils.decorateForReading(rawRead, config);
    }

    private TypedReaderTableSpec<T> transformRawSpec(final TransformationModel<T> transformation) {
        final List<TypedReaderColumnSpec<T>> columns = m_rawSpec.stream()//
            .filter(transformation::keep)//
            .sorted((c1, c2) -> Integer.compare(transformation.getPosition(c1), transformation.getPosition(c2)))//
            .map(c -> TypedReaderColumnSpec.createWithName(transformation.getName(c), c.getType(), true))//
            .collect(toList());
        return new TypedReaderTableSpec<>(columns);
    }

    @Override
    public TypedReaderTableSpec<T> getRawSpec() {
        return m_rawSpec;
    }

    @Override
    public boolean isValidFor(final Collection<Path> paths) {
        return paths.size() == m_individualSpecs.size() && m_individualSpecs.keySet().containsAll(paths);
    }

}
