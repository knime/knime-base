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

import static org.knime.filehandling.utility.nodes.stringtopath.StringToPathNodeModel.CFG_ABORT_ON_MISSING_FILE;
import static org.knime.filehandling.utility.nodes.stringtopath.StringToPathNodeModel.CFG_APPENDED_COLUMN_NAME;
import static org.knime.filehandling.utility.nodes.stringtopath.StringToPathNodeModel.CFG_FAIL_ON_MISSING_VALS;
import static org.knime.filehandling.utility.nodes.stringtopath.StringToPathNodeModel.CFG_GENERATED_COLUMN_MODE;
import static org.knime.filehandling.utility.nodes.stringtopath.StringToPathNodeModel.CFG_SELECTED_COLUMN_NAME;

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.filesystem.LegacyFileSystemSelection;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;

/**
 * Node parameters for String to Path.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class StringToPathNodeParameters implements NodeParameters {

    @Section(title = "Error Handling")
    interface ErrorHandlingSection {
    }

    @Section(title = "Output")
    @After(ErrorHandlingSection.class)
    interface OutputSection {
    }

    @Persistor(FileSystemPersistor.class)
    LegacyFileSystemSelection m_fileSystemSelection = new LegacyFileSystemSelection();

    static final class FileSystemPersistor extends LegacyFileSystemSelection.LegacyFileSystemSelectionPersistor {
        FileSystemPersistor() {
            super(StringToPathNodeModel.CFG_FILE_SYSTEM);
        }
    }

    @Widget(title = "Input column",
        description = "Column that will be converted. It has to contain a string with correct Path <tt>/foo/bar.txt</tt> syntax.")
    @ChoicesProvider(StringColumnsProviderForCorrectPort.class)
    @Persist(configKey = CFG_SELECTED_COLUMN_NAME)
    @ValueProvider(AutoGuessStringColumnProvider.class)
    @ValueReference(ColumnRef.class)
    String m_column;

    interface ColumnRef extends ParameterReference<String> {
    }

    static final class StringColumnsProviderForCorrectPort extends StringColumnsProvider {

        @Override
        public int getInputTableIndex(final NodeParametersInput parametersInput) {
            // Last port
            return parametersInput.getInPortTypes().length - 1;
        }
    }

    static final class AutoGuessStringColumnProvider extends ColumnNameAutoGuessValueProvider {

        AutoGuessStringColumnProvider() {
            super(ColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            int portIndex = parametersInput.getInPortTypes().length - 1;
            return ColumnSelectionUtil.getFirstCompatibleColumn(parametersInput, portIndex, StringValue.class);
        }

    }

    @Widget(title = "Fail if file/folder does not exist",
        description = "Checks if the files or folders referenced by the created Paths are existing and will abort if one is not.")
    @Layout(ErrorHandlingSection.class)
    @Persist(configKey = CFG_ABORT_ON_MISSING_FILE)
    boolean m_failIfNotExisting = false;

    @Widget(title = "Fail on missing values",
        description = "If selected the node will fail if the selected column contains missing values.")
    @Layout(ErrorHandlingSection.class)
    @Persist(configKey = CFG_FAIL_ON_MISSING_VALS)
    boolean m_failOnMissingValues = true;

    @Widget(title = "Output column", description = "Whether to replace an existing column or append a new one.")
    @ValueReference(AppendOrReplace.Ref.class)
    @Layout(OutputSection.class)
    @Persist(configKey = CFG_GENERATED_COLUMN_MODE)
    @ValueSwitchWidget
    AppendOrReplace m_appendOrReplace = AppendOrReplace.APPEND_NEW;

    enum AppendOrReplace {

            @Label(value = "Append", description = "Append a new column")
            APPEND_NEW, //
            @Label(value = "Replace", description = "Replace the input column")
            REPLACE_SELECTED;

        static final class Ref implements ParameterReference<AppendOrReplace> {
        }

        static final class IsAppend implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getEnum(AppendOrReplace.Ref.class).isOneOf(APPEND_NEW);
            }
        }
    }

    @Effect(predicate = AppendOrReplace.IsAppend.class, type = EffectType.SHOW)
    @Widget(title = "New column name", description = "Name of the appended column.")
    @Layout(OutputSection.class)
    @Persist(configKey = CFG_APPENDED_COLUMN_NAME)
    String m_newColumnName = "Path";

}
