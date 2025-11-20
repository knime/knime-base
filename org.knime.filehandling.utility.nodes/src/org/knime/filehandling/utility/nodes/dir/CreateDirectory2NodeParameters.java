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

package org.knime.filehandling.utility.nodes.dir;

import static org.knime.filehandling.utility.nodes.dir.CreateDirectory2NodeConfig.CFG_ADDITIONAL_PATH_VARIABLES;
import static org.knime.filehandling.utility.nodes.dir.CreateDirectory2NodeConfig.CFG_DIR_PARENT;
import static org.knime.filehandling.utility.nodes.dir.CreateDirectory2NodeConfig.CFG_DIR_PATH_VARIABLE_NAME;
import static org.knime.filehandling.utility.nodes.dir.CreateDirectory2NodeConfig.DEFAULT_DIR_PATH_VAR_NAME;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.filehandling.utility.nodes.dialog.variables.AbstractPathVariableArrayPersistor;
import org.knime.filehandling.utility.nodes.dialog.variables.PathVariable;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
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

/**
 * Node parameters for Create Folder.
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class CreateDirectory2NodeParameters implements NodeParameters {

    @Section(title = "Output Location")
    interface OutputLocation {
    }

    @Section(title = "Additional Path Variables",
        description = "A list of additional variables that will be created by the node."
            + " Each variable will denote a path to a file or folder.")
    @After(OutputLocation.class)
    interface AdditionalPathVariablesSection {
    }

    @TextMessage(value = FileSystemManagedByPortMessage.class)
    @Layout(OutputLocation.class)
    Void m_fileSystemManagedByPortMessage;

    @Layout(OutputLocation.class)
    @Modification(LegacyFileWriterModifier.class)
    @Persist(configKey = CFG_DIR_PARENT)
    LegacyFileWriter m_targetFolder = new LegacyFileWriter();

    @Layout(OutputLocation.class)
    @Widget(title = "Export path as (variable name)",
        description = "The name of the exported variable denoting the actual path of the created folder.")
    @Persist(configKey = CFG_DIR_PATH_VARIABLE_NAME)
    String m_dirPathVariableName = DEFAULT_DIR_PATH_VAR_NAME;

    @Layout(AdditionalPathVariablesSection.class)
    @Widget(title = "Additional path variables",
        description = """
                A list of additional variables that will be created by the node. Each variable will denote
                a path to a file or folder.
                """)
    @ArrayWidget(addButtonText = "Add path variable", elementTitle = "Path variable")
    @Persistor(PathVariableArrayPersistor.class)
    PathVariable[] m_additionalPathVariables = {};

    private static final class LegacyFileWriterModifier implements LegacyFileWriter.LegacyFileWriterModifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            final var fileSelection = LegacyFileWriter.LegacyFileWriterModifier.findFileSelection(group);
            fileSelection //
                .modifyAnnotation(Widget.class) //
                .withProperty("title", "Folder") //
                // TODO refine via UIEXT-1764
                .withProperty("description", "Select a file system and location for creating the folder.") //
                .modify();
            fileSelection //
                .addAnnotation(FileSelectionWidget.class) //
                .withProperty("value", SingleFileSelectionMode.FOLDER) //
                .modify();
            fileSelection //
                .addAnnotation(WithFileSystem.class) //
                .withProperty("value",
                    new FileSystemOption[]{FileSystemOption.LOCAL, FileSystemOption.SPACE, FileSystemOption.EMBEDDED})
                .modify();
        }

    }

    private static final class PathVariableArrayPersistor extends AbstractPathVariableArrayPersistor {
        public PathVariableArrayPersistor() {
            super(CFG_ADDITIONAL_PATH_VARIABLES, true);
        }
    }

    private static final class FileSystemManagedByPortMessage implements SimpleTextMessageProvider {

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
            return """
                No file system is currently connected. To proceed, either connect a file system to the input
                port and ensure connector node has been executed or remove the port.
                 """;
        }

        @Override
        public MessageType type() {
            return MessageType.INFO;
        }

    }

}
