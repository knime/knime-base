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
 * ------------------------------------------------------------------------
 */

package org.knime.filehandling.utility.nodes.deletepaths.table;

import java.util.List;

import org.knime.base.node.io.filehandling.webui.FileSystemManagedByPortMessage;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.utility.nodes.deletepaths.AbstractDeleteFilesAndFoldersNodeConfig;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.message.TextMessage;

/**
 * Node parameters for Delete Files/Folders (Table).
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class DeleteFilesAndFoldersTableNodeParameters implements NodeParameters {

    private static final String CFG_FAIL_IF_DELETE_FAILS = "fail_if_delete_fails";

    @Section(title = "Column Selection")
    interface ColumnSelectionSection {
    }

    @Section(title = "Options")
    @After(ColumnSelectionSection.class)
    interface OptionsSection {
    }

    @TextMessage(value = FileSystemManagedByPortMessage.class)
    Void m_fileSystemManagedByPortMessage;

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Path column", description = "The Path column that contains the paths which should be deleted.")
    @ChoicesProvider(FSLocationColumnChoicesProvider.class)
    @Persist(configKey = DeleteFilesAndFoldersTableNodeConfig.CFG_COLUMN_SELECTION)
    String m_columnSelection;

    @Layout(OptionsSection.class)
    @Widget(title = "Fail if delete fails", description = """
            If this option is checked, the node will fail if one of the files/folders could not be deleted, \
            i.e., it could not be accessed. If it is unchecked, the output table will contain a column that \
            indicates whether a file/folder was successfully deleted or not.""")
    @Persist(configKey = CFG_FAIL_IF_DELETE_FAILS)
    @Migration(FailIfDeleteFailsMigration.class)
    boolean m_failIfDeleteFails = true;

    @Layout(OptionsSection.class)
    @Widget(title = "Fail if file does not exist",
        description = "If this option is checked, the node will fail if one of the files/folders does not exist.")
    @Persist(configKey = DeleteFilesAndFoldersTableNodeConfig.CFG_FAIL_IF_FILE_NOT_EXIST)
    boolean m_failIfFileDoesNotExist = true;

    private static class FSLocationColumnChoicesProvider extends CompatibleColumnsProvider {

        FSLocationColumnChoicesProvider() {
            super(FSLocationValue.class);
        }

    }

    /**
     * Migration for backwards compatibility with the old "abort_if_delete_fails" config key. See
     * {@link AbstractDeleteFilesAndFoldersNodeConfig}.
     */
    private static final class FailIfDeleteFailsMigration implements NodeParametersMigration<Boolean> {

        private static final String LEGACY_CONFIG_KEY = "abort_if_delete_fails";

        @Override
        public List<ConfigMigration<Boolean>> getConfigMigrations() {
            return List.of( //
                ConfigMigration.builder(FailIfDeleteFailsMigration::loadFromLegacyKey)
                    .withDeprecatedConfigPath(LEGACY_CONFIG_KEY).build());
        }

        private static Boolean loadFromLegacyKey(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(LEGACY_CONFIG_KEY);
        }
    }
}
