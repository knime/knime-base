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

package org.knime.filehandling.utility.nodes.transfer.table;

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriter;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Node parameters for Transfer Files (Table).
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class TransferFilesTableNodeParameters implements NodeParameters {

    private static final String CFG_DESTINATION_POLICY_KEY = "from_table";

    @Section(title = "Source", description = "Configure the source file/folder to be copied/moved")
    private interface Source {
    }

    @Section(title = "Destination", description = "Configure the destination file/folder to be copied/moved to")
    @After(Source.class)
    private interface Destination {
    }

    @Widget(title = "Source column", description = """
                Select the column containing the paths to the files/folders that must be copied/moved.
            """)
    @Layout(Source.class)
    @ChoicesProvider(PathColumnChoices.class)
    @Persist(configKey = "source_column")
    String m_sourceColumn;

    @Layout(Source.class)
    @Widget(title = "Fail if source does not exist", description = """
                If the source file/folder to copy/move does not exit the node will fail.
            """)
    @Persist(configKey = "fail_if_source_does_not_exist")
    boolean m_failIfSourceDoesNotExist;

    @Widget(title = "Delete source files / folders", description = """
            Delete all files/folders that have been successfully copied from the source folder. The
            output contains an additional column indicating if the file has been deleted (true) or
            not (false).
            """)
    @Layout(Source.class)
    @Persist(configKey = "delete_source_files_folders")
    @ValueReference(DeleteSourceFilesAndFoldersRef.class)
    boolean m_deleteSourceFilesAndFolders;

    @Widget(title = "Fail on unsuccessful deletion", description = """
            If the 'Delete source files / folders' option is activated and something goes wrong during the
            deletion process the node will fail.
            """)
    @Layout(Source.class)
    @Persist(configKey = "fail_on_unsuccessful_deletion")
    @Effect(predicate = IsDeleteSourceFilesAndFolders.class, type = EffectType.SHOW)
    boolean m_failOnUnsuccessfulDeletion;

    @Widget(title = "Overwrite policy", description = """
                How to handle files to be copied already existing in destination folder
            """)
    @Layout(Destination.class)
    @ValueSwitchWidget
    @Persist(configKey = "transfer_policy")
    OverwritePolicy m_transferPolicy;

    private enum OverwritePolicy {
           @Label(value = "Fail", description = """
                    Will issue an error during the node's execution (to prevent unintentional overwrite).
                    """)
            FAIL, //
           @Label(value = "Ignore", description = """
                    Will ignore if a file already exists and continue the copying process.
                    """)
            IGNORE, //
            @Label(value = "Overwrite", description = "Will replace any existing file.")
            OVERWRITE, //
           @Label(value = "Overwrite if newer", description = """
                    Will replace any existing file if the source file's last modified date is after the
                    destination file's last modified date, otherwise the file will not be overwritten and the
                    copy process will continue.
                    """)
            OVERWRITE_IF_NEWER,
    }

    @Widget(title = "Detailed output", description = """
            If selected the output will not only show the folder that has been copied/moved but also all the
            files/folders it contains.
            """)
    @Layout(Destination.class)
    @Persist(configKey = "detailed_output")
    boolean m_detailedOutput;

    @Widget(title = "Choose destination", description = "Select a method with which to set the destination.")
    @Layout(Destination.class)
    @ValueSwitchWidget
    @ValueReference(DestinationPolicyRef.class)
    @Persistor(DestinationPolicyPersistor.class)
    DestinationPolicy m_fromTable = DestinationPolicy.FROM_TABLE;

    private enum DestinationPolicy {
            @Label(value = "From file chooser", description = """
                    Specify a folder where you want to copy/move the (source) files/folders to.
                    """)
            FILE_CHOOSER, //
            @Label(value = "From table", description = """
                    Select the column containing the destination, i.e., the new location and names of
                    the files/folders to be copied/moved. <br /> <i>Note:</i> If the source references a
                    file/folder the destination also has to be a file/folder.
                    """)
            FROM_TABLE,
    }

    @Widget(title = "Destination column", description = """
            Select the column containing the destination, i.e., the new location and names of the
            files/folders to be copied/moved. <br /> <i>Note:</i> If the source references a file/folder the
            destination also has to be a file/folder.
            """)
    @Layout(Destination.class)
    @ChoicesProvider(PathColumnChoices.class)
    @Effect(predicate = DestinationIsFromTable.class, type = EffectType.SHOW)
    @Persist(configKey = "destination_column")
    String m_destinationColumn;

    @Persist(configKey = "destination_location")
    @Layout(Destination.class)
    @Effect(predicate = DestinationIsFromTable.class, type = EffectType.HIDE)
    @Modification(LegacyFileWriterModifier.class)
    LegacyFileWriter m_targetFolder = new LegacyFileWriter();

    private static final class LegacyFileWriterModifier implements LegacyFileWriter.LegacyFileWriterModifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            final var fileSelection = LegacyFileWriter.LegacyFileWriterModifier.findFileSelection(group);
            fileSelection.modifyAnnotation(Widget.class) //
                .withProperty("title", "Destination path") //
                .withProperty("description", "Select a file system and destination to copy/move files/folders to.") //
                .modify();
            fileSelection.addAnnotation(WithFileSystem.class) //
                .withProperty("value", new FileSystemOption[]{FileSystemOption.LOCAL,
                    FileSystemOption.SPACE, FileSystemOption.EMBEDDED, FileSystemOption.CONNECTED}) //
                .modify();
        }
    }

    @Widget(title = "Destination file path", description = """
            Depending on the selected mode the location of the files/folders to be copied to the destination
            folder will differ.
            """)
    @Layout(Destination.class)
    @Persist(configKey = "destination_file_path")
    @Effect(predicate = DestinationIsFromTable.class, type = EffectType.HIDE)
    @ValueSwitchWidget
    @ValueReference(DestinationPathRef.class)
    DestinationPath m_destinationPath;

    private enum DestinationPath {
            @Label(value = "File/folder only", description = """
                    Copy only the selected file/folder itself into the destination folder, without recreating any
                    parent directories from the source path.
                    """)
            RELATIVE, //
            @Label(value = "Full path", description = """
                    Recreate the full directory structure from the source path inside the destination folder.
                    For example, if the source is <i>src_folder/src_subfolder</i> and the destination is
                    <i>dest_folder</i>, the content will be copied to <i>dest_folder/src_folder/src_subfolder</i>.
                    """)
            KEEP, //
            @Label(value = "Path after prefix", description = """
                    Recreate the directory structure from the source path, but ignore the specified folder prefix.
                    For example, if the prefix is <i>src_folder</i> and the source path is
                    <i>src_folder/src_subfolder</i>, the content will be copied to <i>dest_folder/src_subfolder</i>.
                    <b>Note:</b> In special cases this option may attempt to copy the source to a location outside
                    the specified destination, in which case the node will fail.
                    """)
            REMOVE_FOLDER_PREFIX, //
    }

    @Widget(title = "Folder prefix", description = """
            The folder prefix (the beginning of the path) to be ignored when using 'Path after prefix' mode.
            """)
    @Layout(Destination.class)
    @Persist(configKey = "folder_prefix")
    @Effect(predicate = IncludePolicyIsPrefix.class, type = EffectType.SHOW)
    String m_folderPrefix;

    private static final class DeleteSourceFilesAndFoldersRef implements ParameterReference<Boolean> {
    }

    private static final class DestinationPolicyRef implements ParameterReference<DestinationPolicy> {
    }

    private static final class DestinationPathRef implements ParameterReference<DestinationPath> {
    }

    private static class PathColumnChoices implements ColumnChoicesProvider {

        // The index of the table spec will change based on whether one or both of the source and
        // destination file system ports1 are connected.
        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            var tableSpec = Arrays  //
                .stream(context.getInPortSpecs()) //
                .filter(ps -> ps instanceof DataTableSpec) //
                .findFirst();
            if (tableSpec.isEmpty()) {
                return List.of();
            }
            return ((DataTableSpec)tableSpec.get()).stream() //
                .filter(colSpec -> colSpec.getType().isCompatible(FSLocationValue.class)) //
                .toList();
        }
    }

    private static final class DestinationIsFromTable implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(DestinationPolicyRef.class).isOneOf(DestinationPolicy.FROM_TABLE);
        }
    }

    private static final class IsDeleteSourceFilesAndFolders implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(DeleteSourceFilesAndFoldersRef.class).isTrue();
        }
    }

    private static final class IncludePolicyIsPrefix implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i //
                .getEnum(DestinationPolicyRef.class) //
                .isOneOf(DestinationPolicy.FILE_CHOOSER) //
                .and(i.getEnum(DestinationPathRef.class) //
                    .isOneOf(DestinationPath.REMOVE_FOLDER_PREFIX) //
                );
        }
    }

    private static final class DestinationPolicyPersistor implements NodeParametersPersistor<DestinationPolicy> {
        @Override
        public DestinationPolicy load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var fromTable = settings.getBoolean(CFG_DESTINATION_POLICY_KEY);
            return fromTable ? DestinationPolicy.FROM_TABLE : DestinationPolicy.FILE_CHOOSER;
        }

        @Override
        public void save(final DestinationPolicy param, final NodeSettingsWO settings) {
            switch (param) {
                case FROM_TABLE -> settings.addBoolean(CFG_DESTINATION_POLICY_KEY, true);
                case FILE_CHOOSER -> settings.addBoolean(CFG_DESTINATION_POLICY_KEY, false);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_DESTINATION_POLICY_KEY}};
        }
    }
}
