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
 *   2022-04-05 (Bjoern Lohrmann): created
 */
package org.knime.ext.example.filehandling.node;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.ext.example.filehandling.fs.ExampleFSConnection;
import org.knime.ext.example.filehandling.fs.ExampleFSConnectionConfig;
import org.knime.ext.example.filehandling.fs.ExampleFSConnectionConfig.ConnectionMode;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.base.auth.AuthPanel;
import org.knime.filehandling.core.connections.base.auth.AuthSettings;
import org.knime.filehandling.core.connections.base.auth.EmptyAuthProviderPanel;
import org.knime.filehandling.core.connections.base.auth.StandardAuthTypes;
import org.knime.filehandling.core.connections.base.auth.UserPasswordAuthProviderPanel;
import org.knime.filehandling.core.connections.base.ui.WorkingDirectoryChooser;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Node dialog for the Example Connector.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
class ExampleConnectorNodeDialog extends NodeDialogPane {

    private final ExampleConnectorSettings m_settings;

    private final Map<ConnectionMode, JRadioButton> m_connectionModes = new EnumMap<>(ConnectionMode.class);
    private final JRadioButton m_radioFileserver;
    private final JRadioButton m_radioDomain;

    private final JPanel m_connectionCardsPanel;

    private final DialogComponentString m_fileserverHost;
    private final DialogComponentNumber m_fileserverPort;
    private final DialogComponentString m_fileserverShare;

    private final DialogComponentString m_domainName;
    private final DialogComponentString m_domainNamespace;

    private final AuthPanel m_authPanel;

    private final WorkingDirectoryChooser m_workingDirChooser;
    private final ChangeListener m_workdirListener;

    /**
     * Creates new instance
     */
    public ExampleConnectorNodeDialog() {
        m_settings = new ExampleConnectorSettings();

        final ButtonGroup group = new ButtonGroup();
        m_radioFileserver = createConnectionModeButton(ConnectionMode.FILESERVER, group);
        m_radioDomain = createConnectionModeButton(ConnectionMode.DOMAIN, group);

        m_connectionCardsPanel = new JPanel(new CardLayout());

        m_fileserverHost = new DialogComponentString(m_settings.getFileserverHostModel(), "Host:  ", false, 30);
        m_fileserverPort = new DialogComponentNumber(m_settings.getFileserverPortModel(), "Port:", 1);
        m_fileserverShare = new DialogComponentString(m_settings.getFileserverShareModel(), "Share:", false, 30);

        m_domainName = new DialogComponentString(m_settings.getDomainNameModel(), "Domain:                   ", false,
                30);
        m_domainNamespace = new DialogComponentString(m_settings.getDomainNamespaceModel(), "Share/Namespace:", false,
                30);

        m_workingDirChooser = new WorkingDirectoryChooser("example.workingDir", this::createFSConnection);
        m_workdirListener = e -> m_settings.getWorkingDirectoryModel()
                .setStringValue(m_workingDirChooser.getSelectedWorkingDirectory());

        AuthSettings authSettings = m_settings.getAuthSettings();
        m_authPanel = new AuthPanel(authSettings, //
                Arrays.asList( //
                        new UserPasswordAuthProviderPanel(
                                authSettings.getSettingsForAuthType(StandardAuthTypes.USER_PASSWORD), this), //
                        new EmptyAuthProviderPanel( //
                                authSettings.getSettingsForAuthType(ExampleFSConnectionConfig.KERBEROS_AUTH_TYPE)), //
                        new EmptyAuthProviderPanel( //
                                authSettings.getSettingsForAuthType(ExampleFSConnectionConfig.GUEST_AUTH_TYPE)), //
                        new EmptyAuthProviderPanel( //
                                authSettings.getSettingsForAuthType(StandardAuthTypes.ANONYMOUS))));

        addTab("Settings", createSettingsPanel());
        addTab("Advanced", createTimeoutsPanel());
    }

    private FSConnection createFSConnection() throws IOException {
        try {
            m_settings.validate();
        } catch (InvalidSettingsException e) {
            throw new IOException(e.getMessage(), e);
        }

        final CredentialsProvider credentialsProvider = getCredentialsProvider();
        final ExampleFSConnectionConfig config = m_settings.createFSConnectionConfig(credentialsProvider::get);
        return new ExampleFSConnection(config);
    }

    private JComponent createSettingsPanel() {
        Box box = new Box(BoxLayout.Y_AXIS);
        box.add(createConnectionPanel());
        box.add(createAuthPanel());
        box.add(createFilesystemPanel());
        return box;
    }

    private JComponent createConnectionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Connection"));

        final GBCBuilder gbc = new GBCBuilder();

        gbc.resetPos().insetLeft(5).anchorWest().fillNone();
        panel.add(new JLabel("Connect to: "), gbc.build());

        gbc.incX();
        panel.add(m_radioFileserver, gbc.build());

