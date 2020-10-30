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
 *   Oct 20, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;

/**
 * A immutable {@link ColumnTransformation}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> Type used to identify external data types
 */
public final class ImmutableColumnTransformation<T> implements ColumnTransformation<T> {

    private final ProductionPath m_productionPath;

    private final boolean m_keep;

    private final int m_position;

    private final String m_name;

    private final TypedReaderColumnSpec<T> m_externalSpec;

    /**
     * Constructor.
     *
     * @param externalSpec the external {@link TypedReaderColumnSpec}
     * @param productionPath the {@link ProductionPath}
     * @param keep whether to keep the column
     * @param position in the output
     * @param name in the output
     */
    public ImmutableColumnTransformation(final TypedReaderColumnSpec<T> externalSpec,
        final ProductionPath productionPath, final boolean keep, final int position, final String name) {
        m_externalSpec = externalSpec;
        m_productionPath = productionPath;
        m_keep = keep;
        m_position = position;
        m_name = name;
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public ProductionPath getProductionPath() {
        return m_productionPath;
    }

    @Override
    public boolean keep() {
        return m_keep;
    }

    @Override
    public int getPosition() {
        return m_position;
    }

    @Override
    public TypedReaderColumnSpec<T> getExternalSpec() {
        return m_externalSpec;
    }

    @Override
    public String toString() {
        return String.format("{ExternalSpec: %s, name: %s, position: %s, ProductionPath: %s, keep: %s}", m_externalSpec,
            m_name, m_position, m_productionPath, m_keep);
    }
}