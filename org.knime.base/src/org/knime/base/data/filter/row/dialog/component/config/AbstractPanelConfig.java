/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.filter.row.dialog.component.config;

import java.util.List;

import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.base.data.filter.row.dialog.registry.OperatorRegistry;
import org.knime.core.data.DataTableSpec;

/**
 * Base configuration class for the the {@link EditorPanelConfig} and the {@link TreePanelConfig}.
 *
 * @author Viktor Buria
 * @since 4.0
 */
public abstract class AbstractPanelConfig {//NOSONAR

    private DataTableSpec m_dataTableSpec;

    private List<ColumnSpec> m_columnSpecList;

    private OperatorRegistry<?> m_operatorRegistry;

    /**
     * Gets a table specification.
     *
     * @return the {@link DataTableSpec}
     */
    public DataTableSpec getDataTableSpec() {
        return m_dataTableSpec;
    }

    /**
     * Sets a table specification.
     *
     * @param tableSpec the {@link DataTableSpec}
     */
    public void setDataTableSpec(final DataTableSpec tableSpec) {
        m_dataTableSpec = tableSpec;
    }

    /**
     * @param columnSpecList holds the list of Column Specs from the Data Table Spec
     */
    public void setColumnSpecList(final List<ColumnSpec> columnSpecList) {
        m_columnSpecList = columnSpecList;
    }

    /**
     * @return the list of Column Specs
     */
    public List<ColumnSpec> getColumnSpecList() {
        return m_columnSpecList;
    }

    /**
     * Gets an operator registry.
     *
     * @return the {@link OperatorRegistry}
     */
    public OperatorRegistry<?> getOperatorRegistry() { //NOSONAR
        return m_operatorRegistry;
    }

    /**
     * Sets an operator registry.
     *
     * @param operatorRegistry the {@link OperatorRegistry}
     */
    public void setOperatorRegistry(final OperatorRegistry<?> operatorRegistry) {
        m_operatorRegistry = operatorRegistry;
    }
}
