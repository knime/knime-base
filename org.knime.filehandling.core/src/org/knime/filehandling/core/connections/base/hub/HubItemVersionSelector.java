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
 *   Sep 25, 2022 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.connections.base.hub;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.connections.ItemVersionAware.RepositoryItemVersion;
import org.knime.filehandling.core.connections.base.hub.HubAccessUtil.HubAccess;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusView;

/**
 * Hub repository item version selector component.
 *
 * @author Alexander Bondaletov
 */
public final class HubItemVersionSelector extends JPanel {

    private static final long serialVersionUID = 1L;

    private final HubItemVersionSettings m_versionSettings; //NOSONAR not intended for serialization

    private final HubItemVersionSelectionComboBox m_comboBox;

    private final StatusView m_statusView; // NOSONAR

    private final Component m_statusViewPlaceholder;

    private final HubAccess m_hubAccess; //NOSONAR not intended for serialization

    private FetchItemVersionsSwingWorker m_fetchVersionsWorker = null; //NOSONAR not intended for serialization

    private FlowVariableModel m_itemVersionFvm; //NOSONAR not intended for serialization

    private final FlowVariableModelButton m_itemVersionFvmBtn;

    private boolean m_overwrittenByVariable = false; // NOSONAR

    private String m_itemId = null; // NOSONAR

    private boolean m_ignoreSettingsChange = false; // NOSONAR

    /**
     * Constructor.
     *
     * @param versionSettings The version settings instance to use.
     * @param hubAccess The {@link HubAccess} instance to fetch versions from KNIME Hub.
     * @param itemVersionFvm the {@link FlowVariableModel} for the item version
     */
    public HubItemVersionSelector(final HubItemVersionSettings versionSettings, final HubAccess hubAccess,
        final FlowVariableModel itemVersionFvm) {

        m_versionSettings = versionSettings;
        m_versionSettings.addChangeListener(e -> onVersionSettingsChanged());

        m_hubAccess = hubAccess;

        m_comboBox = new HubItemVersionSelectionComboBox();
        m_comboBox.setChangeListener(this::onItemVersionSelectionChanged);

        m_statusView = new StatusView(300);
        m_statusViewPlaceholder = Box.createHorizontalStrut((int)m_statusView.getPanel().getPreferredSize().getWidth());

        m_itemVersionFvm = itemVersionFvm;
        m_itemVersionFvm.addChangeListener(e -> onItemVersionFvmChanged());
        m_itemVersionFvmBtn = new FlowVariableModelButton(m_itemVersionFvm);

        final var gbc = new GridBagConstraints();
        setLayout(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 5);
        add(m_comboBox, gbc);

        gbc.gridx += 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(m_itemVersionFvmBtn, gbc);

        gbc.gridx += 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(m_statusView.getPanel(), gbc);

        gbc.gridx += 1;
        add(m_statusViewPlaceholder, gbc);
        m_statusViewPlaceholder.setVisible(false); // when the HubItemVersionSelector is enabled, the glue
                                                    // is invisible (see setEnabled())
    }

    /**
     * Invoked to transfer changes from the UI to the underlying settings (HubSpaceSettings)
     *
     * @param ignored
     */
    private void onItemVersionSelectionChanged(final ChangeEvent ignored) {
        if (!isEnabled() || m_ignoreSettingsChange) {
            return;
        }

        m_statusView.clearStatus();
        final var selectedItem = m_comboBox.getSelectedItem();

        // To avoid event loops (UI change -> settings change -> UI change -> ...)
        // we use m_ignoreSettingsChange as a circuit breaker here.
        m_ignoreSettingsChange = true;
        if (selectedItem == null) {
            m_versionSettings.setItemVersion(null);
        } else {
            m_versionSettings.setItemVersion(selectedItem.getVersion());
        }
        m_ignoreSettingsChange = false;
    }

