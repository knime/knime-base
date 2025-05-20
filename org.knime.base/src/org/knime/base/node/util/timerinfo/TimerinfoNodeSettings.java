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
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node settings for the 'Timer Info' node.
 *
 * @author Magnus Gohm, KNIME AG, Konstanz, Germany
 * @since 5.4
 */
@SuppressWarnings("restriction")
public class TimerinfoNodeSettings implements DefaultNodeSettings {

    interface RecursionPolicyRef extends Reference<RecursionPolicy> {
    }

    static final class MaxDepthDisabledPolicy implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(RecursionPolicyRef.class).isOneOf(RecursionPolicy.NO_RECURSION);
        }
    }

    static final class IncludeComponentIONodesPolicy implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(RecursionPolicyRef.class).isOneOf(RecursionPolicy.NO_RECURSION,
                RecursionPolicy.ONLY_METANODES);
        }
    }

    @Widget(title = "Recursion", description = """
            Chooses a recursion option for metanodes and components <br/>
            <ul>
                <li><b>No recursion</b> : Only lists the nodes on the top level of the workflow.</li>
                <li><b>Only metanodes</b> : Recurses only the metanodes up to the specified depth.</li>
                <li><b>Components and metanodes</b> : Recurses components and metanodes up to the specified depth.</li>
            </ul>
            """)
    @ValueReference(RecursionPolicyRef.class)
    @ValueSwitchWidget
    @Migration(RecursionPolicyLegacyBehavior.class)
    // new instances of the node handle components depending on the recursion policy
    RecursionPolicy m_recursionPolicy = RecursionPolicy.NO_RECURSION;

    private static final class RecursionPolicyLegacyBehavior implements DefaultProvider<RecursionPolicy> {
        @Override
        public RecursionPolicy getDefault() {
            // use old behavior for existing nodes -> only metanode recursion
            return RecursionPolicy.ONLY_METANODES;
        }
    }

    @Widget(title = "Max Depth", description = """
            Controls depth of reporting of nodes in (nested) metanodes and components.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Effect(predicate = MaxDepthDisabledPolicy.class, type = EffectType.DISABLE)
    @Persist(configKey = "MaxDepth")
    int m_maxDepth = 2;

    @Widget(title = "Include component input and output nodes", description = """
            Includes the component input and output nodes in the output table.
            """)
    @Effect(predicate = IncludeComponentIONodesPolicy.class, type = EffectType.DISABLE)
    @Migrate(loadDefaultIfAbsent = true)
    // default is `true` to make it obvious that there are special nodes where time might be spent
    // this default value does not matter for old settings, since the components were opaque there
    // and no input/output nodes are encountered
    boolean m_includeComponentIO = true;

    @Widget(title = "Include node comments", description = "Include node comments for each node in the output table")
    @Migration(FalseProvider.class)
    boolean m_includeNodeComments = true;

    private static class FalseProvider implements DefaultProvider<Boolean> {
        @Override
        public Boolean getDefault() {
            return false;
        }
    }

    //Setting enums-----------------------------------------------------------------------------------------------------

    enum RecursionPolicy {

            /**
             * No recursion policy.
             */
            @Label(value = "No recursion")
            NO_RECURSION,

            /**
             * Only metanodes policy.
             */
            @Label(value = "Only metanodes")
            ONLY_METANODES,

            /**
             * Components and metanodes policy.
             */
            @Label(value = "Components and metanodes")
            COMPONENTS_AND_METANODES;

    }

}
