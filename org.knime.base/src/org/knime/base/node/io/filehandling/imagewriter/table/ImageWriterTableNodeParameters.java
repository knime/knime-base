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

package org.knime.base.node.io.filehandling.imagewriter.table;

import java.util.List;

import org.knime.core.data.image.ImageValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * Node parameters for Image Writer (Table).
 *
 * @author GitHub Copilot, KNIME GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ImageWriterTableNodeParameters implements NodeParameters {

    @Section(title = "Output location")
    interface OutputLocation {
    }

    @Section(title = "Image")
    @After(OutputLocation.class)
    interface ImageColumn {
    }

    @Section(title = "File names")
    @After(ImageColumn.class)
    interface FileNames {
    }

    @Persist(configKey = "output_location")
    @Modification(FileChooserModifier.class)
    LegacyFileWriterWithOverwritePolicyOptions m_fileChooser = new LegacyFileWriterWithOverwritePolicyOptions();

    private static final class FileChooserModifier implements LegacyFileWriterWithOverwritePolicyOptions.Modifier {
        @Override
        public void modify(final WidgetGroupModifier group) {
            final var fileSelection = findFileSelection(group);
            fileSelection.modifyAnnotation(Widget.class) //
                .withProperty("title", "Folder") //
                .withProperty("description",
                    "Select a folder where the image files will be stored. You can specify the file system and path.")
                .modify();
            fileSelection.addAnnotation(FileSelectionWidget.class) //
                .withProperty("value", SingleFileSelectionMode.FOLDER) //
                .modify();
            fileSelection.addAnnotation(WithFileSystem.class) //
                .withProperty("value",
                    new FileSystemOption[]{FileSystemOption.LOCAL, FileSystemOption.SPACE, FileSystemOption.EMBEDDED,
                        FileSystemOption.CONNECTED, FileSystemOption.CUSTOM_URL}) //
                .modify();

            final var createMissingFolders = findCreateMissingFolders(group);
            createMissingFolders.modifyAnnotation(Widget.class) //
                .withProperty("description",
                    "If enabled, missing folders in the output path will be created automatically. "
                        + "If disabled, the node will fail if any folder in the path does not exist.") //
                .modify();

            restrictOverwritePolicyOptions(group, ImageWriterOverwritePolicyChoicesProvider.class);
        }
    }

    private static final class ImageWriterOverwritePolicyChoicesProvider
        extends LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicyChoicesProvider {
        @Override
        protected List<OverwritePolicy> getChoices() {
            return List.of(//
                OverwritePolicy.fail, //
                OverwritePolicy.overwrite, //
                OverwritePolicy.ignore //
            );
        }
    }

    @Layout(ImageColumn.class)
    @Widget(title = "Image column", description = "Select the column containing the images to write to files.")
    @ChoicesProvider(ImageColumnChoicesProvider.class)
    @Persist(configKey = "image_column")
    String m_imageColumn = "";

    private static final class ImageColumnChoicesProvider extends CompatibleColumnsProvider {
        protected ImageColumnChoicesProvider() {
            super(ImageValue.class);
        }
    }

    @Layout(ImageColumn.class)
    @Widget(title = "Remove image column",
        description = "If enabled, the image column will be excluded from the output table. Only the file paths "
            + "of the written images will be included.")
    @Persist(configKey = "remove_image_column")
    boolean m_removeImageColumn;

    @Layout(FileNames.class)
    @Persistor(FileNamingPersistor.class)
    FileNamingSettings m_fileNaming = new FileNamingSettings();

    static final class FileNamingSettings implements NodeParameters {
        @Widget(title = "Generate file names",
            description = "If enabled, file names will be generated using a pattern with an auto-incrementing "
                + "counter. If disabled, file names will be taken from a column in the input table.")
        @ValueReference(GenerateFileNamesRef.class)
        boolean m_generateFileNames = true;

        @Widget(title = "File name pattern",
            description = "A pattern for generated file names. Must contain exactly one '?' which will be replaced "
                + "by an auto-incrementing counter to ensure unique file names. The file extension will be "
                + "determined automatically based on the image format and should not be included in the pattern. "
                + "Example: 'Image_?' will create files like 'Image_0.png', 'Image_1.png', etc.")
        @Effect(predicate = IsGenerateFileNames.class, type = Effect.EffectType.SHOW)
        String m_fileNamePattern = "File_?";

        @Widget(title = "File name column",
            description = "Select the column containing the file names for the output images, or select RowID to use "
                + "row IDs as file names. The file extension will be determined automatically and should not be "
                + "included in the column values.")
        @ChoicesProvider(FileNameColumnChoicesProvider.class)
        @Effect(predicate = IsFromColumnFileNames.class, type = Effect.EffectType.SHOW)
        StringOrEnum<RowIDChoice> m_fileNameColumn = new StringOrEnum<>("");

        interface GenerateFileNamesRef extends ParameterReference<Boolean> {
        }

        static final class IsGenerateFileNames implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final EffectPredicateProvider.PredicateInitializer i) {
                return i.getBoolean(GenerateFileNamesRef.class).isTrue();
            }
        }

        static final class IsFromColumnFileNames implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final EffectPredicateProvider.PredicateInitializer i) {
                return i.getBoolean(GenerateFileNamesRef.class).isFalse();
            }
        }
    }

    private static final class FileNameColumnChoicesProvider extends CompatibleColumnsProvider.StringColumnsProvider {
        protected FileNameColumnChoicesProvider() {
            super();
        }
    }

    static final class FileNamingPersistor implements NodeParametersPersistor<FileNamingSettings> {
        private static final String LEGACY_ROW_ID_VALUE = "$RowID$";

        @Override
        public FileNamingSettings load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var result = new FileNamingSettings();
            // Handle missing keys gracefully for backwards compatibility
            result.m_generateFileNames = settings.getBoolean("generate_file_names", true);
            result.m_fileNamePattern = settings.getString("filename_pattern", "File_?");

            // Handle different legacy formats for filename_column
            if (settings.containsKey("filename_column")) {
                try {
                    // Try to load as nested config (SettingsModelColumnName format)
                    final var columnSettings = settings.getNodeSettings("filename_column");
                    final boolean useRowID = columnSettings.getBoolean("useRowID", false);
                    if (useRowID) {
                        result.m_fileNameColumn = new StringOrEnum<>(RowIDChoice.ROW_ID);
                    } else {
                        final String columnName = columnSettings.getString("columnName", "");
                        result.m_fileNameColumn = new StringOrEnum<>(columnName);
                    }
                } catch (InvalidSettingsException e) {
                    // Fall back to reading as simple string (e.g., "$RowID$" from very old workflows)
                    final String loadedColumn = settings.getString("filename_column");
                    if (LEGACY_ROW_ID_VALUE.equals(loadedColumn)) {
                        result.m_fileNameColumn = new StringOrEnum<>(RowIDChoice.ROW_ID);
                    } else {
                        result.m_fileNameColumn = new StringOrEnum<>(loadedColumn);
                    }
                }
            } else {
                // Default value for missing key
                result.m_fileNameColumn = new StringOrEnum<>("");
            }
            return result;
        }

        @Override
        public void save(final FileNamingSettings obj, final NodeSettingsWO settings) {
            settings.addBoolean("generate_file_names", obj.m_generateFileNames);
            settings.addString("filename_pattern", obj.m_fileNamePattern);

            // Save in SettingsModelColumnName format (nested config with useRowID and columnName)
            final var columnSettings = settings.addNodeSettings("filename_column");
            if (obj.m_fileNameColumn.getEnumChoice().isPresent()) {
                columnSettings.addBoolean("useRowID", true);
                columnSettings.addString("columnName", "");
            } else {
                columnSettings.addBoolean("useRowID", false);
                columnSettings.addString("columnName", obj.m_fileNameColumn.getStringChoice());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"generate_file_names"}, {"filename_pattern"}, {"filename_column"}};
        }
    }
}
