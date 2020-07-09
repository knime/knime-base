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
 */
package org.knime.filehandling.core.node.portobject.writer;

import java.util.EnumSet;
import java.util.Set;

import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.portobject.PortObjectIONodeConfig;
import org.knime.filehandling.core.node.portobject.SelectionMode;

/**
 * Configuration class for port object writer nodes that can be extended with additional configurations.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public class PortObjectWriterNodeConfig extends PortObjectIONodeConfig<SettingsModelWriterFileChooser> {

    private static final String[] EMPTY_SUFFIX = new String[0];

    /**
     * Constructor for configurations for which the default filter mode is set to the default associated with the given
     * {@link SelectionMode} and no valid suffixes are available.
     *
     * @param creationConfig {@link NodeCreationConfiguration} of the corresponding KNIME node
     * @param defaultSelectionMode the default {@link SelectionMode}
     */
    public PortObjectWriterNodeConfig(final NodeCreationConfiguration creationConfig,
        final SelectionMode defaultSelectionMode) {
        this(creationConfig, new String[0], defaultSelectionMode.getDefaultFilter());
    }

    /**
     * Constructor for configurations for which the default filter mode is set to the default associated with the given
     * {@link SelectionMode}, the set of available {@link FileOverwritePolicy FileOverwritePolicys} is configurable, and
     * no valid suffixes are available.
     *
     * @param creationConfig {@link NodeCreationConfiguration} of the corresponding KNIME node
     * @param defaultSelectionMode the default {@link SelectionMode}
     * @param defaultPolicy the policy selected by default
     * @param supportedPolicies the policies supported by the corresponding KNIME node (must contain
     *            <b>defaultPolicy</b> or must be empty if defaultPolicy is {@code null}))
     */
    public PortObjectWriterNodeConfig(final NodeCreationConfiguration creationConfig,
        final SelectionMode defaultSelectionMode, final FileOverwritePolicy defaultPolicy,
        final Set<FileOverwritePolicy> supportedPolicies) {
        this(creationConfig, new String[0], defaultSelectionMode.getDefaultFilter(), defaultPolicy, supportedPolicies);
    }

    /**
     * Constructor for configurations for which the file filter mode is {@link SelectionMode#FILE} and no file suffixes
     * are available.
     *
     * @param creationConfig {@link NodeCreationConfiguration} of the corresponding KNIME node
     */
    public PortObjectWriterNodeConfig(final NodeCreationConfiguration creationConfig) {
        this(creationConfig, EMPTY_SUFFIX);
    }

    /**
     * Constructor for configurations for which the default filter mode is {@link SelectionMode#FILE} and a set of valid
     * suffixes is available.
     *
     * @param creationConfig {@link NodeCreationConfiguration} of the corresponding KNIME node
     * @param fileSuffixes the suffixes to filter on
     */
    public PortObjectWriterNodeConfig(final NodeCreationConfiguration creationConfig, final String[] fileSuffixes) {
        this(creationConfig, fileSuffixes, FilterMode.FILE);
    }

    /**
     * Constructor for configurations for which the default filter mode is set according the provided {@link FilterMode}
     * and a set of valid suffixes is available.
     *
     * @param creationConfig {@link NodeCreationConfiguration} of the corresponding KNIME node
     * @param fileSuffixes the suffixes to filter on
     * @param defaultFilterMode the default {@link FilterMode}
     */
    private PortObjectWriterNodeConfig(final NodeCreationConfiguration creationConfig, final String[] fileSuffixes,
        final FilterMode defaultFilterMode) {
        this(creationConfig, fileSuffixes, defaultFilterMode, FileOverwritePolicy.FAIL,
            EnumSet.of(FileOverwritePolicy.OVERWRITE, FileOverwritePolicy.FAIL));
    }

    /**
     * Constructor for configurations for which the default filter mode is set according the provided
     * {@link FilterMode}, the set of available {@link FileOverwritePolicy FileOverwritePolicys} is configurable, and a
     * set of valid suffixes is available.
     *
     * @param creationConfig {@link NodeCreationConfiguration} of the corresponding KNIME node
     * @param fileSuffixes the suffixes to filter on
     * @param defaultFilterMode the default {@link FilterMode}
     * @param defaultPolicy the policy selected by default
     * @param supportedPolicies the policies supported by the corresponding KNIME node (must contain
     *            <b>defaultPolicy</b> or must be empty if defaultPolicy is {@code null}))
     */
    private PortObjectWriterNodeConfig(final NodeCreationConfiguration creationConfig, final String[] fileSuffixes,
        final FilterMode defaultFilterMode, final FileOverwritePolicy defaultPolicy,
        final Set<FileOverwritePolicy> supportedPolicies) {
        super(new SettingsModelWriterFileChooser(CFG_FILE_CHOOSER,
            creationConfig.getPortConfig().orElseThrow(IllegalStateException::new), CONNECTION_INPUT_PORT_GRP_NAME,
            defaultFilterMode, defaultPolicy, supportedPolicies, fileSuffixes));
    }

}
