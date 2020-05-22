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
 *   Mar 26, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGenerator;
import org.knime.filehandling.core.node.table.reader.rowkey.RowKeyGeneratorContext;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapper;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapping;
import org.knime.filehandling.core.node.table.reader.util.IndexMapper;
import org.knime.filehandling.core.node.table.reader.util.IndividualTableReader;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Default implementation of MultiTableRead.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the type representing values
 */
final class DefaultMultiTableRead<V> implements MultiTableRead<V> {

    private final String m_rootPath;

    private final DataTableSpec m_outputSpec;

    private final Map<Path, ? extends ReaderTableSpec<?>> m_individualSpecs;

    private final TypeMapping<V> m_typeMapping;

    private final RowKeyGeneratorContext<V> m_keyGenContext;

    DefaultMultiTableRead(final String rootPath, final Map<Path, ? extends ReaderTableSpec<?>> individualSpecs,
        final DataTableSpec outputSpec, final TypeMapping<V> typeMapping,
        final RowKeyGeneratorContext<V> keyGenContext) {
        m_rootPath = rootPath;
        m_outputSpec = outputSpec;
        m_individualSpecs = individualSpecs;
        m_typeMapping = typeMapping;
        m_keyGenContext = keyGenContext;
    }

    @Override
    public DataTableSpec getOutputSpec() {
        return m_outputSpec;
    }

    @Override
    public boolean isValidFor(final Collection<Path> paths) {
        return paths.size() == m_individualSpecs.size() && m_individualSpecs.keySet().containsAll(paths);
    }

    @Override
    public IndividualTableReader<V> createIndividualTableReader(final Path path, final TableReadConfig<?> config,
        final FileStoreFactory fsFactory) {
        CheckUtils.checkArgument(m_individualSpecs.containsKey(path), "Unknown path '%s' encountered.");
        final IndexMapper idxMapper =
            MultiTableUtils.createIndexMapper(m_outputSpec, m_individualSpecs.get(path), config);
        final TypeMapper<V> typeMapper = m_typeMapping.createTypeMapper(fsFactory);
        final RowKeyGenerator<V> keyGen = m_keyGenContext.createKeyGenerator(path);
        return new DefaultIndividualTableReader<>(typeMapper, idxMapper, keyGen);
    }

    @Override
    public TableSpecConfig createTableSpec() {
        return new TableSpecConfig(m_rootPath, m_outputSpec, m_individualSpecs, m_typeMapping.getProductionPaths());
    }

}
