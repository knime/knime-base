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
 *   Oct 19, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.util;

import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.getNameAfterInit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.TransformationModel;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.DefaultIndexMapper.DefaultIndexMapperBuilder;

/**
 * Factory for {@link IndexMapper} that is initialized with a particular {@link TransformationModel}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class IndexMapperFactory {

    private final Map<String, Integer> m_nameToIdx;

    private final Supplier<DefaultIndexMapperBuilder> m_builderSupplier;

    /**
     * Constructor.
     *
     * @param originalNamesInOutput the original names that are included in the output (in order)
     * @param config the {@link TableReadConfig} for the current read
     */
    public IndexMapperFactory(final List<String> originalNamesInOutput, final TableReadConfig<?> config) {
        m_nameToIdx = createNameToIndexMap(originalNamesInOutput);
        final int outputSize = m_nameToIdx.size();
        m_builderSupplier =
            config.useRowIDIdx() ? () -> DefaultIndexMapper.builder(outputSize).setRowIDIdx(config.getRowIDIdx())
                : () -> DefaultIndexMapper.builder(outputSize);
    }

    private static Map<String, Integer> createNameToIndexMap(final List<String> originalNamesInOutput) {
        final Map<String, Integer> nameToIdx = new HashMap<>(originalNamesInOutput.size());
        int idx = 0;
        for (String column : originalNamesInOutput) {
            nameToIdx.put(column, idx);
            idx++;
        }
        return nameToIdx;
    }

    /**
     * Creates an {@link IndexMapper} for the provided {@link ReaderTableSpec}.
     *
     * @param individualSpec the {@link ReaderTableSpec} for which to create the {@link IndexMapper}
     * @return the {@link IndexMapper} for {@link ReaderTableSpec individualSpec}
     */
    public IndexMapper createIndexMapper(final ReaderTableSpec<?> individualSpec) {
        final DefaultIndexMapperBuilder builder = m_builderSupplier.get();
        for (int i = 0; i < individualSpec.size(); i++) {
            final ReaderColumnSpec columnSpec = individualSpec.getColumnSpec(i);
            final String name = getNameAfterInit(columnSpec);
            Integer outputIdx = m_nameToIdx.get(name);
            if (outputIdx != null) {
                builder.addMapping(outputIdx, i);
            }
        }
        return builder.build();
    }

}
