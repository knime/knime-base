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
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetModifier;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.meta.FSConnectionConfig.FileSystemOption;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.PredicateInitializer;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
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

    interface OutputLocation {
    }

    @After(OutputLocation.class)
    interface ImageColumn {
    }

    @After(ImageColumn.class)
    interface FileNames {
    }

    @Section(title = "Output location")
    @Persist(configKey = "filechooser")
    @Modification(FileChooserModifier.class)
    LegacyFileWriterWithOverwritePolicyOptions m_outputLocation = new LegacyFileWriterWithOverwritePolicyOptions();

    private static final class FileChooserModifier implements LegacyFileWriterWithOverwritePolicyOptions.Modifier {
        @Override
        public void modify(final WidgetGroupModifier group) {
            final var fileSelection = findFileSelection(group);
            fileSelection.modifyAnnotation(Widget.class) //
                .withProperty("title", "Folder") //
                .withProperty("description",
                    "Select a folder where the image files will be stored. You can specify the file system and path.") //
                .modify();
            fileSelection.addAnnotation(FileSelectionWidget.class) //
                .withProperty("value", SingleFileSelectionMode.FOLDER) //
                .modify();
            fileSelection.modifyAnnotation(FileSelectionWidget.class) //
                .withProperty("fileSystemConnection", FSCategory.CONNECTED) //
                .withProperty("defaultFSLocation",
                    new FileSystemOption[]{FileSystemOption.LOCAL, FileSystemOption.SPACE,
                        FileSystemOption.EMBEDDED, FileSystemOption.CONNECTED}) //
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

    @Section(title = "Image")
    @Widget(title = "Image column",
        description = "Select the column containing the images to write to files.")
    @ChoicesProvider(ImageColumnChoicesProvider.class)
    @Persist(configKey = "image_col")
    String m_imageColumn = "";

    private static final class ImageColumnChoicesProvider extends CompatibleColumnsProvider {
        protected ImageColumnChoicesProvider() {
            super(ImageValue.class);
        }
    }

    @Widget(title = "Remove image column",
        description = "If enabled, the image column will be excluded from the output table. "
            + "Only the file paths of the written images will be included.")
    @Persist(configKey = "remove_image_col")
    boolean m_removeImageColumn = false;

    @Section(title = "File names")
    @Widget(title = "File name source",
        description = "Specify how to name the output image files. You can either generate names using a pattern "
            + "with an auto-incrementing counter, or use file names from a column in the input table.")
    @ValueSwitchWidget
    @Persist(configKey = "use_filename_col")
    @Modification.WidgetReference(FileNameSourceRef.class)
    FileNameSource m_fileNameSource = FileNameSource.GENERATE;

    interface FileNameSourceRef extends ParameterReference<FileNameSource> {
    }

    /**
     * Enum for selecting the file name source.
     */
    enum FileNameSource {
            @Label("Generate")
            GENERATE, //
            @Label("From column")
            FROM_COLUMN;
    }

    @Widget(title = "File name pattern",
        description = "A pattern for generated file names. Must contain exactly one '?' which will be replaced by "
            + "an auto-incrementing counter to ensure unique file names. The file extension will be determined "
            + "automatically based on the image format and should not be included in the pattern. "
            + "Example: 'Image_?' will create files like 'Image_0.png', 'Image_1.png', etc.")
    @Persist(configKey = "filename_pattern")
    @Effect(predicate = IsGenerateFileNames.class, type = EffectType.SHOW)
    String m_fileNamePattern = "File_?";

    static final class IsGenerateFileNames implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileNameSourceRef.class).isOneOf(FileNameSource.GENERATE);
        }
    }

    @Widget(title = "File name column",
        description = "Select the column containing the file names for the output images. "
            + "The file extension will be determined automatically and should not be included in the column values.")
    @ChoicesProvider(FileNameColumnChoicesProvider.class)
    @Persist(configKey = "filename_col")
    @Effect(predicate = IsFromColumnFileNames.class, type = EffectType.SHOW)
    String m_fileNameColumn = "";

    private static final class FileNameColumnChoicesProvider extends CompatibleColumnsProvider {
        protected FileNameColumnChoicesProvider() {
            super();
        }
    }

    static final class IsFromColumnFileNames implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileNameSourceRef.class).isOneOf(FileNameSource.FROM_COLUMN);
        }
    }
}