    private void onItemVersionFvmChanged() {
        if (!isEnabled() || m_ignoreSettingsChange) {
            return;
        }

        m_ignoreSettingsChange = true;

        String itemVersionValue = null;
        if (m_itemVersionFvm.isVariableReplacementEnabled()) {
            itemVersionValue = m_itemVersionFvm.getVariableValue()//
                .map(flowVar -> flowVar.getValue(StringType.INSTANCE))//
                .orElse(null);
        }

        if (StringUtils.isNotBlank(itemVersionValue)) {
            m_overwrittenByVariable = true;
            m_comboBox.setEnabled(false);
            m_comboBox.setSelectedItem(itemVersionValue);
            m_versionSettings.setItemVersion(itemVersionValue);
        } else {
            m_overwrittenByVariable = false;
            m_comboBox.setEnabled(true);
            m_comboBox.setSelectedItem(null);
            m_versionSettings.setItemVersion(null);
            triggerFetchItemVersions(m_itemId);
        }
        m_ignoreSettingsChange = false;
    }

    /**
     * @param enabled Whether the selector is enabled or not.
     */
    @Override
    public void setEnabled(final boolean enabled) {
        var wasEnabled = isEnabled();
        super.setEnabled(enabled);
        updateEnabledness();
        if (enabled && !wasEnabled && m_itemId != null) {
            triggerFetchItemVersions(m_itemId);
        }
    }

    private void updateEnabledness() {
        var effectivelyEnabled = isEnabled();
        m_comboBox.setEnabled(!m_overwrittenByVariable && effectivelyEnabled);
        m_itemVersionFvmBtn.setEnabled(effectivelyEnabled);
        m_statusView.getPanel().setVisible(effectivelyEnabled);
        m_statusViewPlaceholder.setVisible(!effectivelyEnabled);
    }

    /**
     * Invoked by surrounding code when the selected repository item has changed and the version selection needs to be
     * updated.
     *
     * @param itemId The repository item ID to use.
     */
    public void setItemId(final String itemId) {
        m_itemId = itemId;

        if (!isEnabled()) {
            return;
        }
        triggerFetchItemVersions(m_itemId);
    }

    /**
     * Invoked to react to changes in the underlying settings for the version and then update the version UI. This only
     * happens while loading settings.
     */
    private void onVersionSettingsChanged() {
        if (!isEnabled() || m_ignoreSettingsChange) {
            return;
        }

        updateEnabledness();
        m_ignoreSettingsChange = true;
        m_comboBox.setSelectedItem(m_versionSettings.getItemVersion().orElse(null));
        triggerFetchItemVersions(m_itemId);
        m_ignoreSettingsChange = false;
    }

    private void triggerFetchItemVersions(final String itemId) {
        if (m_fetchVersionsWorker != null) {
            m_fetchVersionsWorker.cancel(true);
            m_fetchVersionsWorker = null;
        }

        m_fetchVersionsWorker = new FetchItemVersionsSwingWorker(itemId);
        m_fetchVersionsWorker.execute();
    }

    private class FetchItemVersionsSwingWorker extends SwingWorkerWithContext<List<RepositoryItemVersion>, Void> {

        private final String m_currItemId;

        FetchItemVersionsSwingWorker(final String spaceId) {
            m_currItemId = StringUtils.isBlank(spaceId) ? null : HubSpaceSelector.sanitizeSpaceId(spaceId);
            m_comboBox.setEnabled(false);
            m_statusView.setStatus(DefaultStatusMessage.mkInfo("Loading versions..."));
        }

        @Override
        protected List<RepositoryItemVersion> doInBackgroundWithContext() throws Exception {
            if (m_currItemId != null) {
                return m_hubAccess.fetchRepositoryItemVersions(m_currItemId);
            } else {
                return null; // NOSONAR
            }
        }

        @Override
        protected void doneWithContext() {
            m_statusView.clearStatus();
            updateEnabledness();

            if (isCancelled()) {
                return;
            }

            try {
                var result = get();
                if (result == null || result.isEmpty()) { // no (valid) item id was given or no versions exist
                    m_comboBox.clearItemsAndSelection();
                } else {
                    m_comboBox.setItems(result, m_versionSettings.getItemVersion().orElse(null));
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                m_comboBox.clearItemsAndSelection();
                m_statusView.setStatus(DefaultStatusMessage.mkError(ExceptionUtil.unpack(ex).getMessage()));
            }
        }
    }
}
