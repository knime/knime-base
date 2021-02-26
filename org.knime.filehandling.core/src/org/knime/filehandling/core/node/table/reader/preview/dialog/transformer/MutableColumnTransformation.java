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
 *   Oct 8, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.util.Objects;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;

/**
 * A mutable {@link ColumnTransformation}.</br>
 * Provides setters for the different properties that return a boolean indicating whether the value changed or not.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
class MutableColumnTransformation<T> implements ColumnTransformation<T> {

    private boolean m_isValid = true;

    private final DataColumnSpec m_defaultSpec;

    private int m_originalPosition;

    private String m_name;

    private ProductionPath m_productionPath;

    private int m_positionInOutput;

    private boolean m_keep;

    private final TypedReaderColumnSpec<T> m_colSpec;

    MutableColumnTransformation(final DataColumnSpec defaultSpec, final TypedReaderColumnSpec<T> colSpec,
        final int originalPosition, final String name, final ProductionPath productionPath, final int positionInOutput,
        final boolean keep) {
        m_defaultSpec = defaultSpec;
        m_originalPosition = originalPosition;
        m_name = name;
        m_productionPath = productionPath;
        m_positionInOutput = positionInOutput;
        m_keep = keep;
        m_colSpec = colSpec;
    }

    DataColumnSpec getDefaultSpec() {
        return m_defaultSpec;
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
    public int getPosition() {
        return m_positionInOutput;
    }

    @Override
    public boolean keep() {
        return m_keep;
    }

    @Override
    public TypedReaderColumnSpec<T> getExternalSpec() {
        return m_colSpec;
    }

    boolean isRenamed() {
        return !getOriginalName().equals(getName());
    }

    boolean setPosition(final int position) {
        if (position != m_positionInOutput) {
            m_positionInOutput = position;
            return true;
        }
        return false;
    }

    boolean setOriginalPosition(final int originalPosition) {
        if (originalPosition != m_originalPosition) {
            m_originalPosition = originalPosition;
            return true;
        }
        return false;
    }

    boolean resetPosition() {
        if (m_positionInOutput != m_originalPosition) {
            m_positionInOutput = m_originalPosition;
            return true;
        }
        return false;
    }

    boolean isValid() {
        return m_isValid;
    }

    boolean setName(final String name) {
        if (!Objects.equals(m_name, name)) {
            m_name = name;
            return true;
        }
        return false;
    }

    boolean setProductionPath(final ProductionPath productionPath) {
        if (!Objects.equals(m_productionPath, productionPath)) {
            m_productionPath = productionPath;
            return true;
        }
        return false;
    }

    boolean setKeep(final boolean keep) {
        if (m_keep != keep) {
            m_keep = keep;
            return true;
        }
        return false;
    }

    void setIsValid(final boolean isValid) {
        m_isValid = isValid;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[")//
            .append(m_name)//
            .append(", ");
        if (m_productionPath != null) {
            sb.append(m_productionPath.getConverterFactory().getDestinationType())//
                .append(", ");
        }
        return sb.append(m_keep)//
            .append(", ")//
            .append(m_positionInOutput)//
            .append(", ")//
            .append(m_isValid)//
            .append("]")//
            .toString();
    }

    static <T> boolean areEqual(final MutableColumnTransformation<T> left, final MutableColumnTransformation<T> right) {
        if (left == right) {
            return true;
        }
        return left.m_positionInOutput == right.m_positionInOutput//NOSONAR
            && left.m_keep == right.m_keep//NOSONAR
            && left.m_name.equals(right.m_name)//NOSONAR
            && left.m_defaultSpec.equals(right.m_defaultSpec)// NOSONAR
            && left.m_productionPath.equals(right.m_productionPath);// NOSONAR
    }

}