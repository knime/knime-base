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

package org.knime.base.node.flowcontrol.trycatch.genericcatch;

import java.util.List;

import org.knime.core.node.NodeSettingsRO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Catch Errors (Data Ports, Var Ports and Generic Ports).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class GenericCatchNodeParameters implements NodeParameters {

    @Section(title = "Error Variables")
    interface ErrorVariablesSection {

    }

    @Section(title = "Scope Variables")
    interface ScopeVariablesSection {

    }

    @Layout(ErrorVariablesSection.class)
    @Widget(title = "Always populate error variables", description = "If selected, the variables will also be "
        + "generated when the node is not failing with the default values as selected.")
    @Persist(configKey = GenericCatchNodeModel.CFG_ALWAYS_POPULATE)
    @ValueReference(AlwaysPopulate.class)
    boolean m_alwaysPopulate;

    @Layout(ErrorVariablesSection.class)
    @Widget(title = "Default value for the 'FailingNode' variable, if no node is failing.", description = "")
    @TextInputWidget
    @Effect(predicate = AlwaysPopulate.class, type = EffectType.ENABLE)
    @Persist(configKey = GenericCatchNodeModel.CFG_FAILING_NAME)
    String m_defaultVariable = "none";

    @Layout(ErrorVariablesSection.class)
    @Widget(title = "Default value for the 'FailingNodeMessage' variable, if no node is failing.", description = "")
    @TextInputWidget
    @Effect(predicate = AlwaysPopulate.class, type = EffectType.ENABLE)
    @Persist(configKey = GenericCatchNodeModel.CFG_FAILING_MESSAGE)
    String m_defaultMessage = "none";

    @Layout(ErrorVariablesSection.class)
    @Widget(title = "Default value for the 'FailingNodeDetails' variable, if no node is failing. "
            + "Since this option was added in 5.4, existing nodes will be initialized with the default for the "
            + "'FailingNodeMessage' value.", description = "")
    @TextInputWidget
    @Effect(predicate = AlwaysPopulate.class, type = EffectType.ENABLE)
    @Persist(configKey = GenericCatchNodeModel.CFG_FAILING_DETAILS)
    @Migration(LoadMessageAsDetails.class)
    String m_defaultDetails = "none";

    @Layout(ErrorVariablesSection.class)
    @Widget(title = "Default value for the 'FailingNodeStackTrace' variable, if no node is failing.", description = "")
    @Effect(predicate = AlwaysPopulate.class, type = EffectType.ENABLE)
    @Persist(configKey = GenericCatchNodeModel.CFG_FAILING_STACKTRACE)
    String m_defaultStackTrace = "none";

    @Layout(ScopeVariablesSection.class)
    @Widget(title = "Propagate variables", description = "If selected, variables defined (or modified) within the "
        + "Try-Catch block are propagated downstream of this Catch Errors node. In most cases users will want to check"
        + " this box (which is also the default). Previous versions of KNIME did not have this option and variables "
        + "were always limited in scope and not visible downstream.")
    @Persist(configKey = GenericCatchNodeModel.CFG_PROPAGATE_VARIABLES)
    @Migration(LoadFalsePropagateVariables.class)
    boolean m_propagateVariables = true;

    static final class AlwaysPopulate implements BooleanReference {

    }

    static final class LoadFalsePropagateVariables implements DefaultProvider<Boolean> {

        @Override
        public Boolean getDefault() {
            return false;
        }

    }

    static final class LoadMessageAsDetails implements NodeParametersMigration<String> {

        /**
         * @return the default during loading
         */
        static String loadMessage(final NodeSettingsRO settings) {
            return settings.getString(GenericCatchNodeModel.CFG_FAILING_MESSAGE, "none");
        }

        @Override
        public List<ConfigMigration<String>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(LoadMessageAsDetails::loadMessage).build());
        }

    }

}
