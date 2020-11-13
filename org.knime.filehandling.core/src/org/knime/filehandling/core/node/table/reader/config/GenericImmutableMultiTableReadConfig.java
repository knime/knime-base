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
 *   Nov 15, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import java.util.List;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;

/**
 * An immutable implementation of {@link MultiTableReadConfig} i.e. objects of this class guarantee that their state
 * doesn't change after initialization.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <C> the type of {@link ReaderSpecificConfig}
 */
public class GenericImmutableMultiTableReadConfig<I, C extends ReaderSpecificConfig<C>>
    implements GenericMultiTableReadConfig<I, C> {

    private final ImmutableTableReadConfig<C> m_tableReadConfig;

    private final GenericTableSpecConfig<I> m_tableSpecConfig;

    private final boolean m_failOnDifferingSpecs;

    private final SpecMergeMode m_specMergeMode;

    /**
     * Constructor.
     * @param multiTableReadConfig {@link GenericMultiTableReadConfig}
     */
    public GenericImmutableMultiTableReadConfig(final GenericMultiTableReadConfig<I, C> multiTableReadConfig) {
        CheckUtils.checkArgumentNotNull(multiTableReadConfig, "The multiTableReadConfig parameter must not be null");
        m_tableReadConfig = new ImmutableTableReadConfig<>(multiTableReadConfig.getTableReadConfig());
        m_tableSpecConfig =
            multiTableReadConfig.hasTableSpecConfig() ? multiTableReadConfig.getTableSpecConfig() : null;
        m_failOnDifferingSpecs = multiTableReadConfig.failOnDifferingSpecs();
        @SuppressWarnings("deprecation") // yes but we still need it for backwards compatibility
        SpecMergeMode specMergeMode = multiTableReadConfig.getSpecMergeMode();//NOSONAR
        m_specMergeMode = specMergeMode;
    }

    @Override
    public TableReadConfig<C> getTableReadConfig() {
        return m_tableReadConfig;
    }

    @Override
    public boolean hasTableSpecConfig() {
        return m_tableSpecConfig != null;
    }

    @Override
    public GenericTableSpecConfig<I> getTableSpecConfig() {
        return m_tableSpecConfig;
    }

    @Override
    public void setTableSpecConfig(final GenericTableSpecConfig<I> config) {
        throw new UnsupportedOperationException("ImmutableMultiTableReadConfigs can't be mutated.");
    }

    @Override
    public boolean failOnDifferingSpecs() {
        return m_failOnDifferingSpecs;
    }

    /**
     * @deprecated only used as fallback for old nodes
     */
    @Deprecated
    @Override
    public SpecMergeMode getSpecMergeMode() {
        return m_specMergeMode;
    }

    @Override
    public boolean isConfiguredWith(final String rootItem, final List<I> items) {
        return hasTableSpecConfig() && getTableSpecConfig().isConfiguredWith(rootItem, items);
    }

}