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

package org.knime.base.node.preproc.crossjoin;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for Cross Joiner.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class CrossJoinerNodeParameters implements NodeParameters {

    /** Reference for the "Append top data table's RowIDs" toggle. */
    interface ShowFirstRowIdsRef extends ParameterReference<Boolean> {
    }

    /** Reference for the "Append bottom data table's RowIDs" toggle. */
    interface ShowSecondRowIdsRef extends ParameterReference<Boolean> {
    }

    /** Predicate provider: show dependent controls when first RowIDs should be appended. */
    static final class ShowFirstRowIdsIsTrue implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(ShowFirstRowIdsRef.class).isTrue();
        }
    }

    /** Predicate provider: show dependent controls when second RowIDs should be appended. */
    static final class ShowSecondRowIdsIsTrue implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(ShowSecondRowIdsRef.class).isTrue();
        }
    }

    @Widget(title = "Bottom table's column name suffix", description = """
            The suffix attached to a column's name if the bottom table contains a column
            with the same name. The column names of the first inport are always retained.
            If there are duplicates found in the second table the suffix is added once or
            multiple times to ensure uniqueness.
            """)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Persist(configKey = "rigthSuffix")
    public String rightColumnNameSuffix = " (#1)";

    @Widget(title = "Separator for new RowIDs", description = """
            This string will separate the RowIDs in the new data table. E.g. <i>RowID1 + sep + RowID2</i>
            """)
    @TextInputWidget
    @Persist(configKey = "CFG_SEPARATOR")
    public String rowKeySeparator = "_";

    @Widget(title = "Chunk size", description = """
            Number of rows read at once. Increasing this value yields faster execution time
            but also increases memory consumption.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = "CFG_CACHE")
    public int cacheSize = 1;

    @Widget(title = "Append top data table's RowIDs", description = """
            If selected a new column will be attached to the output, containing the RowIDs
            of the top data table.
            """)
    @Persist(configKey = "CFG_SHOW_FIRST")
    @ValueReference(ShowFirstRowIdsRef.class)
    public boolean showFirstRowIds = false;

    @Widget(title = "Column name (top)", description = "Name of the column containing the RowIDs from the top table")
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Effect(predicate = ShowFirstRowIdsIsTrue.class, type = EffectType.SHOW)
    @Persist(configKey = "CFG_FIRST_COLUMNNAME")
    public String firstRowIdsColumnName = "FirstRowIDs";

    @Widget(title = "Append bottom data table's RowIDs", description = """
            If selected a new column will be attached to the output, containing the RowIDs
            of the bottom data table.
            """)
    @Persist(configKey = "CFG_SHOW_SECOND")
    @ValueReference(ShowSecondRowIdsRef.class)
    public boolean showSecondRowIds = false;

    @Widget(title = "Column name (bottom)",
        description = "Name of the column containing the RowIDs from the bottom table")
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Effect(predicate = ShowSecondRowIdsIsTrue.class, type = EffectType.SHOW)
    @Persist(configKey = "CFG_SECOND_COLUMNNAME")
    public String secondRowIdsColumnName = "SecondRowIDs";
}
