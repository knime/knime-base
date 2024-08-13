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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2024 (magnus): created
 */
package org.knime.base.node.util.timerinfo;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Node settings for the 'Timer Info' node.
 *
 * @author Magnus Gohm, KNIME AG, Konstanz, Germany
 * @since 5.4
 */
@SuppressWarnings("restriction")
public class TimerinfoNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Max Depth", description = """
            Controls depth of reporting of nodes in (nested) metanodes and components. A depth of &quot;0&quot; means
            no descent is performed.
            """)
    @NumberInputWidget(min = 0, max = Integer.MAX_VALUE)
    @Persist(configKey = "MaxDepth")
    int m_maxDepth = 2;

    static class ComponentsOnlyPolicy extends OneOfEnumCondition<ComponentResolutionPolicy> {
        @Override
        public ComponentResolutionPolicy[] oneOf() {
            return new ComponentResolutionPolicy[]{ComponentResolutionPolicy.COMPONENTS};
        }
    }

    @Widget(title = "Component resolution", description = """
            Chooses how components are resolved regarding the timer information. <br/>
            <ul>
                <li><b>Components</b> : Lists the whole component without the nested nodes</li>
                <li><b>Nested nodes</b> : Lists only the nested component nodes without the component itself</li>
                <li><b>Components and nested nodes</b> : Lists both</li>
            </ul>
            """)
    @Signal(condition=ComponentsOnlyPolicy.class)
    @ValueSwitchWidget
    @Persist(optional = true, defaultProvider = LegacyBehavior.class)
    // new instances of the node handle components similar to metanodes (but omitting component in/out "special" nodes)
    ComponentResolutionPolicy m_componentResolution = ComponentResolutionPolicy.NODES;

    private static final class LegacyBehavior implements DefaultProvider<ComponentResolutionPolicy> {
        @Override
        public ComponentResolutionPolicy getDefault() {
            // use old behavior for existing nodes -> opaque components
            return ComponentResolutionPolicy.COMPONENTS;
        }
    }

    @Widget(title = "Include component input and output nodes", advanced = true, description = """
            Includes the component input and output nodes in the output table.
            """)
    @Effect(signals = ComponentsOnlyPolicy.class, type = EffectType.DISABLE)
    @Persist(optional = true)
    // default is `true` to make it obvious that there are special nodes where time might be spent
    // this default value does not matter for old settings, since the components were opaque there
    // and no input/output nodes are encountered
    boolean m_includeComponentIO = true;

}
