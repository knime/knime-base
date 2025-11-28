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

package org.knime.filehandling.utility.nodes.compress.filechooser;

import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.CONNECTED;
import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.CUSTOM_URL;
import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.EMBEDDED;
import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.LOCAL;
import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.SPACE;

import java.util.function.Supplier;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.InputFSPortProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin.PersistEmbedded;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.utility.nodes.compress.AbstractCompressNodeConfig;
import org.knime.filehandling.utility.nodes.compress.table.CompressTableNodeFactory;
import org.knime.filehandling.utility.nodes.truncator.TruncatePathOption;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithCreateMissingFolders;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.node.parameters.persistence.legacy.LegacyMultiFileSelection;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.LegacyPredicateInitializer;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Compress Files/Folder.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class CompressFileChooserNodeParameters implements NodeParameters {

    private static final String CFG_OUTPUT_LOCATION = "destination_location";

    private static final String CFG_ARCHIVE_ENTRY_PATH = "archive_entry_path";

    private static final String CFG_FOLDER_PREFIX = "folder_prefix";

    private static final String CFG_IF_PATH_EXISTS = "if_path_exists";

    @Section(title = "Source")
    interface SourceSection {
    }

    @Section(title = "Destination")
    @After(SourceSection.class)
    interface DestinationSection {
    }

    @Section(title = "Archive Options")
    @After(DestinationSection.class)
    interface ArchiveOptionsSection {
    }

    @Layout(SourceSection.class)
    @Persist(configKey = CompressFileChooserNodeConfig.CFG_INPUT_LOCATION)
    @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FOLDER,
        MultiFileSelectionMode.FILES_IN_FOLDERS})
    @WithFileSystem(value = {CONNECTED, LOCAL, SPACE, EMBEDDED, CUSTOM_URL},
        connectionProvider = SourceFileConnectionProvider.class)
    @ValueReference(SourceFileChooserRef.class)
    LegacyMultiFileSelection m_sourceFileChooser = new LegacyMultiFileSelection(MultiFileSelectionMode.FILE);

    @Layout(DestinationSection.class)
    @Persist(configKey = CFG_OUTPUT_LOCATION)
    @Modification(OutputLocationModification.class)
    LegacyFileWriterWithCreateMissingFolders m_outputLocation = new LegacyFileWriterWithCreateMissingFolders();

    @Layout(DestinationSection.class)
    @Widget(title = "If exists",
        description = "Specify the behavior of the node in case the output file already exists.")
    @ValueSwitchWidget
    @Persist(configKey = CFG_IF_PATH_EXISTS)
    OverwritePolicy m_overwritePolicy = OverwritePolicy.FAIL;

    enum OverwritePolicy {
            @Label(value = "Fail",
                description = "Will issue an error during the node's execution (to prevent unintentional overwrite).")
            FAIL, //
            @Label(value = "Overwrite", description = "Will replace any existing file.")
            OVERWRITE, //
    }

    @Layout(ArchiveOptionsSection.class)
    @Widget(title = "Archive entry path", description = """
            Depending on the selected mode the location of the files/folders to be copied to the destination
            folder will differ.
            """)
    @ValueSwitchWidget
    @ValueReference(TruncatePathOptionRef.class)
    @Persist(configKey = CFG_ARCHIVE_ENTRY_PATH)
    TruncatePathOptionForCompression m_truncatePathOption = TruncatePathOptionForCompression.RELATIVE;

    @Layout(ArchiveOptionsSection.class)
    @Widget(title = "Folder prefix", description = """
            The folder prefix (the beginning of the path) to be ignored when using 'Path after prefix' mode.
            """)
    @Persist(configKey = CFG_FOLDER_PREFIX)
    @PersistEmbedded
    @Effect(predicate = TruncatePathOptionIsPrefix.class, type = EffectType.SHOW)
    /* currently, <folder prefix> is the default value, not the placeholder (which would be better suited here)
     * See {@link TruncationSettings}
     */
    @TextInputWidget(placeholder = "<folder prefix>")
    String m_folderPrefix;

    @Layout(ArchiveOptionsSection.class)
    @Widget(title = "Compression", description = "Select the desired compression format.")
    @Persistor(CompressionFormat.Persistor.class)
    @Migrate
    @ValueReference(CompressionRef.class)
    CompressionFormat m_compression = CompressionFormat.ZIP;

    @Layout(ArchiveOptionsSection.class)
    @Widget(title = "Include empty folders",
        description = "This option allows to specify whether or not empty folders should be included in the archive.")
    @Persist(configKey = "include_empty_folders")
    @Effect(predicate = SourceFilterModeIsFolder.class, type = EffectType.SHOW)
    boolean m_includeEmptyFolders;

    private static class OutputLocationModification implements LegacyFileWriterWithOverwritePolicyOptions.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            findFileSelection(group) //
                .modifyAnnotation(Widget.class) //
                .withProperty("title", "File") //
                .withProperty("description",
                    "Enter a valid path where the created archive should get saved. The required syntax of a "
                        + "path depends on the chosen file system, such as \"C:\\path\\to\\file\" (Local File "
                        + "System on Windows) or \"/path/to/file\" (Local File System on Linux/MacOS and "
                        + "Mountpoint). For file systems connected via input port, the node description of the "
                        + "respective connector node describes the required path format. You can also choose a "
                        + "previously selected folder from the drop-down list, or select a location from the "
                        + "\"Browse...\" dialog. Note that browsing is disabled in some cases: "
                        + "Custom/KNIME URL (always disabled), Mountpoint (disabled if not connected), "
                        + "and file systems provided via input port (disabled if connector node hasn't been "
                        + "executed).") //
                .modify();
            findFileSelection(group) //
                .addAnnotation(FileSelectionWidget.class) //
                .withValue(SingleFileSelectionMode.FILE) //
                .modify();
            findFileSelection(group) //
                .addAnnotation(WithFileSystem.class) //
                .withValue(new FileSystemOption[]{CONNECTED, LOCAL, SPACE, EMBEDDED, CUSTOM_URL})
                .withProperty("connectionProvider", OutputLocationConnectionProvider.class) //
                .modify();
            findFileSelection(group) //
                .addAnnotation(ValueReference.class) //
                .withValue(OutputLocationFileSelectionRef.class) //
                .modify();
            findFileSelection(group) //
                .addAnnotation(ValueProvider.class) //
                .withValue(OutputLocationExtensionChanger.class) //
                .modify();
            findFileSelection(group) //
                .modifyAnnotation(FileWriterWidget.class) //
                .withProperty("fileExtensionProvider", OutputLocationFileExtensionProvider.class) //
                .modify();
        }
    }

    private static final class SourceFileChooserRef implements ParameterReference<LegacyMultiFileSelection> {
    }

    private static final class SourceFileConnectionProvider extends InputFSPortProvider {
        @Override
        protected String getGroupId() {
            return AbstractCompressNodeConfig.CONNECTION_INPUT_FILE_PORT_GRP_NAME;
        }
    }

    private static final class OutputLocationFileSelectionRef implements ParameterReference<FileSelection> {
    }

    private static final class OutputLocationConnectionProvider extends InputFSPortProvider {
        @Override
        protected String getGroupId() {
            return AbstractCompressNodeConfig.CONNECTION_OUTPUT_DIR_PORT_GRP_NAME;
        }
    }

    /**
     * Adapt the extension of the output location when the compression format changes.
     */
    private static final class OutputLocationExtensionChanger implements StateProvider<FileSelection> {

        Supplier<CompressionFormat> m_compression;

        Supplier<FileSelection> m_outputLocation;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_compression = initializer.computeFromValueSupplier(CompressionRef.class);
            m_outputLocation = initializer.getValueSupplier(OutputLocationFileSelectionRef.class);
        }

        @Override
        public FileSelection computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var oldOutputLocation = m_outputLocation.get();
            final var oldFsLocation = oldOutputLocation.getFSLocation();
            final var oldPath = oldFsLocation.getPath();
            final var compression = m_compression.get();
            final var newExtension = "." + compression.toConfigString();

            // if path ends with any of the known extensions, replace it
            for (final var format : CompressionFormat.values()) {
                final var knownExtension = "." + format.toConfigString();
                if (oldPath.endsWith(knownExtension)) {
                    final var newPath = oldPath.substring(0, oldPath.length() - knownExtension.length()) + newExtension;
                    final var newLocation = new FSLocation(oldFsLocation.getFSCategory(),
                        oldFsLocation.getFileSystemSpecifier().orElse(null), newPath);
                    return new FileSelection(newLocation);
                }
            }
            throw new StateComputationFailureException();
        }
    }

    private static final class OutputLocationFileExtensionProvider implements StateProvider<String> {

        Supplier<CompressionFormat> m_compression;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_compression = initializer.computeFromValueSupplier(CompressionRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var compression = m_compression.get();
            return compression.toConfigString();
        }
    }

    /**
     * Mirrored from {@link TruncatePathOption} to allow specifying node-specific labels & descriptions.
     */
    private enum TruncatePathOptionForCompression {

            @Label(value = "File/folder only", description = """
                    This mode ensures that when de-compressing the archive solely the selected source file/folder
                    and its content will be created.
                    """)
            RELATIVE, //
            @Label(value = "Full path", description = """
                    This mode ensures that when de-compressing the archive the selected source file/folder,
                    its content as well as the folders containing the selected source will be created.
                    Note that those additional folders don't contain any other files/folders other than
                    those to be compressed.
                    """)
            KEEP, //
            @Label(value = "Path after prefix", description = """
                    This mode behaves similar to the 'Full path' option, except that the specified folder
                    prefix (the beginning of the path) is being ignored, i.e., the folders in
                    the prefix will not be created when de-compressing the archive.
                    <b>Note:</b> This mode might create invalid archive entries, i.e.,
                    entries resulting in archives that would not be de-compressible.
                    In this case the node will fail.
                    """)
            REMOVE_FOLDER_PREFIX, //
    }

    private interface TruncatePathOptionRef extends ParameterReference<TruncatePathOptionForCompression> {
    }

    private static final class TruncatePathOptionIsPrefix implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(TruncatePathOptionRef.class) //
                .isOneOf(TruncatePathOptionForCompression.REMOVE_FOLDER_PREFIX);
        }
    }

    private static final class SourceFilterModeIsFolder implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return ((LegacyPredicateInitializer)i).getLegacyMultiFileSelection(SourceFileChooserRef.class)
                .getSelectionMode().isOneOf(MultiFileSelectionMode.FOLDER);
        }
    }

    private static final class CompressionRef implements ParameterReference<CompressionFormat> {
    }

    /**
     * See {@link AbstractCompressNodeConfig} for supported compression formats. This (and the persistor) could be
     * extracted if the {@link CompressTableNodeFactory} is also migrated.
     */
    enum CompressionFormat {
            @Label(value = "zip", description = "ZIP compression format")
            ZIP, //
            @Label(value = "jar", description = "JAR compression format")
            JAR, //
            @Label(value = "tar", description = "TAR compression format (uncompressed)")
            TAR, //
            @Label(value = "tar.gz", description = "TAR with GZIP compression")
            TAR_GZ, //
            @Label(value = "tar.bz2", description = "TAR with BZIP2 compression")
            TAR_BZ2, //
            @Label(value = "cpio", description = "CPIO compression format")
            CPIO;

        private static final String BZ2_EXTENSION = "bz2";

        private static final String GZ_EXTENSION = "gz";

        String toConfigString() {
            return switch (this) {
                case ZIP -> ArchiveStreamFactory.ZIP;
                case JAR -> ArchiveStreamFactory.JAR;
                case TAR -> ArchiveStreamFactory.TAR;
                case TAR_GZ -> ArchiveStreamFactory.TAR + "." + GZ_EXTENSION;
                case TAR_BZ2 -> ArchiveStreamFactory.TAR + "." + BZ2_EXTENSION;
                case CPIO -> ArchiveStreamFactory.CPIO;
            };
        }

        static CompressionFormat fromConfigString(final String value) throws InvalidSettingsException {
            return switch (value) {
                case ArchiveStreamFactory.ZIP -> ZIP;
                case ArchiveStreamFactory.JAR -> JAR;
                case ArchiveStreamFactory.TAR -> TAR;
                case ArchiveStreamFactory.TAR + "." + GZ_EXTENSION -> TAR_GZ;
                case ArchiveStreamFactory.TAR + "." + BZ2_EXTENSION -> TAR_BZ2;
                case ArchiveStreamFactory.CPIO -> CPIO;
                default -> throw new InvalidSettingsException("Unknown compression format: " + value);
            };
        }

        private static final class Persistor implements NodeParametersPersistor<CompressionFormat> {

            private static final String CFG_COMPRESSION = "compression";

            @Override
            public CompressionFormat load(final NodeSettingsRO settings) throws InvalidSettingsException {
                String value = settings.getString(CFG_COMPRESSION, ArchiveStreamFactory.ZIP);
                return CompressionFormat.fromConfigString(value);
            }

            @Override
            public void save(final CompressionFormat param, final NodeSettingsWO settings) {
                settings.addString(CFG_COMPRESSION, param.toConfigString());
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[][]{{CFG_COMPRESSION}};
            }
        }
    }

}
