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
 *   Jun 5, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.writer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractSettingsModelFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

/**
 * File chooser settings model for writer nodes. </br>
 * Adds the setting for creating missing folders and the {@link FileOverwritePolicy}.</br>
 * The policy is not stored in the settings if fewer than two policies are supported by the settings model.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public final class SettingsModelWriterFileChooser extends AbstractSettingsModelFileChooser {

    private static final String CFG_CREATE_MISSING_FOLDERS = "create_missing_folders";

    private static final String CFG_FILE_OVERWRITE_POLICY = "if_path_exists";

    private final Set<FileOverwritePolicy> m_supportedPolicies;

    private final FileOverwritePolicy m_defaultPolicy;

    private FileOverwritePolicy m_selectedPolicy;

    private boolean m_createMissingFolders = false;

    /**
     * Constructor.
     *
     * @param configName under which to store the settings
     * @param portsConfig {@link PortsConfiguration} of the corresponding KNIME node
     * @param fileSystemPortIdentifier identifier of the file system port group in <b>portsConfig</b>
     * @param defaultFilterMode the default {@link FilterMode} (may be {@code null} if there is no policy for existing
     *            files)
     * @param defaultPolicy the policy selected by default
     * @param supportedPolicies the policies supported by the corresponding KNIME node (must contain
     *            <b>defaultPolicy</b> or must be empty if defaultPolicy is {@code null}))
     * @param fileExtensions the supported file extensions
     */
    public SettingsModelWriterFileChooser(final String configName, final PortsConfiguration portsConfig,
        final String fileSystemPortIdentifier, final FilterMode defaultFilterMode,
        final FileOverwritePolicy defaultPolicy, final Set<FileOverwritePolicy> supportedPolicies,
        final String... fileExtensions) {
        super(configName, portsConfig, fileSystemPortIdentifier, defaultFilterMode, fileExtensions);
        if (defaultPolicy == null) {
            CheckUtils.checkArgument(supportedPolicies.isEmpty(),
                "There must not be any supported policy if the default policy is null.");
        } else {
            CheckUtils.checkArgument(supportedPolicies.contains(defaultPolicy),
                "The default policy must be among the possible policies.");
        }
        m_defaultPolicy = defaultPolicy;
        m_supportedPolicies = Collections.unmodifiableSet(EnumSet.copyOf(supportedPolicies));
        m_selectedPolicy = defaultPolicy;
    }

    private SettingsModelWriterFileChooser(final SettingsModelWriterFileChooser toCopy) {
        super(toCopy);
        m_selectedPolicy = toCopy.m_selectedPolicy;
        m_createMissingFolders = toCopy.m_createMissingFolders;
        m_supportedPolicies = toCopy.m_supportedPolicies;
        m_defaultPolicy = toCopy.m_defaultPolicy;
    }

    /**
     * Creates a {@link WritePathAccessor} to be used in writer nodes.
     *
     * @return a {@link WritePathAccessor}
     */
    public WritePathAccessor createWritePathAccessor() {
        return createPathAccessor();
    }

    /**
     * Returns an unmodifiable set of the supported {@link FileOverwritePolicy policies}.
     *
     * @return an unmodifiable set of the supported {@link FileOverwritePolicy policies}
     */
    public Set<FileOverwritePolicy> getSupportedPolicies() {
        return m_supportedPolicies;
    }

    /**
     * Returns the selected {@link FileOverwritePolicy}.
     *
     * @return the selected {@link FileOverwritePolicy}
     */
    public FileOverwritePolicy getFileOverwritePolicy() {
        return m_selectedPolicy;
    }

    /**
     * Sets the provided {@link FileOverwritePolicy} and notifies the change listeners if the value changed.
     *
     * @param policy {@link FileOverwritePolicy} to set
     */
    public void setFileOverwritePolicy(final FileOverwritePolicy policy) {
        CheckUtils.checkArgument(m_supportedPolicies.contains(policy), "The policy '%s' is not supported by this node.",
            policy);
        if (policy != m_selectedPolicy) {
            m_selectedPolicy = policy;
            notifyChangeListeners();
        }
    }

    /**
     * Indicates whether missing folders should be created if they don't exist yet.
     *
     * @return {@code true} if missing folders should be created
     */
    public boolean isCreateMissingFolders() {
        return m_createMissingFolders && !isCustomURL();
    }

    boolean isCustomURL() {
        return getLocation().getFSCategory() == FSCategory.CUSTOM_URL;
    }

    boolean isCreateMissingFoldersUI() {
        return m_createMissingFolders;
    }

    /**
     * Sets whether missing folders should be created if they don't exist yet and notifies the change listeners if the
     * value changed.
     *
     * @param createMissingFolders {@code true} if missing folders should be created
     */
    public void setCreateMissingFolders(final boolean createMissingFolders) {
        if (m_createMissingFolders != createMissingFolders) {
            m_createMissingFolders = createMissingFolders;
            notifyChangeListeners();
        }
    }

    @Override
    protected void loadAdditionalSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        setCreateMissingFolders(settings.getBoolean(CFG_CREATE_MISSING_FOLDERS, false));
        if (hasPolicyChoice()) {
            setFileOverwritePolicy(loadPolicyInDialog(settings));
        }
    }

    @Override
    protected void saveAdditionalSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveAdditionalSettingsForDialog(settings);
        settings.addBoolean(CFG_CREATE_MISSING_FOLDERS, m_createMissingFolders);
        if (hasPolicyChoice()) {
            savePolicy(settings);
        }
    }

    @Override
    protected void validateAdditionalSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateAdditionalSettingsForModel(settings);
        settings.getBoolean(CFG_CREATE_MISSING_FOLDERS);
        if (hasPolicyChoice()) {
            loadPolicyInModel(settings);
        }
    }

    @Override
    protected void loadAdditionalSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadAdditionalSettingsForModel(settings);
        setCreateMissingFolders(settings.getBoolean(CFG_CREATE_MISSING_FOLDERS));
        if (hasPolicyChoice()) {
            setFileOverwritePolicy(loadPolicyInModel(settings));
        }
    }

    /**
     * Indicates whether the user has any choice in selecting a {@link FileOverwritePolicy}.
     *
     * @return {@code true} if the user can select a {@link FileOverwritePolicy}
     */
    public boolean hasPolicyChoice() {
        return m_supportedPolicies.size() > 1;
    }

    @Override
    protected void saveAdditionalSettingsForModel(final NodeSettingsWO settings) {
        super.saveAdditionalSettingsForModel(settings);
        settings.addBoolean(CFG_CREATE_MISSING_FOLDERS, m_createMissingFolders);
        if (hasPolicyChoice()) {
            savePolicy(settings);
        }
    }

    private FileOverwritePolicy loadPolicyInDialog(final NodeSettingsRO settings) {
        final String policyText = settings.getString(CFG_FILE_OVERWRITE_POLICY, m_defaultPolicy.getText());
        return getPolicyFromText(policyText).orElse(m_defaultPolicy);
    }

    private Optional<FileOverwritePolicy> getPolicyFromText(final String policyText) {
        return m_supportedPolicies.stream()//
            .filter(p -> p.getText().equals(policyText))//
            .findAny();
    }

    private void savePolicy(final NodeSettingsWO settings) {
        settings.addString(CFG_FILE_OVERWRITE_POLICY, m_selectedPolicy.getText());
    }

    private FileOverwritePolicy loadPolicyInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final String policyText = settings.getString(CFG_FILE_OVERWRITE_POLICY);
        return getPolicyFromText(policyText).orElseThrow(() -> new InvalidSettingsException(
            String.format("The file overwrite policy '%s' is not supported by this node.", policyText)));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelWriterFileChooser createClone() {
        return new SettingsModelWriterFileChooser(this);
    }

    @Override
    protected String getModelTypeID() {
        return "SMID_WriterFileChooser";
    }

}
