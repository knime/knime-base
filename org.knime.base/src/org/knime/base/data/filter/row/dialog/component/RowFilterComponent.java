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

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.base.data.filter.row.dialog.OperatorPanelParameters;
import org.knime.base.data.filter.row.dialog.component.config.EditorPanelConfig;
import org.knime.base.data.filter.row.dialog.component.config.TreePanelConfig;
import org.knime.base.data.filter.row.dialog.component.editor.EditorPanel;
import org.knime.base.data.filter.row.dialog.component.tree.TreePanel;
import org.knime.base.data.filter.row.dialog.model.Node;
import org.knime.base.data.filter.row.dialog.registry.OperatorRegistry;
import org.knime.base.data.filter.row.dialog.util.NodeConverterHelper;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

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

    /**
     * Constructs a {@link RowFilterComponent}.
     *
     * @param config the {@linkplain RowFilterConfig configuration}
     * @param elementFactory the factory to create tree elements
     */
    public RowFilterComponent(final RowFilterConfig config, final RowFilterElementFactory elementFactory) {
        m_config = config;
        m_elementFactory = elementFactory;

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

        m_config.setRoot(getRoot());
        m_config.saveSettingsTo(settings);
    }

    /**
     * Loads configuration from the Node settings.
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

        m_treePanelConfig.setDataTableSpec(tableSpec);
        m_treePanelConfig.setOperatorRegistry(operatorRegistry);

        m_editorPanelConfig.setDataTableSpec(tableSpec);
        m_editorPanelConfig.setOperatorRegistry(operatorRegistry);
        m_editorPanelConfig.setOperatorPanelParameters(operatorPanelConfig);

        m_elementFactory.setLastGroupId(m_config.findLastGroupId());
        m_elementFactory.setLastConditionId(m_config.findLastConditionId());

        setRoot(m_config.getRoot());
    }

    private Node getRoot() {
        return NodeConverterHelper.convertToNode(m_treePanel.getRoot());
    }

}
