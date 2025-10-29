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

package org.knime.base.node.io.extractsysprop;

import java.util.List;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;

/**
 * Node parameters for Extract System Properties.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ReadSysPropertyNodeParameters implements NodeParameters {

    @Widget(title = "Extract all available properties",
        description = "If selected all available runtime properties are extracted. "
            + "Note that the properties may vary when run on different systems.")
    @ValueReference(ExtractAllPropsRef.class)
    @Persist(configKey = "isExtractAllProps")
    boolean m_extractAllProps = true;

    @Widget(title = "Fail if property not present in runtime environment",
        description = "If selected and any chosen property is not available in the current runtime environment, "
            + "the node will fail upon execution. If not selected and any property is not present, "
            + "the node will silently ignore this property. This option is important if the workflow is run "
            + "on a different system/machine than it was configured on, e.g. when used in a compute cluster "
            + "environment or on a central server.")
    @Effect(predicate = IsNotExtractAllProperties.class, type = EffectType.ENABLE)
    @Persist(configKey = "failIfSomeMissing")
    boolean m_failIfSomeMissing;

    @Widget(title = "Select properties",
        description = "Choose which system properties to extract. Properties that are not available in the current"
            + " runtime environment will be handled according to the 'Fail if property not present' setting.")
    @ChoicesProvider(SystemPropertiesChoicesProvider.class)
    @TwinlistWidget
    @Effect(predicate = IsNotExtractAllProperties.class, type = EffectType.ENABLE)
    @Persist(configKey = "selectedProps")
    String[] m_selectedProps = new String[0];

    interface ExtractAllPropsRef extends ParameterReference<Boolean> {
    }

    static final class IsNotExtractAllProperties implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(ExtractAllPropsRef.class).isFalse();
        }
    }

    static final class SystemPropertiesChoicesProvider implements StringChoicesProvider {
        @Override
        public List<String> choices(final NodeParametersInput context) {
            return ReadSysPropertyConfiguration.readAllProps().keySet().stream().toList();
        }
    }

}