        gbc.incX();
        panel.add(m_radioDomain, gbc.build());

        gbc.incX().fillHorizontal().setWeightX(1);
        panel.add(Box.createHorizontalGlue(), gbc.build());

        gbc.resetX().incY().insetLeft(30).insetTop(10).setWeightX(1).widthRemainder();
        m_connectionCardsPanel.add(createFileserverSettingsPanel(), ConnectionMode.FILESERVER.toString());
        m_connectionCardsPanel.add(createDomainSettingsPanel(), ConnectionMode.DOMAIN.toString());
        panel.add(m_connectionCardsPanel, gbc.build());

        return panel;
    }

    private Component createDomainSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        GBCBuilder gbc = new GBCBuilder();

        gbc.resetPos().anchorWest().fillNone();
        panel.add(m_domainName.getComponentPanel(), gbc.build());

        gbc.incX().fillHorizontal().setWeightX(1);
        panel.add(Box.createHorizontalGlue(), gbc.build());

        gbc.resetX().incY().fillNone().setWeightX(0);
        panel.add(m_domainNamespace.getComponentPanel(), gbc.build());

        gbc.incX().fillHorizontal().setWeightX(1).widthRemainder();
        panel.add(Box.createHorizontalGlue(), gbc.build());

        return panel;
    }

    private Component createFileserverSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());

        GBCBuilder gbc = new GBCBuilder();

        gbc.resetPos().anchorWest().fillNone();
        panel.add(m_fileserverHost.getComponentPanel(), gbc.build());

        gbc.incX().insetLeft(10);
        panel.add(m_fileserverPort.getComponentPanel(), gbc.build());

        gbc.incX().insetLeft(0).fillHorizontal().setWeightX(1);
        panel.add(Box.createHorizontalGlue(), gbc.build());

        gbc.resetX().incY().fillNone().setWeightX(0);
        panel.add(m_fileserverShare.getComponentPanel(), gbc.build());

        gbc.incX().fillHorizontal().setWeightX(1).widthRemainder();
        panel.add(Box.createHorizontalGlue(), gbc.build());

        return panel;
    }

    private JRadioButton createConnectionModeButton(final ConnectionMode mode, final ButtonGroup group) {
        JRadioButton rb = new JRadioButton(mode.toString());
        rb.addActionListener(e -> {
            m_settings.setConnectionMode(mode);
            updateConnectionCards();
        });

        group.add(rb);
        m_connectionModes.put(mode, rb);

        return rb;
    }

    private void updateConnectionCards() {
        ConnectionMode mode = m_settings.getConnectionMode();
        ((CardLayout) m_connectionCardsPanel.getLayout()).show(m_connectionCardsPanel, mode.toString());
    }

    private JComponent createAuthPanel() {
        m_authPanel.setBorder(BorderFactory.createTitledBorder("Authentication"));
        return m_authPanel;
    }

    private JComponent createFilesystemPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 10, 0, 0);
        panel.add(m_workingDirChooser, c);

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy += 1;
        panel.add(Box.createVerticalGlue(), c);

        panel.setBorder(BorderFactory.createTitledBorder("File system settings"));
        return panel;
    }

    private JComponent createTimeoutsPanel() {
        final DialogComponentNumber timeout = new DialogComponentNumber(m_settings.getTimeoutModel(), "", 1);
        timeout.getComponentPanel().setLayout(new FlowLayout(FlowLayout.LEFT));

        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Read/Write timeout (seconds): "), c);

        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(timeout.getComponentPanel(), c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        panel.add(Box.createVerticalGlue(), c);

        panel.setBorder(BorderFactory.createTitledBorder("Connection settings"));
        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        validateBeforeSaving();
        m_settings.saveForDialog(settings);
        m_authPanel.saveSettingsTo(settings.addNodeSettings(AuthSettings.KEY_AUTH));
    }

    private void validateBeforeSaving() throws InvalidSettingsException {
        m_settings.validate();
        m_workingDirChooser.addCurrentSelectionToHistory();
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {

        try {
            m_authPanel.loadSettingsFrom(settings.getNodeSettings(AuthSettings.KEY_AUTH), specs);
            m_settings.loadSettingsForDialog(settings);
        } catch (InvalidSettingsException | NotConfigurableException ex) { // NOSONAR
            // ignore
        }

        settingsLoaded();
    }

    private void settingsLoaded() {
        m_workingDirChooser.setSelectedWorkingDirectory(m_settings.getWorkingDirectoryModel().getStringValue());
        m_workingDirChooser.addListener(m_workdirListener);

        m_connectionModes.get(m_settings.getConnectionMode()).setSelected(true);
        updateConnectionCards();
    }

    @Override
    public void onClose() {
        m_workingDirChooser.removeListener(m_workdirListener);
        m_workingDirChooser.onClose();
    }
}
