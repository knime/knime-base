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

package org.knime.filehandling.utility.nodes.stringtopath.variable;

import static org.knime.filehandling.utility.nodes.stringtopath.variable.StringToPathVariableNodeModel.CFG_ABORT_ON_MISSING_FILE;
import static org.knime.filehandling.utility.nodes.stringtopath.variable.StringToPathVariableNodeModel.CFG_FILE_SYSTEM;
import static org.knime.filehandling.utility.nodes.stringtopath.variable.StringToPathVariableNodeModel.CFG_ON_FV_CONFLICT_MAKE_UNIQUE;
import static org.knime.filehandling.utility.nodes.stringtopath.variable.StringToPathVariableNodeModel.CFG_SUFFIX;
import static org.knime.filehandling.utility.nodes.stringtopath.variable.StringToPathVariableNodeModel.CFG_VARIABLE_FILTER;

import java.util.List;

import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.webui.node.dialog.defaultdialog.internal.filesystem.LegacyFileSystemSelection;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyNameFilterPersistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.StringFilter;

/**
 * Node parameters for String to Path (Variable).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class StringToPathVariableNodeParameters implements NodeParameters {

    @Widget(title = "Variable selection",
        description = """
                The String variables that will be converted to Path variables. The variables have to contain \
                Strings with correct Path <tt>/foo/bar.txt</tt> syntax.
                """)
    @ChoicesProvider(StringFlowVariablesProvider.class)
    @Persistor(VariableFilterPersistor.class)
    StringFilter m_variableFilter = new StringFilter();

    static final class VariableFilterPersistor extends LegacyNameFilterPersistor {

        VariableFilterPersistor() {
            super(CFG_VARIABLE_FILTER);
        }

    }

    @Persistor(FileSystemPersistor.class)
    LegacyFileSystemSelection m_fileSystemSelection = new LegacyFileSystemSelection();

    @Widget(title = "Suffix added to the new variables", description = """
            The suffix that will be added to the name of the String variables that are converted to create \
            the names of the new Path variables.
            """)
    @Persist(configKey = CFG_SUFFIX)
    String m_variableSuffix = "_location";

    @Widget(title = "Fail if file/folder does not exist", description = """
            Checks if the files or folders referenced by the created Paths are existing and will abort if one \
            is not.
            """)
    @Persist(configKey = CFG_ABORT_ON_MISSING_FILE)
    boolean m_abortOnMissingFile;

    @Widget(title = "Append suffix if variable already exists", description = """
            If selected, and if an output flow variable has the same name as an existing one, a unique suffix \
            is appended to make the name unique. If not selected, the output flow variable will overwrite the \
            existing flow variable.
            """)
    @Persist(configKey = CFG_ON_FV_CONFLICT_MAKE_UNIQUE)
    @Migration(LoadTrueIfAbsent.class)
    boolean m_onFVConflictMakeUnique;

    static final class LoadTrueIfAbsent implements DefaultProvider<Boolean> {

        @Override
        public Boolean getDefault() {
            return true;
        }

    }

    static final class StringFlowVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            final var stringVariables =
                    context.getAvailableInputFlowVariables(VariableType.StringType.INSTANCE).values().stream().toList();
            return stringVariables.stream()
                    .filter(flowVar -> !flowVar.getName().startsWith(Scope.Global.getPrefix())).toList();
        }

    }

    static final class FileSystemPersistor extends LegacyFileSystemSelection.LegacyFileSystemSelectionPersistor {

        FileSystemPersistor() {
            super(CFG_FILE_SYSTEM);
        }

    }

}
