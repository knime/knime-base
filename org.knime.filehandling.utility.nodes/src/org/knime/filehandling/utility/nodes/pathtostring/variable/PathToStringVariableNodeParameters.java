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

package org.knime.filehandling.utility.nodes.pathtostring.variable;

import java.util.List;

import org.knime.core.node.workflow.FlowVariable;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.persistence.legacy.LegacyNameFilterPersistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.FlowVariableChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.StringFilter;

/**
 * Node parameters for Path to String (Variable).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class PathToStringVariableNodeParameters implements NodeParameters {

    @Section(title = "Variable Selection")
    interface VariableSelectionSection {
    }

    @Section(title = "Output")
    interface OutputSection {
    }

    @Widget(title = "Variable selection", description = """
            The Path variables that will be converted to String variables.
            """)
    @ChoicesProvider(FSLocationVariablesProvider.class)
    @Persistor(FlowVariableFilterPersistor.class)
    StringFilter m_variableFilter = new StringFilter();

    @Widget(title = "Suffix added to the new variables", description = """
            The suffix that will be added to the name of the Path variables that are converted to create the
            names of the new String variables.
            """)
    @Persist(configKey = PathToStringVariableNodeModel.CFG_CREATED_VARIABLE_NAME)
    String m_variableSuffix = "_location";

    @Widget(title = "If flow variable exists",
            description = "Choose a resolution if the output flow variable has the same name as an existing one.")
    @ValueSwitchWidget
    @Persistor(FlowVariableConflictPersistor.class)
    FlowVariableConflict m_flowVariableConflictHandling = FlowVariableConflict.OVERWRITE;

    enum FlowVariableConflict {
        @Label(value = "Overwrite", description = "The new flow variable overwrites the existing one.")
        OVERWRITE,
        @Label(value = "Append suffix",
                description = "A suffix (like \"(#1)\") will be appended to make the name unique.")
        APPEND_SUFFIX;
    }

    static final class FlowVariableConflictPersistor extends EnumBooleanPersistor<FlowVariableConflict> {

        protected FlowVariableConflictPersistor() {
            super(PathToStringVariableNodeModel.CFG_ON_FV_CONFLICT_MAKE_UNIQUE, FlowVariableConflict.class,
                FlowVariableConflict.APPEND_SUFFIX);
        }

    }

    @Widget(title = "Create KNIME URL for 'Relative to' and 'Mountpoint' file systems", description = """
            This option is only relevant for paths with the Relative to workflow data area,
            Relative to workflow, Relative to mountpoint or Mountpoint file system.
            If checked, a String is created that contains a KNIME URL. Such a KNIME URL starts
            with "knime://" and can be used to e.g. control legacy reader nodes via flow variables. If unchecked,
            a String is created that contains solely the path, i.e. without the knime protocol and hostname. Such a
            String can e.g. be used for manipulations and converted back to a Path using the String to
            Path node.
            """)
    @Persist(configKey = PathToStringVariableNodeModel.CFG_CREATE_KNIME_URL)
    boolean m_createKNIMEUrl = true;

    static final class FSLocationVariablesProvider implements FlowVariableChoicesProvider {

        @Override
        public List<FlowVariable> flowVariableChoices(final NodeParametersInput context) {
            return context.getAvailableInputFlowVariables(FSLocationVariableType.INSTANCE).values().stream().toList();
        }

    }

    static final class FlowVariableFilterPersistor extends LegacyNameFilterPersistor {

        protected FlowVariableFilterPersistor() {
            super(PathToStringVariableNodeModel.CFG_VARIABLE_FILTER);
        }

    }
}
