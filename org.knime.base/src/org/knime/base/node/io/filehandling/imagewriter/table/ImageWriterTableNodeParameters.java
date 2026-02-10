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

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
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
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.AutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;

/**
 * Node parameters for Image Writer (Table).
 *
 * @author Jochen Reißinger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ImageWriterTableNodeParameters implements NodeParameters {

    @Section(title = "Output Location")
    interface OutputLocation {
    }

    @Section(title = "Image Column")
    @After(OutputLocation.class)
    interface ImageColumn {
    }

    @Section(title = "File Names")
    @After(ImageColumn.class)
    interface FileNames {
    }

    @Layout(OutputLocation.class)
    @Persist(configKey = "output_location")
    @Modification(FileChooserModifier.class)
    LegacyFileWriterWithOverwritePolicyOptions m_fileChooser = new LegacyFileWriterWithOverwritePolicyOptions();

    private static final class FileChooserModifier implements LegacyFileWriterWithOverwritePolicyOptions.Modifier {
        @Override
        public void modify(final WidgetGroupModifier group) {
            final var fileSelection = findFileSelection(group);
            fileSelection.modifyAnnotation(Widget.class).withProperty("title", "Folder")
                .withProperty("description",
                    "Select a folder where the image files will be stored. You can specify the file system and path.")
                .modify();
            fileSelection.addAnnotation(FileSelectionWidget.class).withProperty("value", SingleFileSelectionMode.FOLDER)
                .modify();
            fileSelection.addAnnotation(WithFileSystem.class).withProperty("value", new FileSystemOption[]{
                FileSystemOption.LOCAL, FileSystemOption.SPACE, FileSystemOption.EMBEDDED, FileSystemOption.CONNECTED})
                .modify();

            final var createMissingFolders = findCreateMissingFolders(group);
            createMissingFolders.modifyAnnotation(Widget.class)
                .withProperty("description",
                    "If enabled, missing folders in the output path will be created automatically. "
                        + "If disabled, the node will fail if any folder in the path does not exist.")
                .modify();

            restrictOverwritePolicyOptions(group, ImageWriterOverwritePolicyChoicesProvider.class);
        }
    }

    private static final class ImageWriterOverwritePolicyChoicesProvider
        extends LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicyChoicesProvider {
        @Override
        protected List<OverwritePolicy> getChoices() {
            return List.of(OverwritePolicy.fail, OverwritePolicy.overwrite, OverwritePolicy.ignore);
        }
    }

    @Layout(ImageColumn.class)
    @Widget(title = "Image column", description = "Select the column containing the images to write to files.")
    @ChoicesProvider(ImageColumnChoicesProvider.class)
    @Persist(configKey = "image_column")
    String m_imageColumn = "";

    private static final class ImageColumnChoicesProvider implements ColumnChoicesProvider {
        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return getCompatibleColumns(context, ImageValue.class);
        }
    }

    @Layout(ImageColumn.class)
    @Widget(title = "Remove image column",
        description = "If enabled, the image column will be excluded from the output table.")
    @Persist(configKey = "remove_image_column")
    boolean m_removeImageColumn;

    @Layout(FileNames.class)
    @Widget(title = "Generate file names",
        description = "If enabled, file names will be generated using a pattern with an auto-incrementing "
            + "counter. If disabled, file names will be taken from a column in the input table.")
    @ValueReference(GenerateFileNamesRef.class)
    @Persist(configKey = "generate_file_names")
    boolean m_generateFileNames = true;

    private interface GenerateFileNamesRef extends ParameterReference<Boolean> {
    }

    @Layout(FileNames.class)
    @Widget(title = "File name pattern",
        description = "The file names will be generated using the provided pattern. The pattern must contain a "
            + "single \"?\" symbol. This symbol will, during execution, be replaced by an incrementing counter to "
            + "make the file names unique. The file extension will be detected automatically and must not be "
            + "specified.")
    @Effect(predicate = IsGenerateFileNames.class, type = Effect.EffectType.SHOW)
    @Persist(configKey = "filename_pattern")
    String m_fileNamePattern = "File_?";

    private static final class IsGenerateFileNames implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final EffectPredicateProvider.PredicateInitializer i) {
            return i.getBoolean(GenerateFileNamesRef.class).isTrue();
        }
    }

    @Layout(FileNames.class)
    @Widget(title = "File name column",
        description = "Select the column containing the file names for the output images, or select RowID to use "
            + "row IDs as file names. The file extension will be determined automatically and should not be "
            + "included in the column values.")
    @ChoicesProvider(FileNameColumnChoicesProvider.class)
    @ValueReference(FileNameColumnRef.class)
    @ValueProvider(FileNameColumnValueProvider.class)
    @Effect(predicate = IsGenerateFileNames.class, type = Effect.EffectType.HIDE)
    @Migrate
    @Persistor(FileNameColumnPersistor.class)
    StringOrEnum<RowIDChoice> m_fileNameColumn = new StringOrEnum<>(RowIDChoice.ROW_ID);

    private static final class FileNameColumnPersistor implements NodeParametersPersistor<StringOrEnum<RowIDChoice>> {

        private static final String CFG_FILENAME_COLUMN = "filename_column";

        private static final String CFG_USE_ROW_ID = "useRowID";

        private static final String CFG_COLUMN_NAME = "columnName";

        @Override
        public StringOrEnum<RowIDChoice> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(CFG_FILENAME_COLUMN)) {
                final var columnSettings = settings.getNodeSettings(CFG_FILENAME_COLUMN);
                final boolean useRowID = columnSettings.getBoolean(CFG_USE_ROW_ID, false);
                if (useRowID) {
                    return new StringOrEnum<>(RowIDChoice.ROW_ID);
                } else {
                    final String columnName = columnSettings.getString(CFG_COLUMN_NAME, "");
                    return new StringOrEnum<>(columnName);
                }
            }
            return new StringOrEnum<>(RowIDChoice.ROW_ID);
        }

        @Override
        public void save(final StringOrEnum<RowIDChoice> value, final NodeSettingsWO settings) {
            final var columnSettings = settings.addNodeSettings(CFG_FILENAME_COLUMN);
            if (value.getEnumChoice().isPresent()) {
                columnSettings.addBoolean(CFG_USE_ROW_ID, true);
                columnSettings.addString(CFG_COLUMN_NAME, "");
            } else {
                columnSettings.addBoolean(CFG_USE_ROW_ID, false);
                columnSettings.addString(CFG_COLUMN_NAME, value.getStringChoice());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_FILENAME_COLUMN}};
        }

    }

    private interface FileNameColumnRef extends ParameterReference<StringOrEnum<RowIDChoice>> {
    }

    private static final class FileNameColumnValueProvider extends AutoGuessValueProvider<StringOrEnum<RowIDChoice>> {

        protected FileNameColumnValueProvider() {
            super(FileNameColumnRef.class);
        }

        @Override
        protected boolean isEmpty(final StringOrEnum<RowIDChoice> value) {
            return value.getEnumChoice().isEmpty() && value.getStringChoice() == null;
        }

        @Override
        protected StringOrEnum<RowIDChoice> autoGuessValue(final NodeParametersInput parametersInput) {
            var tableSpecs = parametersInput.getInPortSpecs();
            return ColumnSelectionUtil
                .getFirstCompatibleColumn(parametersInput, tableSpecs.length - 1, StringValue.class)
                .map(colSpec -> new StringOrEnum<RowIDChoice>(colSpec.getName()))
                .orElse(new StringOrEnum<>(RowIDChoice.ROW_ID));
        }
    }

    private static final class FileNameColumnChoicesProvider implements ColumnChoicesProvider {
        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return getCompatibleColumns(context, StringValue.class);
        }
    }

    private static List<DataColumnSpec> getCompatibleColumns(final NodeParametersInput context,
        final Class<? extends DataValue> valueType) {
        return Arrays.stream(context.getInPortSpecs()).filter(DataTableSpec.class::isInstance).findFirst()
            .map(spec -> ((DataTableSpec)spec).stream().filter(col -> col.getType().isCompatible(valueType)).toList())
            .orElse(List.of());
    }
}
