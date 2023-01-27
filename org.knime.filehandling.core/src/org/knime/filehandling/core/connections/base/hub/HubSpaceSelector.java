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
 *   Sep 14, 2022 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.connections.base.hub;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.connections.base.hub.HubAccessUtil.HubAccess;
import org.knime.filehandling.core.connections.base.hub.HubSpaceSelectionComboBox.SpaceComboItem;
import org.knime.filehandling.core.defaultnodesettings.ExceptionUtil;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusView;

/**
 * A component for selecting a Hub Space. It allows user to select the space from the fetched space, or to enter space
 * id manually.
 *
 * @author Alexander Bondaletov
 */
public final class HubSpaceSelector extends JPanel {
    private static final long serialVersionUID = 1L;

    private final HubSpaceSettings m_settings;//NOSONAR not intended for serialization

    private final HubSpaceSelectionComboBox m_combobox;

    private final JButton m_findMoreBtn;

    private final StatusView m_statusView; // NOSONAR

    private final Component m_statusViewPlaceholder;

    private final HubAccess m_hubAccess; //NOSONAR not intended for serialization

    /**
     * Used to ensure that a {@link ListSpacesSwingWorker} has feed its results at least once into the combobox, before
     * a {@link SpaceResolverSwingWorker} updates the selected item.
     */
    private final CountDownLatch m_spacesHaveBeenListed = new CountDownLatch(1); // NOSONAR

    private boolean m_ignoreSettingsChange = false;

    private ListSpacesSwingWorker m_listWorker;//NOSONAR not intended for serialization

    private SpaceResolverSwingWorker m_fetchWorker; //NOSONAR not intended for serialization

    /**
     * @param settings The hub space settings object.
     * @param hubAccess The {@link HubAccess} object.
     *
     */
    public HubSpaceSelector(final HubSpaceSettings settings, final HubAccess hubAccess) {
        m_settings = settings;
        m_settings.addChangeListener(e -> onSettingsChanged());

        m_hubAccess = hubAccess;

        m_combobox = new HubSpaceSelectionComboBox();
        m_combobox.setChangeListener(this::onSpaceSelectionChanged);

        m_findMoreBtn = new JButton("More...");
        m_findMoreBtn.addActionListener(e -> onFindMore());

        m_statusView = new StatusView(300);
        m_statusViewPlaceholder = Box.createHorizontalStrut((int)m_statusView.getPanel().getPreferredSize().getWidth());

        final var gbc = new GridBagConstraints();
        setLayout(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 5);

        add(m_combobox, gbc);

        gbc.gridx += 1;
        add(m_findMoreBtn, gbc);

        gbc.gridx += 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(m_statusView.getPanel(), gbc);

        gbc.gridx += 1;
        add(m_statusViewPlaceholder, gbc);
        m_statusViewPlaceholder.setVisible(false); // when the HubSpaceSelector is enabled, the glue is invisible (see setEnabled())
    }

    /**
     * Invoked to transfer changes from the UI (HubSpaceSelectionComboBox) to the underlying settings (HubSpaceSettings)
     *
     * @param event
     */
    private void onSpaceSelectionChanged(final ChangeEvent event) {
        m_statusView.clearStatus();
        final var selectedItem = m_combobox.getSelectedItem();

        // To avoid event loops (UI change -> settings change -> UI change -> ...)
        // we use m_ignoreSettingsChange as a circuit breaker here.
        m_ignoreSettingsChange = true;
        if (selectedItem == null || StringUtils.isBlank(selectedItem.getId())) {
            m_settings.set("", "");
            m_statusView
                .setStatus(DefaultStatusMessage.mkError("Please pick a Space from the list, or specify a valid ID."));
        } else {
            m_settings.set(selectedItem.getId(), selectedItem.getName());
            if (!selectedItem.isResolved()) {
                triggerSpaceResolution(selectedItem);
            }
        }
        m_ignoreSettingsChange = false;
    }

