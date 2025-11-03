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

package org.knime.filehandling.utility.nodes.tempdir;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FolderSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriter;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.message.TextMessage.SimpleTextMessageProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Create Temp Folder.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class CreateTempDir2NodeParameters implements NodeParameters {

    @Section(title = "Output Location")
    interface OutputLocation {
    }

    @Section(title = "File Options")
    @After(OutputLocation.class)
    interface FileOptions {
    }

    @Section(title = "Additional Path Variables",
        description = "A list of additional variables that will be created by the node. Each variable will denote a path to a file or folder.")
    @After(FileOptions.class)
    interface AdditionalPathVariablesSection {
    }

    static final class FileSystemManagedByPortMessage implements SimpleTextMessageProvider {

        @Override
        public boolean showMessage(final NodeParametersInput context) {
            return FileSystemPortConnectionUtil.hasEmptyFileSystemPort(context);
        }

        @Override
        public String title() {
            return "File system managed by File System Input Port";
        }

        @Override
        public String description() {
            return "No file system is currently connected. To proceed, either connect a file system to the input"
                + " port or remove the port.";
        }

        @Override
        public MessageType type() {
            return MessageType.INFO;
        }

    }

    @TextMessage(value = FileSystemManagedByPortMessage.class)
    @Layout(OutputLocation.class)
    Void m_fileSystemManagedByPortMessage;

    @Layout(OutputLocation.class)
    @Modification(LegacyFileWriterModifier.class)
    @Persist(configKey = "temp_dir_location")
    LegacyFileWriter m_targetFolder = new LegacyFileWriter();

    static final class LegacyFileWriterModifier implements LegacyFileWriter.LegacyFileWriterModifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            final var fileSelection = LegacyFileWriter.LegacyFileWriterModifier.findFileSelection(group);
            fileSelection.modifyAnnotation(Widget.class).withProperty("title", "Folder")//
                .withProperty("description",
                    "Select a file system and location for creating the temporary folder. TODO") // TODO update
                .modify();
            fileSelection.addAnnotation(FolderSelectionWidget.class).modify();
        }

    }

    @Layout(FileOptions.class)
    @Widget(title = "Delete temp folder on reset",
        description = "Check this box to delete the folder and all its content when the node is reset.")
    @Persist(configKey = "delete_on_reset")
    boolean m_deleteTempFolderOnReset = true;

    @Layout(FileOptions.class)
    @Widget(title = "Temp folder prefix",
        description = "Name prefix of the temporary folder. It will be amended by some random number to ensure uniqueness.")
    @TextInputWidget
    @Persist(configKey = "temp_dir_prefix")
    String m_tempFolderPrefix = "knimetemp-";

    @Layout(FileOptions.class)
    @Widget(title = "Export path as (variable name)",
        description = "The name of the exported variable denoting the actual path of the created temporary folder.")
    @TextInputWidget
    @Persist(configKey = "temp_dir_path_variable_name")
    String m_tempDirPathVariableName = "temp_dir_path";

    @Layout(AdditionalPathVariablesSection.class)
    @Widget(title = "Additional path variables",
        description = "A list of additional variables that will be created by the node. Each variable will denote a path to a file or folder.")
    @ArrayWidget(addButtonText = "Add path variable", elementTitle = "Path Variable")
    @Persistor(PathVariableArrayPersistor.class)
    PathVariable[] m_additionalPathVariables = {};

    static class PathVariable implements WidgetGroup {

        @Widget(title = "Variable name", description = "The name of the path variable.")
        @TextInputWidget
        String m_variableName = "";

        @Widget(title = "Path", description = "The path relative to the temporary directory.")
        @TextInputWidget
        String m_path = "";

        @Widget(title = "File extension",
            description = "Optional file extension (e.g., .txt, .csv). Leave empty for folder variables.")
        @TextInputWidget
        String m_extension = "";
    }
}
