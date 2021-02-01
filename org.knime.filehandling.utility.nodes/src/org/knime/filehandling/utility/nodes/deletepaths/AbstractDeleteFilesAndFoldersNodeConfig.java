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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * Abstract super class for the configuration of the "Delete Files/Folders" nodes.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractDeleteFilesAndFoldersNodeConfig {

    /** The source file system connection port group name. */
    public static final String CONNECTION_INPUT_PORT_GRP_NAME = "File System Connection";

    /** The destination file system connection port group name. */
    public static final String OUTPUT_PORT_GRP_NAME = "Output Port";

    private static final String CFG_ABORT_IF_FAILS = "abort_if_delete_fails";

    private final SettingsModelBoolean m_abortIfFails = new SettingsModelBoolean(CFG_ABORT_IF_FAILS, true);

    /**
     * Returns the {@link SettingsModelBoolean} for the isAbortedIfFails option.
     *
     * @return the {@link SettingsModelBoolean}
     */
    protected final SettingsModelBoolean isAbortedIfFails() {
        return m_abortIfFails;
    }

    /**
     * Returns a boolean for the istAbortIfFileNotExist option.
     *
     * @return a boolean whether this option is active or not
     */
    protected abstract boolean isAbortIfFileNotExist();

    /**
     * Loads the settings for the model.
     *
     * @param settings the {@link NodeSettingsRO}
     * @throws InvalidSettingsException
     */
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_abortIfFails.loadSettingsFrom(settings);
    }

    /**
     * Saves the settings for the model.
     *
     * @param settings the {@link NodeSettingsWO}
     */
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        m_abortIfFails.saveSettingsTo(settings);
    }

    /**
     * Validates the settings for the model.
     *
     * @param settings the {@link NodeSettingsRO}
     * @throws InvalidSettingsException
     */
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_abortIfFails.validateSettings(settings);
    }
}
