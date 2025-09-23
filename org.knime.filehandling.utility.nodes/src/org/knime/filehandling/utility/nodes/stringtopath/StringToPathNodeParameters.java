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

package org.knime.filehandling.utility.nodes.stringtopath;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.port.FileSystemPortObject;
import org.knime.filehandling.utility.nodes.stringtopath.StringToPathNodeParameters.FileSystem.FileSystemChooser.FileSystemCategory;
import org.knime.filehandling.utility.nodes.stringtopath.StringToPathNodeParameters.FileSystem.FileSystemChooser.FileSystemCategoryRef;
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
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for String to Path.
 *
 * @author Kai Franze, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
final class StringToPathNodeParameters implements NodeParameters {

    StringToPathNodeParameters() {
    }

    StringToPathNodeParameters(final NodeParametersInput context) {
        final var inputTableIndex = findIndexOfFirst(DATA_TABLE_PORT_SPEC_CLASS, context.getInPortSpecs());
        context.getInTableSpec(inputTableIndex).ifPresent(inputTableSpec -> {
            ColumnSelectionUtil.getFirstStringColumn(inputTableSpec).ifPresent(column -> {
                m_selectedColumnName = column.getName();
            });
        });
    }

    @Section(title = "File system")
    interface FileSystemSection {
    }

    @Section(title = "Column selection")
    @After(FileSystemSection.class)
    interface ColumnSelectionSection {
    }

    @Section(title = "Output")
    @After(ColumnSelectionSection.class)
    interface OutputSection {
    }

    interface GenerateColumnModeRef extends ParameterReference<GenerateColumnMode> {
    }

