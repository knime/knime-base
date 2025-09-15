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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.defaultnodesettings.filesystemchooser.SettingsModelFileSystem;
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
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for String to Path.
 *
 * @author Kai Franze, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class StringToPathNodeParameters implements NodeParameters {

    StringToPathNodeParameters() {
        // default constructor
    }

    StringToPathNodeParameters(final NodeParametersInput input) {
        // Auto-guess the first string column if available
        var inputTableSpec = input.getInTableSpec(0);
        if (inputTableSpec.isPresent()) {
            m_selectedColumnName =
                inputTableSpec.get().stream().filter(spec -> spec.getType().isCompatible(StringValue.class)).findFirst()
                    .map(DataColumnSpec::getName).orElse("");
        }
    }

    // ===== SECTION DEFINITIONS =====

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

    // ===== PARAMETER REFERENCES FOR EFFECTS =====

    interface GenerateColumnModeRef extends ParameterReference<GenerateColumnMode> {
    }

    // ===== EFFECT PREDICATES =====

    static final class IsAppendMode implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(GenerateColumnModeRef.class).isOneOf(GenerateColumnMode.APPEND_NEW);
        }
    }

    // ===== CUSTOM PERSISTORS =====

    static final class FileSystemPersistor implements NodeParametersPersistor<SettingsModelFileSystem> {

        @Override
        public void save(final SettingsModelFileSystem obj, final NodeSettingsWO settings) {
            obj.saveSettingsTo(settings);
        }

        @Override
        public SettingsModelFileSystem load(final NodeSettingsRO settings) throws InvalidSettingsException {
            // This will be initialized properly by the node factory
            var model = new SettingsModelFileSystem("file_system", null,
                StringToPathNodeModel.CONNECTION_INPUT_PORT_GRP_NAME, java.util.EnumSet.allOf(FSCategory.class));
            model.loadSettingsFrom(settings);
            return model;
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"file_system"}};
        }
    }

    static final class GenerateColumnModePersistor implements NodeParametersPersistor<GenerateColumnMode> {

        @Override
        public void save(final GenerateColumnMode obj, final NodeSettingsWO settings) {
            settings.addString("generated_column_mode", obj.getActionCommand());
        }

        @Override
        public GenerateColumnMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var modeStr = settings.getString("generated_column_mode", GenerateColumnMode.APPEND_NEW.getActionCommand());
            return GenerateColumnMode.valueOf(modeStr);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"generated_column_mode"}};
        }
    }

    static final class StringColumnsProvider implements ChoicesProvider {
        @Override
        public String[] choices(final NodeParametersInput input) {
            return input.getInTableSpec(0).map(DataTableSpec::stream)
                .map(stream -> stream.filter(spec -> spec.getType().isCompatible(StringValue.class))
                    .map(DataColumnSpec::getName).toArray(String[]::new))
                .orElse(new String[0]);
        }
    }

    // ===== PARAMETERS =====

    @Layout(FileSystemSection.class)
    @Widget(title = "File system",
        description = "Select the file system to which the created paths should be related to. "
            + "There are four default file system options to choose from: Local File System, Mountpoint, Relative to, and Custom/KNIME URL. "
            + "It is possible to use other file systems with this node by enabling the file system connection input port.")
    @Persistor(FileSystemPersistor.class)
    SettingsModelFileSystem m_fileSystem;

    @Layout(ColumnSelectionSection.class)
    @Widget(title = "Column selection",
        description = "Column that will be converted. It has to contain a string with correct Path /foo/bar.txt syntax.")
    @ChoicesProvider(StringColumnsProvider.class)
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
    @Persistor(GenerateColumnModePersistor.class)
    StringToPathNodeModel.GenerateColumnMode m_generatedColumnMode =
        StringToPathNodeModel.GenerateColumnMode.APPEND_NEW;

    @Layout(OutputSection.class)
    @Widget(title = "New column name", description = "Name of the appended column.")
    @Effect(predicate = IsAppendMode.class, type = EffectType.SHOW)
    @TextInputWidget
    @Persist(configKey = "appended_column_name")
    String m_appendedColumnName = "Path";

    // ===== ENUM DEFINITIONS =====

    enum GenerateColumnMode {
            @Label(value = "Append column",
                description = "Append the new column to the table with the selected column name.")
            APPEND_NEW("Append column:"),

            @Label(value = "Replace selected column",
                description = "Replace the selected column with the new Path column.")
            REPLACE_SELECTED("Replace selected column");

        private final String m_text;

        GenerateColumnMode(final String text) {
            this.m_text = text;
        }

        public String getText() {
            return m_text;
        }

        public String getActionCommand() {
            return name();
        }

        public static GenerateColumnMode fromActionCommand(final String actionCommand) {
            return valueOf(actionCommand);
        }
    }
}