    /**
     * Invoked to transfer changes from the underlying settings (HubSpaceSettings) to the UI
     * (HubSpaceSelectionComboBox). This is only useful to initialize the UI while loading settings.
     *
     * @param event
     */
    private void onSettingsChanged() {
        // To avoid event loops (UI change -> settings change -> UI change -> ...)
        // we use m_ignoreSettingsChange as a circuit breaker here.
        if (!m_ignoreSettingsChange) {
            m_combobox.setSelectedItem(new SpaceComboItem(m_settings.getSpaceId(), //
                m_settings.getSpaceName()));
        }
    }

    private synchronized void triggerSpaceResolution(final SpaceComboItem item) {
        if (m_fetchWorker != null) {
            m_fetchWorker.cancel(true);
        }

        m_fetchWorker = new SpaceResolverSwingWorker(item);
        m_fetchWorker.execute();
    }

    /**
     * Triggers a refresh of the list of Hub Spaces in combo box.
     */
    public synchronized void triggerSpaceListing() {
        if (m_listWorker != null) {
            m_listWorker.cancel(true);
        }
        m_listWorker = new ListSpacesSwingWorker();
        m_listWorker.execute();
    }

    @Override
    public void setEnabled(final boolean enabled) {
        if (isEnabled() == enabled) {
            return;
        }
        super.setEnabled(enabled);
        m_combobox.setEnabled(enabled);
        m_findMoreBtn.setEnabled(enabled);

        // depending on the enabledness, we display the StatusView, or the (empty) placeholder, but never both
        m_statusView.getPanel().setVisible(enabled);
        m_statusViewPlaceholder.setVisible(!enabled);

        if (enabled) {
            triggerSpaceListing();
        }
    }

    private void onFindMore() {
        var parent = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, this);

        var item = HubSpaceSelectSubDialog.showDialog(parent, m_hubAccess);
        if (item != null) {
            m_combobox.setSelectedItem(new SpaceComboItem(item));
        }
    }

    private class ListSpacesSwingWorker extends SwingWorkerWithContext<List<SpaceComboItem>, Void> {

        ListSpacesSwingWorker() {
            m_combobox.setEnabled(false);
            m_statusView.setStatus(DefaultStatusMessage.mkInfo("Loading Spaces..."));
        }

        @Override
        protected List<SpaceComboItem> doInBackgroundWithContext() throws Exception {
            return m_hubAccess.listSpaces().stream().map(SpaceComboItem::new).collect(Collectors.toList());
        }

        @Override
        protected void doneWithContext() {
            m_combobox.setEnabled(isEnabled());
            m_statusView.clearStatus();
            m_spacesHaveBeenListed.countDown();

            if (isCancelled()) {
                return;
            }

            try {
                m_combobox.setItems(get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                m_combobox.setItems(Collections.emptyList());
                m_statusView.setStatus(DefaultStatusMessage.mkError(ExceptionUtil.unpack(ex).getMessage()));
            }
        }
    }

    private class SpaceResolverSwingWorker extends SwingWorkerWithContext<SpaceComboItem, Void> {
        private final SpaceComboItem m_itemToResolve;

        public SpaceResolverSwingWorker(final SpaceComboItem toResolve) {
            m_itemToResolve = toResolve;
            m_statusView.setStatus(DefaultStatusMessage.mkInfo("Locating Space by ID %s...", toResolve.getId()));
        }

        @Override
        protected SpaceComboItem doInBackgroundWithContext() throws Exception {
            Thread.sleep(200);

            var spaceId = sanitizeSpaceId(m_itemToResolve.getId());
            var space = m_hubAccess.fetchSpace(spaceId);
            // make sure that we wait for the spaces to have been listed at least once
            m_spacesHaveBeenListed.await();
            return new SpaceComboItem(space);
        }

        @Override
        protected void doneWithContext() {
            m_statusView.clearStatus();

            if (isCancelled()) {
                return;
            }

            try {
                m_combobox.setSelectedItem(get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException ex) {
                m_statusView.setStatus(DefaultStatusMessage.mkError(ExceptionUtil.unpack(ex).getMessage()));
            }
        }
    }

    static String sanitizeSpaceId(final String unsanitizedSpaceId) {
        var sanitized = unsanitizedSpaceId.strip();
        if (sanitized.startsWith("~")) {
            sanitized = "*" + sanitized.substring(1);
        } else if (!sanitized.startsWith("*")) {
            sanitized = "*" + sanitized;
        }
        return sanitized;
    }
}
