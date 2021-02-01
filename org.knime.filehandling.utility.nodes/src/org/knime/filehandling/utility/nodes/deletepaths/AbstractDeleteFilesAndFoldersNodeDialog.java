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
 *   14.01.2021 (lars.schweikardt): created
 */
package org.knime.filehandling.utility.nodes.deletepaths;

import java.awt.GridBagLayout;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Abstract node dialog for the "Delete Files/Folders" nodes.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @param <C> an {@link AbstractDeleteFilesAndFoldersNodeConfig} instance
 */
public abstract class AbstractDeleteFilesAndFoldersNodeDialog<C extends AbstractDeleteFilesAndFoldersNodeConfig>
    extends NodeDialogPane {

    private final C m_config;

    private final DialogComponentBoolean m_abortIfFails;

    /**
     * Constructor.
     *
     * @param nodeConfig node specific implementation of the {@link AbstractDeleteFilesAndFoldersNodeConfig}
     */
    protected AbstractDeleteFilesAndFoldersNodeDialog(final C nodeConfig) {
        m_config = nodeConfig;
        m_abortIfFails = new DialogComponentBoolean(m_config.isAbortedIfFails(), "Abort if delete fails");
    }

    /**
     * Returns the node specific implementation of the {@link AbstractDeleteFilesAndFoldersNodeConfig}.
     *
     * @return the node config
     */
    protected final C getConfig() {
        return m_config;
    }

    /** Creates the settings tab. */
    protected final void createSettingsTab() {
        addTab("Settings", initLayout());
    }

    /**
     * Initializes the {@link JPanel} for the node dialog.
     *
     * @return the {@link JPanel} for the node dialog.
     */
    private JPanel initLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createAndInitGBC().setWeightX(1).fillHorizontal();
        panel.add(createPathPanel(), gbc.build());
        panel.add(createOptionsPanel(), gbc.incY().build());
        panel.add(new JPanel(), gbc.incY().setWeightY(1).build());

        return panel;
    }

    /**
     * Creates the options {@link JPanel} for the node dialog.
     *
     * @return the options {@link JPanel}
     */
    private JPanel createOptionsPanel() {
        final GBCBuilder gbc = createAndInitGBC();
        final JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options"));
        optionsPanel.add(m_abortIfFails.getComponentPanel(), gbc.build());
        if (additionalOptions().isPresent()) {
            optionsPanel.add(additionalOptions().get(), gbc.incY().build()); //NOSONAR
        }
        optionsPanel.add(new JPanel(), gbc.incX().setWeightX(1).build());
        return optionsPanel;
    }

    /**
     * Creates the {@link JPanel} for the selection of the paths, this could be either a
     * {@link DialogComponentReaderFileChooser} or a {@link DialogComponentColumnNameSelection}.
     *
     * @return the {@link JPanel} for the selection of the paths
     */
    protected abstract JPanel createPathPanel();

    /**
     * Returns an {@link Optional} of a {@link JPanel} for additional options.
     *
     * @return {@link Optional} of a {@link JPanel}
     */
    protected abstract Optional<JPanel> additionalOptions();

    /**
     * Provides a preconfigured instance of a {@link GBCBuilder}.
     *
     * @return a {@link GBCBuilder}
     */
    protected static final GBCBuilder createAndInitGBC() {
        return new GBCBuilder().resetPos().setWeightX(0).setWeightX(0).fillNone().anchorFirstLineStart();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_abortIfFails.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_abortIfFails.loadSettingsFrom(settings, specs);
    }
}
