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
 *   Jul 20, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.VariableType;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.internal.FSLocationUtils;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.config.FSLocationSpecHandler;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * {@link FSLocationSpecHandler} implementation for FSLocation instances.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
enum FSLocationHandler implements FSLocationSpecHandler<FSLocation> {
        INSTANCE;

    private static final String CFG_OLD = "location";

    static final String CFG_PATH = "path";

    @Override
    public FSLocation load(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            return FSLocationUtils.loadFSLocation(settings.getConfig(CFG_PATH));
        } catch (InvalidSettingsException ex) {
            try {
                return FSLocationUtils.loadFSLocation(settings.getConfig(CFG_OLD));
            } catch (InvalidSettingsException oldNameEx) {
                throw ex;
            }
        }
    }

    @Override
    public void save(final NodeSettingsWO settings, final FSLocation spec) {
        FSLocationUtils.saveFSLocation(spec, settings.addNodeSettings(CFG_PATH));
    }

    @Override
    public FSLocation adapt(final FSLocation oldSpec, final FSLocationSpec newSpec) {
        if (newSpec instanceof FSLocation) {
            return (FSLocation)newSpec;
        } else {
            final String path = oldSpec == null ? "" : oldSpec.getPath();
            return new FSLocation(newSpec.getFileSystemCategory(), newSpec.getFileSystemSpecifier().orElse(null), path);
        }
    }

    @Override
    public StatusMessage warnIfConnectedOverwrittenWithFlowVariable(final FSLocation flowVarLocationSpec) {
        if (flowVarLocationSpec.getFSCategory() == FSCategory.CONNECTED) {
            return new DefaultStatusMessage(MessageType.WARNING,
                "The file system at the input port differs from the connected file system provided via flow variable."
                    + " Only the path (%s) of the variable is used.",
                flowVarLocationSpec.getPath());
        } else {
            return new DefaultStatusMessage(MessageType.WARNING, "Only the path (%s) of the flow variable is used.",
                flowVarLocationSpec.getPath());
        }
    }

    @Override
    public VariableType<FSLocation> getVariableType() {
        return FSLocationVariableType.INSTANCE;
    }

}
