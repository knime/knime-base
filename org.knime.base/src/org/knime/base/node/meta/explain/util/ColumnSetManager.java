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
 *   13.03.2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.util;

import java.util.Arrays;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
class ColumnSetManager {

    private final DataColumnSpecFilterConfiguration m_filterCfg;

    private DataColumnSpec[] m_cols;

    public ColumnSetManager(final DataColumnSpecFilterConfiguration colFilterCfg) {
        m_filterCfg = colFilterCfg;
    }

    /**
     * Constructor for a ColumnSetManager that is based on a provided table spec.
     * All columns in <b>spec</b> are considered to be included and without further configuration
     * the ColumnSetManager will exclude additional columns encountered in later invocations.
     * @param spec of columns to be managed
     */
    ColumnSetManager(final DataTableSpec spec) {
        m_filterCfg = new DataColumnSpecFilterConfiguration("dummy");
        m_filterCfg.loadDefaults(spec.getColumnNames(), null, EnforceOption.EnforceInclusion);
        m_cols = spec.stream().toArray(DataColumnSpec[]::new);
    }

    /**
     * Updates this instance's managed columns using the filter specified in the constructor.
     *
     * @param inSpec {@link DataTableSpec} from which to extract columns to manage
     */
    public void updateColumnSet(final DataTableSpec inSpec) {
        final FilterResult fr = m_filterCfg.applyTo(inSpec);
        updateColumnSet(fr.getIncludes(), inSpec);
    }

    private void updateColumnSet(final String[] includes, final DataTableSpec spec) {
        m_cols = Arrays.stream(includes).map(spec::getColumnSpec).toArray(DataColumnSpec[]::new);
    }

    public DataTableSpec getTableSpec() {
        hasBeenUpdated();
        return new DataTableSpec(m_cols);
    }

    public DataColumnSpec[] getColumns() {
        hasBeenUpdated();
        return m_cols.clone();
    }

    private void hasBeenUpdated() {
        CheckUtils.checkState(m_cols != null, "This ColumnSetManager has not been updated with a DataColumnSpec. "
            + "Update must be called at least once before any other method can be accessed.");
    }

    /**
     * Checks if the provided {@link DataColumnSpec spec} contains the columns this instance manages.
     *
     * @param spec DataTableSpec to check for containment
     * @return true if all columns are contained in {@link DataColumnSpec spec}
     */
    public boolean containsColumns(final DataTableSpec spec) {
        hasBeenUpdated();
        for (final DataColumnSpec colSpec : m_cols) {
            if (!contains(colSpec, spec)) {
                return false;
            }
        }
        return true;
    }

    public void checkColumnsContained(final DataTableSpec spec) throws MissingColumnException {
        hasBeenUpdated();
        for (final DataColumnSpec colSpec : m_cols) {
            if (!contains(colSpec, spec)) {
                throw new MissingColumnException(colSpec);
            }
        }
    }

    public boolean sameOrder(final DataTableSpec spec) {
        hasBeenUpdated();
        final String[] colNames = spec.getColumnNames();
        CheckUtils.checkArgument(colNames.length == m_cols.length,
            "The provided spec '" + spec + "' has the wrong number of columns.");
        for (int i = 0; i < m_cols.length; i++) {
            if (!colNames[i].equals(m_cols[i].getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param spec {@link DataTableSpec} of input table
     * @return a {@link ColumnRearranger} that can transform the table represented by <b>spec</b> into a table
     *         containing only the columns managed by this instance in the correct order
     * @throws MissingColumnException if any of the columns managed by this instance are missing
     */
    public ColumnRearranger createRearranger(final DataTableSpec spec) throws MissingColumnException {
        checkColumnsContained(spec);
        final ColumnRearranger cr = new ColumnRearranger(spec);
        final String[] colNamesInOrder = Arrays.stream(m_cols).map(DataColumnSpec::getName).toArray(String[]::new);
        cr.keepOnly(colNamesInOrder);
        cr.permute(colNamesInOrder);
        return cr;
    }

    private static boolean contains(final DataColumnSpec col, final DataTableSpec spec) {
        final String name = col.getName();
        if (spec.containsName(name)) {
            final DataColumnSpec incoming = spec.getColumnSpec(name);
            if (col.getType().isASuperTypeOf(incoming.getType())) {
                return true;
            }
        }
        return false;
    }

    public int getNumColumns() {
        return m_cols.length;
    }

}
