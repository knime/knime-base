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
 *   5 Dec 2019 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.sorter.dialog;

import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * This class holds the panel containing all the elements of the sorter dialog.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public class DynamicSorterPanel {

    private final RestorableDynamicOrderPanel<DynamicSortItem> m_panel;

    /**
     * The entry in the JComboBox for sorting by {@link RowKey}.
     */
    public static final DataColumnSpec ROWKEY =
        new DataColumnSpecCreator("-ROWKEY -", DataType.getType(StringCell.class)).createSpec();

    /**
     * Creates a new RestorableDynamicOrderPanel
     * @param includeListKey the key to save and load column names
     * @param sortOrderKey the key to save and load sort orders
     */
    public DynamicSorterPanel(final String includeListKey, final String sortOrderKey) {
        m_panel = new RestorableDynamicOrderPanel<>(new DynamicSorterItemContext(includeListKey, sortOrderKey));
    }

    /**
     * Creates a list of sorting criteria from the settings using the input data
     * table spec and fills the panel with the different sort criteria, if available.
     *
     * @param settings the node settings to read from
     * @param specs the input specs
     *
     * @throws NotConfigurableException if the dialog cannot be opened.
     */
    public void load(final NodeSettingsRO settings, final DataTableSpec[] specs) throws NotConfigurableException {
        m_panel.load(settings, specs);
    }

    /**
     * Sets the list of columns to include and the sorting order list inside the
     *
     * @param settings the node settings to write into
     * @throws InvalidSettingsException if settings are not valid
     */
    public void save(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_panel.save(settings);
    }

    /**
     *
     * @return the panel which holds the sort criteria
     */
    public JPanel getPanel() {
        return m_panel.getPanel();
    }
}