    static final class IsAppendMode implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(GenerateColumnModeRef.class).isOneOf(GenerateColumnMode.APPEND_NEW);
        }

    }

    @Layout(FileSystemSection.class)
    @Persist(configKey = "file_system")
    FileSystem m_fileSystem = new FileSystem();

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Column selection",
        description = "Column that will be converted. It has to contain a string with correct Path /foo/bar.txt syntax.")
    @ChoicesProvider(DynamicPortStringColumnsProvider.class)
    @Persist(configKey = "selected_column_name")
    String m_selectedColumnName = "";

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Fail if file/folder does not exist",
        description = "Checks if the files or folders referenced by the created Paths are existing and will abort if one is not.")
    @Persist(configKey = "fail_on_missing_file_folder")
    boolean m_abortOnMissingFile = false;

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Fail on missing values",
        description = "If selected the node will fail if the selected column contains missing values.")
    @Persist(configKey = "fail_on_missing_values")
    boolean m_failOnMissingValues = true;

    @Layout(OutputSection.class)
    @Widget(title = "Output",
        description = "Choose whether to append the new column to the table or replace the selected column with the new Path column.")
    @RadioButtonsWidget
    @ValueReference(GenerateColumnModeRef.class)
    @Persist(configKey = "generated_column_mode")
    GenerateColumnMode m_generatedColumnMode = GenerateColumnMode.APPEND_NEW;

    @Layout(OutputSection.class)
    @Widget(title = "New column name", description = "Name of the appended column.")
    @Effect(predicate = IsAppendMode.class, type = EffectType.SHOW)
    @TextInputWidget
    @Persist(configKey = "appended_column_name")
    String m_appendedColumnName = "Path";

    private static final Class<? extends PortObjectSpec> DATA_TABLE_PORT_SPEC_CLASS =
        BufferedDataTable.TYPE.getPortObjectSpecClass();

    private static final Class<? extends PortObjectSpec> FILE_SYSTEM_PORT_SPEC_CLASS =
        FileSystemPortObject.TYPE.getPortObjectSpecClass();

    @SuppressWarnings("serial")
    private static final class PortNotFoundException extends RuntimeException {

        PortNotFoundException(final Class<? extends PortObjectSpec> clazz) {
            super("Could not find port of type <" + clazz.getSimpleName() + ">."
                + " Please check your connected input ports.");
        }

    }

    private static <T extends PortObjectSpec> int findIndexOfFirst(final Class<T> clazz,
        final PortObjectSpec[] inSpecs) {
        for (int i = 0; i < inSpecs.length; i++) {
            if (clazz.isInstance(inSpecs[i])) {
                return i;
            }
        }
        throw new PortNotFoundException(clazz);
    }

    private static boolean hasFileSystemPort(final PortObjectSpec[] inSpecs) {
        try {
            findIndexOfFirst(FILE_SYSTEM_PORT_SPEC_CLASS, inSpecs);
            return true;
        } catch (PortNotFoundException e) {
            return false;
        }
    }

    enum GenerateColumnMode {
            @Label(value = "Append column",
                description = "Append the new column to the table with the selected column name.")
            APPEND_NEW,

            @Label(value = "Replace selected column",
                description = "Replace the selected column with the new Path column.")
            REPLACE_SELECTED
    }

    static final class DynamicPortStringColumnsProvider extends StringColumnsProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            final var inputTableIndex = findIndexOfFirst(DATA_TABLE_PORT_SPEC_CLASS, context.getInPortSpecs());
            return context.getInTableSpec(inputTableIndex) //
                .map(spec -> spec.stream().filter(this::isIncluded)) //
                .orElseGet(Stream::empty) //
                .toList();
        }

    }

    static final class FileSystem implements NodeParameters {

        @Persist(configKey = "file_system_chooser__Internals")
        FileSystemChooser m_fileSystemChooser = new FileSystemChooser();

        @Persist(configKey = "location_spec")
        LocationSpecification m_locationSpecification = new LocationSpecification();

        static final class FileSystemChooser implements NodeParameters {

            // Notes:
            // * We don't get the 'PortsConfig' as input, so we cannot just do
            //   'StringToPathNodeModel.createSettingsModelFileSystem(portsConfig)' here.
            // * In 'StringToPathNodeDialog' we use
            //   'org.knime.filehandling.core.defaultnodesettings.filesystemchooser.DialogComponentFileSystem'
            //   which uses 'org.knime.filehandling.core.defaultnodesettings.filesystemchooser.dialog.FileSystemChooser'
            //   to display the dialog, validate, load and save the settings.
            // * The question is: Do we want to re-produce this whole logic here, or can we trim it down to a minimum?

            @Persist(configKey = "overwritten_by_variable")
            boolean overwrittenByVariable = false;

            @Persist(configKey = "has_fs_port")
            @ValueReference(HasFileSystemPortRef.class)
            @ValueProvider(HasFileSystemPortProvider.class)
            boolean m_hasFileSystemPort;

            @Widget(title = "File system category", description = "Select the file system to which the created paths should be related to. "
                + "There are four default file system options to choose from: Local File System, Mountpoint, Relative to, and Custom/KNIME URL. "
                + "It is possible to use other file systems with this node by enabling the file system connection input port.")
            @Persist(configKey = "convenience_fs_category")
            @ChoicesProvider(FileSystemCategoryChoicesProvider.class)
            @ValueProvider(FileSystemCategoryProvider.class)
            @ValueReference(FileSystemCategoryRef.class)
            FileSystemCategory m_fileSystemCategory;

            @Widget(title = "Mountpoint", description = "Select the mountpoint to which the created paths should be related to.")
            @Effect(predicate = IsFileSystemCategoryMountpoint.class, type = EffectType.SHOW)
            @Persist(configKey = "mountpoint")
            @ChoicesProvider(MountpointChoicesProvider.class)
            String mountpoint;

            @Widget(title = "Relative to", description = "Select the base directory to which the created paths should be relative to.")
            @Effect(predicate = IsFileSystemCategoryRelative.class, type = EffectType.SHOW)
            @Persistor(RelativeToPersistor.class)
            RelativeToOption relativeTo = RelativeToOption.WORKFLOW_DATA;

            @Persist(configKey = "spaceId")
            String spaceId = "";

            @Persist(configKey = "spaceName")
            String spaceName = "";

            @Persist(configKey = "custom_url_timeout")
            int customUrlTimeout = 10000;

            @Persist(configKey = "connected_fs")
            boolean connectedToFileSystemPort = false;

            enum FileSystemCategory {
                    @Label(value = "Local file system", description = "Use the local file system")
                    LOCAL,

                    @Label(value = "Mountpoint", description = "Use a KNIME mountpoint")
                    MOUNTPOINT,

                    @Label(value = "Relative to", description = "Use a path relative to a given base directory")
                    RELATIVE,

                    @Label(value = "Custom/KNIME URL", description = "Use a custom file system or KNIME URL")
                    CUSTOM_URL,

                    @Label(value = "Hub Space", description = "Use a Hub Space")
                    HUB_SPACE,

                    @Label(value = "File System Connection",
                        description = "Use the file system connection from the input port")
                    CONNECTED
            }

            static final class HasFileSystemPortRef implements BooleanReference {
            }

            static final class HasFileSystemPortProvider implements StateProvider<Boolean> {

                @Override
                public void init(final StateProviderInitializer initializer) {
                    // Needed to compute the state when the dialog is opened
                    initializer.computeBeforeOpenDialog();
                }

                @Override
                public Boolean computeState(final NodeParametersInput context) {
                    return hasFileSystemPort(context.getInPortSpecs());
                }

            }

            static final class FileSystemCategoryRef implements ParameterReference<FileSystemCategory> {
            }

            static final class FileSystemCategoryChoicesProvider implements EnumChoicesProvider<FileSystemCategory> {

                @Override
                public List<FileSystemCategory> choices(final NodeParametersInput context) {
                    return hasFileSystemPort(context.getInPortSpecs()) //
                        ? List.of(FileSystemCategory.CONNECTED) //
                        : List.of( //
                            FileSystemCategory.LOCAL, //
                            FileSystemCategory.MOUNTPOINT, //
                            FileSystemCategory.RELATIVE, //
                            FileSystemCategory.CUSTOM_URL, //
                            FileSystemCategory.HUB_SPACE);
                }
            }

            static final class FileSystemCategoryProvider implements StateProvider<FileSystemCategory> {

                private Supplier<Boolean> m_hasFileSystemPortSupplier;

                private Supplier<FileSystemCategory> m_fileSystemCategorySupplier;

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeOnValueChange(HasFileSystemPortRef.class);
                    m_hasFileSystemPortSupplier = initializer.getValueSupplier(HasFileSystemPortRef.class);
                    m_fileSystemCategorySupplier = initializer.getValueSupplier(FileSystemCategoryRef.class);
                }

                @Override
                public FileSystemCategory computeState(final NodeParametersInput context) {
                    final var hasFileSystemPort = m_hasFileSystemPortSupplier.get().booleanValue();
                    final var selectedCategory = m_fileSystemCategorySupplier.get();

                    if (hasFileSystemPort) {
                        return FileSystemCategory.CONNECTED;
                    }

                    return selectedCategory == FileSystemCategory.CONNECTED //
                        ? FileSystemCategory.LOCAL //
                        : selectedCategory;
                }

            }

        }

        static final class IsFileSystemCategoryMountpoint implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(FileSystemCategoryRef.class).isOneOf(FileSystemCategory.MOUNTPOINT);
            }

        }

        static final class IsFileSystemCategoryRelative implements EffectPredicateProvider {

            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(FileSystemCategoryRef.class).isOneOf(FileSystemCategory.RELATIVE);
            }

        }

        static final class MountpointChoicesProvider implements StringChoicesProvider {

            @Override
            public List<String> choices(final NodeParametersInput context) {
                // TODO: Creating these is more elaborate, see {@link FileSystemChooser}.
                return List.of("knime-temp-space", "LOCAL", "EXAMPLES", "My-KNIME-Hub");
            }

        }

        enum RelativeToOption {
                @Label(value = "Current Hub Space", description = "...")
                HUB_SPACE("knime.space"),

                @Label(value = "Current mountpoint", description = "...")
                MOUNTPOINT("knime.mountpoint"),

                @Label(value = "Current workflow", description = "...")
                WORKFLOW("knime.workflow"),

                @Label(value = "Current workflow data area", description = "...")
                WORKFLOW_DATA("knime.workflow.data");

            private final String m_value;

            RelativeToOption(final String value) {
                m_value = value;
            }

            String getValue() {
                return m_value;
            }

            static RelativeToOption fromValue(final String value) {
                for (RelativeToOption option : values()) {
                    if (option.getValue().equals(value)) {
                        return option;
                    }
                }
                return null;
            }
        }

        static final class RelativeToPersistor implements NodeParametersPersistor<RelativeToOption> {

            private static final String CFG_KEY = "relative_to";

            @Override
            public RelativeToOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var relativeToString = settings.getString(CFG_KEY, RelativeToOption.WORKFLOW_DATA.getValue());
                return RelativeToOption.fromValue(relativeToString);
            }

            @Override
            public void save(final RelativeToOption param, final NodeSettingsWO settings) {
                settings.addString(CFG_KEY, param.getValue());
            }

            @Override
            public String[][] getConfigPaths() {
                return null;
            }

        }

        static final class LocationSpecification implements NodeParameters {

            @Persist(configKey = "location_present")
            boolean m_locationPresent = true;

            @Persist(configKey = "file_system_type")
            String m_fileSystem_type = "LOCAL";

        }

    }

}
