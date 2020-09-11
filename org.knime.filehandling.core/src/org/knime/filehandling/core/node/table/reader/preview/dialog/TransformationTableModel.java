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
 *   Aug 5, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.filehandling.core.node.table.reader.selector.MutableTransformationModel;
import org.knime.filehandling.core.node.table.reader.selector.TransformationModel;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Model of a spec transformation.
 * TODO Currently only the {@link MutableTransformationModel} has been implemented
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TransformationTableModel<T> extends AbstractTableModel implements MutableTransformationModel<T> {

    private static final String[] COLUMN_NAMES = {"", "Type", "Name"};

    private final transient Function<T, ProductionPath> m_defaultProductionPathFn;

    private final transient Map<TypedReaderColumnSpec<T>, ColumnTuple> m_columns = new HashMap<>();

    private final transient CopyOnWriteArrayList<ChangeListener> m_changeListeners = new CopyOnWriteArrayList<>();

    private final transient ChangeEvent m_changeEvent = new ChangeEvent(this);

    private transient TypedReaderTableSpec<T> m_rawSpec = null;

    private boolean m_enabled = true;

    /* Only Serializable because AbstractTableModel is.
     * Instances of this class will never be serialized.
     */
    private static final long serialVersionUID = 1L; // NOSONAR

    TransformationTableModel(final Function<T, ProductionPath> defaultProductionPathFn) {
        m_defaultProductionPathFn = defaultProductionPathFn;
        addTableModelListener(e -> notifyChangeListeners());
    }

    @Override
    public void updateRawSpec(final TypedReaderTableSpec<T> rawSpec) {
        m_rawSpec = rawSpec;
        boolean tableChanged = false;
        int idx = 0;
        final Set<TypedReaderColumnSpec<T>> knownColumns = new HashSet<>(m_columns.keySet());
        for (TypedReaderColumnSpec<T> column : rawSpec) {
            if (knownColumns.remove(column)) {
                final ColumnTuple columnTuple = m_columns.get(column);
                // we know the column so we need to check if it is still in the same position
                final int oldPosition = columnTuple.getPositionInInput();
                tableChanged |= (oldPosition != idx);
                columnTuple.setPositionInInput(idx);
                // TODO once we allow reordering, this needs to be changed because we will have
                // to update the old column order somehow. Perhaps multiple strategies?
                columnTuple.setPositionInOutput(idx);
            } else {
                m_columns.put(column, new ColumnTuple(MultiTableUtils.getNameAfterInit(column),
                    m_defaultProductionPathFn.apply(column.getType()), idx, idx, true));
                tableChanged = true;
            }
            idx++;
        }

        tableChanged |= removeAnyMissingColumns(knownColumns);

        if (tableChanged) {
            fireTableDataChanged();
        }
    }

    @Override
    public TypedReaderTableSpec<T> getRawSpec() {
        return m_rawSpec;
    }

    @Override
    public void imitate(final TransformationModel<T> transformationModel) {
        boolean modelChanged = false;
        int idx = 0;
        m_rawSpec = transformationModel.getRawSpec();
        final Set<TypedReaderColumnSpec<T>> knownColumns = new HashSet<>(m_columns.keySet());
        for (TypedReaderColumnSpec<T> column : m_rawSpec) {
            final ColumnTuple otherTuple =
                new ColumnTuple(transformationModel.getName(column), transformationModel.getProductionPath(column), idx,
                    transformationModel.getPosition(column), transformationModel.keep(column));
            if (knownColumns.remove(column)) {
                final ColumnTuple columnTuple = m_columns.get(column);
                if (!columnTuple.equals(otherTuple)) {
                    m_columns.put(column, otherTuple);
                    modelChanged = true;
                }
            } else {
                m_columns.put(column, otherTuple);
                modelChanged = true;
            }
        }

        modelChanged |= removeAnyMissingColumns(knownColumns);

        if (modelChanged) {
            fireTableDataChanged();
        }

    }

    private boolean removeAnyMissingColumns(final Set<TypedReaderColumnSpec<T>> knownColumns) {
        if (!knownColumns.isEmpty()) {
            for (TypedReaderColumnSpec<T> removedColumn : knownColumns) {
                m_columns.remove(removedColumn);
            }
            return true;
        }
        return false;
    }

    @Override
    public ProductionPath getProductionPath(final TypedReaderColumnSpec<T> column) {
        return m_columns.get(column).getProductionPath();
    }

    @Override
    public String getName(final TypedReaderColumnSpec<T> column) {
        return m_columns.get(column).getName();
    }

    @Override
    public boolean keep(final TypedReaderColumnSpec<T> column) {
        return m_columns.get(column).isKeep();
    }

    @Override
    public int getPosition(final TypedReaderColumnSpec<T> column) {
        return m_columns.get(column).getPositionInOutput();
    }

    @Override
    public void addChangeListener(final ChangeListener listener) {
        if (!m_changeListeners.contains(listener)) {//NOSONAR
            m_changeListeners.add(listener);//NOSONAR a small price to pay for thread-safety
        }
    }

    @Override
    public void removeChangeListener(final ChangeListener listener) {
        m_changeListeners.remove(listener);//NOSONAR a small price to pay for thread-safety
    }

    private void notifyChangeListeners() {
        for (ChangeListener listener : m_changeListeners) {
            listener.stateChanged(m_changeEvent);
        }
    }

    @Override
    public int getRowCount() {
        return m_rawSpec.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(final int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        if (m_enabled != enabled) {
            m_enabled = enabled;
            fireTableDataChanged();
        }
    }

    private static class ColumnTuple {

        private String m_name;

        private ProductionPath m_productionPath;

        private int m_positionInOutput;

        private int m_positionInInput;

        private boolean m_keep;

        ColumnTuple(final String name, final ProductionPath productionPath, final int positionInInput,
            final int positionInOutput, final boolean keep) {
            m_name = name;
            m_productionPath = productionPath;
            m_positionInInput = positionInInput;
            m_positionInOutput = positionInOutput;
            m_keep = keep;
        }

        String getName() {
            return m_name;
        }

        ProductionPath getProductionPath() {
            return m_productionPath;
        }

        /**
         * @return the positionInOutput
         */
        int getPositionInOutput() {
            return m_positionInOutput;
        }

        /**
         * @param positionInOutput the positionInOutput to set
         */
        void setPositionInOutput(final int positionInOutput) {
            m_positionInOutput = positionInOutput;
        }

        /**
         * @return the positionInInput
         */
        int getPositionInInput() {
            return m_positionInInput;
        }

        /**
         * @param positionInInput the positionInInput to set
         */
        void setPositionInInput(final int positionInInput) {
            m_positionInInput = positionInInput;
        }

        boolean isKeep() {
            return m_keep;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                final ColumnTuple other = (ColumnTuple)obj;
                return m_positionInInput == other.m_positionInInput//
                    && m_positionInOutput == other.m_positionInOutput//
                    && m_keep == other.m_keep//
                    && m_name.equals(other.m_name)//
                    && m_productionPath.equals(other.m_productionPath);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()//
                .append(m_name)//
                .append(m_productionPath)//
                .append(m_keep)//
                .append(m_positionInInput)//
                .append(m_positionInOutput)//
                .build();
        }

    }

}
