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

package org.knime.filehandling.utility.nodes.decompress;

import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.CONNECTED;
import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.CUSTOM_URL;
import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.EMBEDDED;
import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.LOCAL;
import static org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption.SPACE;

import java.util.List;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.knime.base.node.io.filehandling.webui.FileEncodingParameters;
import org.knime.base.node.io.filehandling.webui.FileEncodingParameters.FileEncodingOption;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.InputFSPortProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriter;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicyChoicesProvider;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;

/**
 * Node parameters for Decompress Files.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
final class DecompressNodeParameters implements NodeParameters {

    @Section(title = "Source")
    interface SourceSection {
    }

    @Section(title = "Destination")
    @After(SourceSection.class)
    interface DestinationSection {
    }

    @Section(title = "Encoding")
    @Advanced
    @After(DestinationSection.class)
    interface EncodingSection {
    }

    @Layout(SourceSection.class)
    @Widget(title = "Source file", description = """
            Select a file system from which you want to unzip your archive or decompress your file. You can specify
            the path to an archive file to be decompressed. The node supports various compressed and archive formats
            including .zip, .jar, .tar, .tar.gz, .tar.bz2, .cpio, .ar, and .gzip files.
            """)
    @Modification(SourceFileModifier.class)
    @FileReaderWidget(fileExtensions = {//
        "." + ArchiveStreamFactory.ZIP, //
        "." + ArchiveStreamFactory.JAR, //
        "." + ArchiveStreamFactory.TAR, //
        "." + ArchiveStreamFactory.TAR + "." + CompressorStreamFactory.GZIP, //
        "." + ArchiveStreamFactory.TAR + "." + DecompressNodeConfig.BZ2_EXTENSION, //
        "." + ArchiveStreamFactory.CPIO, //
        "." + ArchiveStreamFactory.AR, //
        "." + CompressorStreamFactory.GZIP, //
        "." + DecompressNodeConfig.GZIP_EXTENSION}
    )
    @Persist(configKey = DecompressNodeConfig.CFG_INPUT_FILE)
    LegacyFileWriter m_sourceFile = new LegacyFileWriter();

    static final class SourceFileModifier implements LegacyFileWriter.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            final var fileSelection = findFileSelection(group);
            fileSelection //
                .modifyAnnotation(Widget.class) //
                .withProperty("title", "File") //
                .withProperty("description", """
                        Enter a path to an archive file to be decompressed. The required syntax of a path depends on
                        the chosen file system, such as "C:\\path\\to\\file" (Local File System on Windows) or
                        "/path/to/file" (Local File System on Linux/MacOS and Mountpoint). For file systems connected
                        via input port, the node description of the respective connector node describes the required
                        path format. You can also choose a previously selected file from the drop-down list, or select
                        a file from the "Browse..." dialog. Note that browsing is disabled in some cases.
                        """) //
                .modify();
            fileSelection //
                .addAnnotation(FileSelectionWidget.class) //
                .withValue(SingleFileSelectionMode.FILE) //
                .modify();
            fileSelection //
                .addAnnotation(WithFileSystem.class) //
                .withValue(new FileSystemOption[]{CONNECTED, LOCAL, SPACE, EMBEDDED, CUSTOM_URL}) //
                .withProperty("connectionProvider", SourceFileConnectionProvider.class) //
                .modify();
        }

    }

    private static final class SourceFileConnectionProvider extends InputFSPortProvider {

        @Override
        protected String getGroupId() {
            return DecompressNodeFactory.CONNECTION_INPUT_FILE_PORT_GRP_NAME;
        }

    }

    @Layout(DestinationSection.class)
    @Widget(title = "Destination folder", description = """
            Select a file system and specify the folder where the decompressed files should be extracted to. The node
            will create the specified folder if it doesn't exist (when the 'Create missing folders' option is enabled).
            """)
    @Modification(DestinationFolderModifier.class)
    @Persist(configKey = DecompressNodeConfig.CFG_OUTPUT_LOCATION)
    LegacyFileWriterWithOverwritePolicyOptions m_destinationFolder = new LegacyFileWriterWithOverwritePolicyOptions();

    static final class DestinationFolderModifier implements LegacyFileWriterWithOverwritePolicyOptions.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            final var fileSelection = findFileSelection(group);
            fileSelection //
                .modifyAnnotation(Widget.class) //
                .withProperty("title", "Folder") //
                .withProperty("description", """
                        Enter a valid directory path where the files should be extracted to. The required syntax of a
                        path depends on the chosen file system, such as "C:\\path\\to\\folder" (Local File System on
                        Windows) or "/path/to/folder" (Local File System on Linux/MacOS and Mountpoint). For file
                        systems connected via input port, the node description of the respective connector node
                        describes the required path format. You can also choose a previously selected folder from the
                        drop-down list, or select a location from the "Browse..." dialog.
                        """) //
                .modify();
            fileSelection //
                .addAnnotation(FileSelectionWidget.class) //
                .withValue(SingleFileSelectionMode.FOLDER) //
                .modify();
            fileSelection //
                .addAnnotation(WithFileSystem.class) //
                .withValue(new FileSystemOption[]{CONNECTED, LOCAL, SPACE, EMBEDDED}) //
                .withProperty("connectionProvider", DestinationFolderConnectionProvider.class) //
                .modify();
            restrictOverwritePolicyOptions(group, DecompressNodeOverwritePolicyChoicesProvider.class);
        }

        static final class DecompressNodeOverwritePolicyChoicesProvider extends OverwritePolicyChoicesProvider {

            @Override
            protected List<OverwritePolicy> getChoices() {
                return List.of(OverwritePolicy.overwrite, OverwritePolicy.ignore, OverwritePolicy.fail);
            }

        }

    }

    private static final class DestinationFolderConnectionProvider extends InputFSPortProvider {

        @Override
        protected String getGroupId() {
            return DecompressNodeFactory.CONNECTION_OUTPUT_DIR_PORT_GRP_NAME;
        }

    }

    @Layout(EncodingSection.class)
    @Widget(title = "Guess file name encoding from archive file extension", description = """
            If selected, tries to guess the encoding for the names of the files to decompress based on file extension
            of the archive file. For example, it will use UTF-8 for .zip files,
            <a href="https://en.wikipedia.org/wiki/Code_page_437">CP437</a> for .arj, and
            <a href="https://en.wikipedia.org/wiki/ASCII">US-ASCII</a> for .cpio. Unfortunately, the encoding cannot
            always be correctly guessed. If you notice that the names of decompressed files contain '?' characters,
            then the encoding was wrongly guessed. In this case, uncheck the box and pick the correct encoding.
            """)
    @Persist(configKey = DecompressNodeConfig.CFG_GUESS_ENCODING)
    @ValueReference(IsGuessEncodingEnabled.class)
    boolean m_guessEncoding = true;

    static final class IsGuessEncodingEnabled implements BooleanReference {
    }

    @Layout(EncodingSection.class)
    @Persistor(FileEncodingPersistor.class)
    @Effect(predicate = IsGuessEncodingEnabled.class, type = EffectType.DISABLE)
    @Modification(CharsetModification.class)
    FileEncodingParameters m_charSet = new FileEncodingParameters();

    static final class CharsetModification implements Modification.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            FileEncodingParameters.changeEffectPredicateProviderOfCustomEncoding(
                group, IsGuessEncodingEnabledAndOtherEncodingSelected.class);
            FileEncodingParameters.generalizeFileEncodingDescription(group);
        }

    }

    static final class IsGuessEncodingEnabledAndOtherEncodingSelected implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(FileEncodingParameters.IsOtherEncoding.class)
                    .and(i.getBoolean(IsGuessEncodingEnabled.class).isFalse());
        }

    }

    static class FileEncodingPersistor implements NodeParametersPersistor<FileEncodingParameters> {

        @Override
        public FileEncodingParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var currentCharsetName =
                    settings.getString(DecompressNodeConfig.CFG_CHARSET, DecompressNodeConfig.DEFAULT_CHARSET);
            final var currentEncoding = FileEncodingOption.fromCharsetName(currentCharsetName);
            return new FileEncodingParameters(currentEncoding,
                currentEncoding == FileEncodingOption.OTHER ? currentCharsetName : null);
        }

        @Override
        public void save(final FileEncodingParameters param, final NodeSettingsWO settings) {
            final var fileEncoding = param.getFileEncoding();
            final var customEncoding = param.getCustomEncoding();
            final var charsetName =
                fileEncoding == FileEncodingOption.OTHER ? customEncoding : fileEncoding.getCharsetName();
            settings.addString(DecompressNodeConfig.CFG_CHARSET, charsetName);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DecompressNodeConfig.CFG_CHARSET}};
        }

    }

}
