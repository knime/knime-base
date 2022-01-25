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

package org.knime.base.data.filter.row.dialog.component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.base.data.filter.row.dialog.OperatorPanelParameters;
import org.knime.base.data.filter.row.dialog.component.config.EditorPanelConfig;
import org.knime.base.data.filter.row.dialog.component.config.TreePanelConfig;
import org.knime.base.data.filter.row.dialog.component.editor.EditorPanel;
import org.knime.base.data.filter.row.dialog.component.tree.TreePanel;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.base.data.filter.row.dialog.model.Node;
import org.knime.base.data.filter.row.dialog.registry.OperatorRegistry;
import org.knime.base.data.filter.row.dialog.util.NodeConverterHelper;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;

/**
 * The Row Filter component.
 *
 * @author Viktor Buria
 * @since 4.0
 */
public class RowFilterComponent {

    private final JPanel m_mainPanel;

    private final TreePanel m_treePanel;

    private final TreePanelConfig m_treePanelConfig;

    private final EditorPanel m_editorPanel;

    private final EditorPanelConfig m_editorPanelConfig;

    private final RowFilterConfig m_config;

    private final RowFilterElementFactory m_elementFactory;

    private final Set<ColumnRole> m_specialColumns;

    /**
     * Constructs a {@link RowFilterComponent}.
     *
     * @param config the {@linkplain RowFilterConfig configuration}
     * @param elementFactory the factory to create tree elements
     * @param specialColumns the additional columns (e.g. RowID) that should be selectable in addition
     * to the columns of the input table
     * @since 4.1
     */
    public RowFilterComponent(final RowFilterConfig config, final RowFilterElementFactory elementFactory,
        final Set<ColumnRole> specialColumns) {
        m_config = config;
        m_elementFactory = elementFactory;
        m_specialColumns = specialColumns == null ? Collections.emptySet() : specialColumns;
        CheckUtils.checkArgument(!m_specialColumns.contains(ColumnRole.ORDINARY),
            "The column role ORDINARY may not be passed as special column to the RowFilterComponent.");

        m_editorPanelConfig = new EditorPanelConfig(config.getGroupTypes());
        m_editorPanel = new EditorPanel(m_editorPanelConfig);

        m_treePanelConfig = new TreePanelConfig(m_elementFactory);
        m_treePanelConfig.setTreeChangedListener(m_editorPanel::saveChanges);
        m_treePanelConfig.setNoSelectionListener(m_editorPanel::hidePanel);
        m_treePanelConfig.setSelectGroupListener(m_editorPanel::showGroup);
        m_treePanelConfig.setSelectConditionListener(m_editorPanel::showCondition);
        m_treePanel = new TreePanel(m_treePanelConfig);

        m_mainPanel = new JPanel();
        m_mainPanel.setLayout(new BoxLayout(m_mainPanel, BoxLayout.X_AXIS));

        m_mainPanel.add(m_treePanel);
        m_mainPanel.add(m_editorPanel);
    }

    /**
     * Constructs a {@link RowFilterComponent}.
     *
     * @param config the {@linkplain RowFilterConfig configuration}
     * @param elementFactory the factory to create tree elements
     */
    public RowFilterComponent(final RowFilterConfig config, final RowFilterElementFactory elementFactory) {
        this(config, elementFactory, EnumSet.noneOf(ColumnRole.class));
    }

    /**
     * Cancels every execution that is currently in progress in this component.
     *
     * @since 4.1
     */
    public void cancelEveryExecution() {
        m_editorPanel.cancelEveryExecution();
    }

    /**
     * Gets the row filter component panel.
     *
     * @return the {@link JPanel}
     */
    public JPanel getPanel() {
        return m_mainPanel;
    }

    private void setRoot(final Node root) {
        m_treePanel.setRoot(NodeConverterHelper.convertToTreeNode(root));
    }

    /**
     * Saves current configuration to the Node settings.
     *
     * @param settings the {@link NodeSettingsWO}
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_editorPanel.saveChanges();

        m_config.setRoot(getRootNode());
        m_config.saveSettingsTo(settings);
    }

    /**
     * Loads configuration from the Node settings and sets a table specification.
     *
     * @param settings the {@link NodeSettingsRO}
     * @param tableSpec the {@linkplain DataTableSpec table specification}
     * @param operatorPanelConfig the {@link OperatorPanelParameters}
     * @param operatorRegistry the {@link OperatorRegistry}
     * @throws InvalidSettingsException if settings are invalid or can't be retrieved
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec tableSpec,
        final OperatorPanelParameters operatorPanelConfig, final OperatorRegistry<?> operatorRegistry)
        throws InvalidSettingsException {

        m_config.loadValidatedSettingsFrom(settings);
        setDataTableSpec(tableSpec, operatorPanelConfig, operatorRegistry);
    }

    /**
     * Sets a table specification.
     *
     * @param tableSpec the {@linkplain DataTableSpec table specification}
     * @param operatorPanelConfig the {@link OperatorPanelParameters}
     * @param operatorRegistry the {@link OperatorRegistry}
     * @since 4.6
     */
    public void setDataTableSpec(final DataTableSpec tableSpec,
        final OperatorPanelParameters operatorPanelConfig, final OperatorRegistry<?> operatorRegistry) {

        final DataTableSpec additionalColumnsTableSpec = addSpecialColumns(tableSpec);
        m_treePanelConfig.setDataTableSpec(additionalColumnsTableSpec);
        m_treePanelConfig.setOperatorRegistry(operatorRegistry);
        m_treePanelConfig.setColumnSpecList(createColumnSpecList(tableSpec));

        m_editorPanelConfig.setDataTableSpec(additionalColumnsTableSpec);
        m_editorPanelConfig.setColumnSpecList(createColumnSpecList(tableSpec));
        m_editorPanelConfig.setOperatorRegistry(operatorRegistry);
        m_editorPanelConfig.setOperatorPanelParameters(operatorPanelConfig);
        m_elementFactory.setLastGroupId(m_config.findLastGroupId());
        m_elementFactory.setLastConditionId(m_config.findLastConditionId());

        setRoot(m_config.getRoot());
    }

    /**
     * Gets current root node of the operator tree with all its children.
     *
     * @return root node
     * @since 4.6
     */
    public Node getRootNode() {
        return NodeConverterHelper.convertToNode(m_treePanel.getRoot());
    }

    private DataTableSpec addSpecialColumns(final DataTableSpec tableSpec) {
        if (m_specialColumns.isEmpty()) {
            return tableSpec;
        }
        final DataColumnSpec[] additionalColumns =
            m_specialColumns.stream().map(c -> c.createColumnSpec(tableSpec)).toArray(DataColumnSpec[]::new);
        final DataTableSpecCreator specCreator = new DataTableSpecCreator();
        specCreator.setName(tableSpec.getName());
        specCreator.addColumns(additionalColumns);
        specCreator.addColumns(tableSpec);
        return specCreator.createSpec();
    }

    private List<ColumnSpec> createColumnSpecList(final DataTableSpec tableSpec) {
        return Stream
            .concat(
                m_specialColumns.stream()
                    .map(c -> new ColumnSpec(c.createUniqueName(tableSpec), c.getType(), Collections.emptySet(), c)),
                tableSpec.stream()
                    .map(c -> new ColumnSpec(c.getName(), c.getType(), c.getDomain().getValues(), ColumnRole.ORDINARY)))
            .collect(Collectors.toList());
    }
}
