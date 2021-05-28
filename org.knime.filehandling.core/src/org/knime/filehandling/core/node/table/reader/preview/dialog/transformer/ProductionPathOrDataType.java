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
 *   May 28, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.util.Objects;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;

/**
 * Placeholder for either a ProductionPath or a DataType.
 * Used in the transformation tab for the type mapping column.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ProductionPathOrDataType {

    static final ProductionPathOrDataType DEFAULT = new ProductionPathOrDataType(null, null);

    private final ProductionPath m_productionPath;

    private final DataType m_dataType;

    ProductionPathOrDataType(final ProductionPath productionPath) {
        this(productionPath, null);
    }

    ProductionPathOrDataType(final DataType dataType) {
        this(null, dataType);
    }

    private ProductionPathOrDataType(final ProductionPath productionPath, final DataType dataType) {
        m_productionPath = productionPath;
        m_dataType = dataType;
    }

    boolean hasProductionPath() {
        return m_productionPath != null;
    }

    ProductionPath getProductionPath() {
        return m_productionPath;
    }

    boolean hasDataType() {
        return this != DEFAULT;
    }

    DataType getDataType() {
        if (m_productionPath != null) {
            return m_productionPath.getDestinationType();
        } else {
            return m_dataType;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_dataType == null) ? 0 : m_dataType.hashCode());
        result = prime * result + ((m_productionPath == null) ? 0 : m_productionPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() == obj.getClass()) {
            ProductionPathOrDataType other = (ProductionPathOrDataType)obj;
            return Objects.equals(m_dataType, other.m_dataType)
                && Objects.equals(m_productionPath, other.m_productionPath);
        }
        return false;
    }

}
