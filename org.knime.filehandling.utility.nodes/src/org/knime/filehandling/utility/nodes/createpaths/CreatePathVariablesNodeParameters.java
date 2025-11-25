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
 *
 * History
 *   24 Nov 2025 (Thomas Reifenberger, TNG Technology Consulting GmbH): created
 *
 */

package org.knime.filehandling.utility.nodes.createpaths;

import static org.knime.filehandling.utility.nodes.createpaths.CreatePathVariablesNodeConfig.CFG_DIR_PARENT;
import static org.knime.filehandling.utility.nodes.createpaths.CreatePathVariablesNodeConfig.CFG_FILE_FOLDER_VARIABLES;

import org.knime.base.node.io.filehandling.webui.FileSystemManagedByPortMessage;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.utility.nodes.dialog.variables.AbstractPathVariableArrayPersistor;
import org.knime.filehandling.utility.nodes.dialog.variables.PathVariable;
import org.knime.node.parameters.NodeParameters;
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

/**
 * Node parameters for Create File/Folder Variables.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class CreatePathVariablesNodeParameters implements NodeParameters {

    @SuppressWarnings("java:S1176")
    interface DialogLayout {

        @Section(title = "Base Location")
        interface BaseLocation {
        }

        @Section(title = "File/Folder Variables")
        @After(BaseLocation.class)
        interface FileFolderVariables {
        }

    }

    @TextMessage(value = FileSystemManagedByPortMessage.class)
    @Layout(DialogLayout.BaseLocation.class)
    Void m_fileSystemManagedByPortMessage;

    @Layout(DialogLayout.BaseLocation.class)
    @Modification(LegacyFileWriterModifier.class)
    @Persist(configKey = CFG_DIR_PARENT)
    LegacyFileWriter m_baseFolder = new LegacyFileWriter();

    @Layout(DialogLayout.FileFolderVariables.class)
    @Widget(title = "File/Folder variables",
        description = "A list of file/folder locations that will form a path with the selected base folder. "
            + "Enter the name of the flow variable in the <i>Variable name</i> column and write the name of the "
            + "file/folder in the <i>Value</i> column. In case of a file also fill in the <i>File extension</i> "
            + "column. These will be added to the path shown in the <i>Base location</i> column.")
    @ArrayWidget(addButtonText = "Add path variable", elementTitle = "Path Variable")
    @Persistor(PathVariableArrayPersistor.class)
    PathVariable[] m_additionalPathVariables = {};

    private static final class LegacyFileWriterModifier implements LegacyFileWriter.Modifier {
        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            final var fileSelection = findFileSelection(group);
            fileSelection //
                .modifyAnnotation(Widget.class) //
                .withProperty("title", "Folder") //
                .withProperty("description", "Select a base folder for your paths.") //
                .modify();
            fileSelection //
                .addAnnotation(FileSelectionWidget.class) //
                .withProperty("value", SingleFileSelectionMode.FOLDER) //
                .modify();
        }
    }

    private static final class PathVariableArrayPersistor extends AbstractPathVariableArrayPersistor {

        PathVariableArrayPersistor() {
            super(CFG_FILE_FOLDER_VARIABLES);
        }
    }
}
