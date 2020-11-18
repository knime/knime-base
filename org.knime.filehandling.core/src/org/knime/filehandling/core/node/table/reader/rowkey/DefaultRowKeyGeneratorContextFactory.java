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
 *   Mar 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.rowkey;

import java.util.function.Function;

import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;

/**
 * Default implementation of {@link GenericRowKeyGeneratorContextFactory}.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <V> the type of values
 * @noinstantiate non-public API
 * @noreference non-public API
 */
public final class DefaultRowKeyGeneratorContextFactory<I, V> implements GenericRowKeyGeneratorContextFactory<I, V> {

    private final Function<V, String> m_rowKeyExtractor;

    private final String m_sourcePrefix;

    /**
     * Constructor.
     *
     * @param rowKeyExtractor for extracting row keys from values
     * @param sourcePrefix prefix for the source index
     */
    public DefaultRowKeyGeneratorContextFactory(final Function<V, String> rowKeyExtractor, final String sourcePrefix) {
        m_rowKeyExtractor = rowKeyExtractor;
        m_sourcePrefix = sourcePrefix;
    }

    @Override
    public GenericRowKeyGeneratorContext<I, V> createContext(final TableReadConfig<?> config) {
        if (config.useRowIDIdx()) {
            return new ExtractingRowKeyGeneratorContext<>(m_rowKeyExtractor, config.getRowIDIdx(), m_sourcePrefix,
                config.prependSourceIdxToRowID());
        } else {
            return new ContinuousCountingRowKeyGeneratorContext<>(config.getPrefixForGeneratedRowIDs());
        }
    }
}
