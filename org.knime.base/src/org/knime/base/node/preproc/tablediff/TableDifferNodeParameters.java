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
 */
package org.knime.base.node.preproc.tablediff;

import org.knime.core.data.DataTableSpec;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;

/**
 * Node parameters for Table Difference Finder.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v0.0
 */
@LoadDefaultsForAbsentFields
final class TableDifferNodeParameters implements NodeParameters {

    interface DialogLayout {
        @Section(title = "Column Selection")
        interface ColumnSelection {
        }

        @Section(title = "Failure Handling")
        @After(ColumnSelection.class)
        interface FailureHandling {
        }
    }

    /**
     * Enum for the failure mode selection.
     */
    enum FailureMode {
            @Label(value = "Never", description = "The node will not fail on any differences between the two tables.")
            NEVER,

            @Label(value = "Different table specs",
                description = "The node will fail if any of the selected columns does not exist in "
                    + "the other table, or the columns differ in type or domain. "
                    + "<i>Note</i> that differing column positions are being ignored.")
            DIFFERENT_SPECS,

            @Label(value = "Different values",
                description = "On the first occurrence of differences in the values the node will fail.")
            DIFFERENT_VALUES
    }

    static final class CompareEntirelyRef implements BooleanReference {
    }

    static final class ColumnFilterEnabled implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(CompareEntirelyRef.class).negate();
        }
    }

    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterPersistor() {
            super("column_filter");
        }
    }

    @Widget(title = "Compare entire tables", description = """
            If this option is checked all columns from the first as well as the second input will be compared
            against each other. Otherwise, the tables will solely be compared respective the selected columns
            of the reference, i.e., the second input table.
            <b>Note</b>: RowIDs are not being compared.
            """)
    @Persist(configKey = "compare_entirely")
    @ValueReference(CompareEntirelyRef.class)
    @Layout(DialogLayout.ColumnSelection.class)
    boolean m_compareTablesEntirely = true;

    @Widget(title = "Select columns from reference table", description = """
            The list contains the names of those columns in the reference table to be included for the
            comparison. It allows you to select the columns manually (by moving them to the right panel),
            via wildcard/regex (all columns whose names match the wildcard/regex are included) or via type
            selection (all columns with a certain type are included).
            """)
    @Persistor(ColumnFilterPersistor.class)
    @ChoicesProvider(ReferenceColumnsProvider.class)
    @Effect(type = EffectType.ENABLE, predicate = ColumnFilterEnabled.class)
    @Layout(DialogLayout.ColumnSelection.class)
    ColumnFilter m_comparedColumns;

    static final class ReferenceColumnsProvider extends AllColumnsProvider {

        @Override
        public int getInputTableIndex() {
            return TableDifferNodeModel.PORT_REFERENCE_TABLE;
        }

    }

    @Widget(title = "Fail option", description = """
            Select what to do when differences are found.
            """)
    @RadioButtonsWidget
    @Persist(configKey = "failure_mode")
    @Layout(DialogLayout.FailureHandling.class)
    FailureMode m_failureMode = FailureMode.NEVER;

    /**
     * Constructor for use by the framework.
     */
    TableDifferNodeParameters() {
        this((DataTableSpec)null);
    }

    /**
     * Constructor for use with node parameters input.
     *
     * @param context the node parameters input
     */
    TableDifferNodeParameters(final NodeParametersInput context) {
        this(context.getInTableSpec(TableDifferNodeModel.PORT_REFERENCE_TABLE).orElse(null));
    }

    /**
     * Constructor that initializes the column filter based on the reference table spec.
     *
     * @param spec the reference table spec
     */
    TableDifferNodeParameters(final DataTableSpec spec) {
        m_comparedColumns = new ColumnFilter(spec == null ? new String[0] : spec.getColumnNames());
    }
}
