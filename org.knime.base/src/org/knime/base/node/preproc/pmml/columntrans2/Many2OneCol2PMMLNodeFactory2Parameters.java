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

package org.knime.base.node.preproc.pmml.columntrans2;

import org.knime.base.node.preproc.pmml.columntrans2.Many2OneCol2PMMLNodeModel.IncludeMethod;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * Node parameters for Many to One (PMML).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class Many2OneCol2PMMLNodeFactory2Parameters implements NodeParameters {

    @Widget(title = "Columns", description = """
            The columns to be condensed into a single column using the selected include method.
            """)
    @ColumnFilterWidget(choicesProvider = AllColumnsProvider.class)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_selectedColumns = new ColumnFilter();

    @Widget(title = "Condensed column name", description = """
            The name of the appended column.
            """)
    @Persist(configKey = Many2OneCol2PMMLNodeModel.CONDENSED_COL_NAME)
    String m_appendedColumnName = "Condensed Column";

    @Widget(title = "Include method", description = """
            The method to condense multiple columns into one.
            """)
    @ValueReference(IncludeMethodRef.class)
    @Persist(configKey = Many2OneCol2PMMLNodeModel.INCLUDE_METHOD)
    IncludeMethod m_includeMethod = IncludeMethod.Binary;

    @Widget(title = "Include pattern", description = """
            When include method is RegExpPattern, this is the pattern used.
            """)
    @Persist(configKey = Many2OneCol2PMMLNodeModel.RECOGNICTION_REGEX)
    @Effect(predicate = ShowPatternPredicate.class, type = EffectType.SHOW)
    String m_pattern = "[^0]*";

    @Widget(title = "Keep original columns", description = """
            Determines whether the included columns are kept or not.
            """)
    @Persist(configKey = Many2OneCol2PMMLNodeModel.KEEP_COLS)
    boolean m_keepColumns = true;

    static final class IncludeMethodRef implements ParameterReference<IncludeMethod> {
    }

    static final class ShowPatternPredicate implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(IncludeMethodRef.class).isOneOf(IncludeMethod.RegExpPattern);
        }

    }

    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {

        ColumnFilterPersistor() {
            super(Many2OneCol2PMMLNodeModel.SELECTED_COLS);
        }

    }

}
