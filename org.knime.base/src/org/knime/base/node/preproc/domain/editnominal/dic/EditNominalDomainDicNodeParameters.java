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

package org.knime.base.node.preproc.domain.editnominal.dic;

import org.knime.core.data.NominalValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node parameters for Edit Nominal Domain (Dictionary).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class EditNominalDomainDicNodeParameters implements NodeParameters {

    @Widget(title = "Select domain value columns", description = """
            Select the columns from the second input table that should be used to add domain values to \
            the columns in the first input table with matching names and types.
            """)
    @ChoicesProvider(ColumnFilterChoicesProvider.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "If domain value columns are not present in data",
        description = "Determines if the execution should either fail or the column should be ignored if an "
            + "included column does not exist in the input table (1st).")
    @ValueSwitchWidget
    @Persistor(MissingColumnHandlingPersistor.class)
    MissingColumnHandling m_missingColumnHandling = MissingColumnHandling.FAIL;

    @Widget(title = "If column types do not match",
        description = "Determines if the execution should either fail or the column should be ignored if the "
            + "types of an included column are not equal.")
    @ValueSwitchWidget
    @Persistor(TypeMismatchHandlingPersistor.class)
    TypeMismatchHandling m_typeMismatchHandling = TypeMismatchHandling.FAIL;

    @Widget(title = "Domain values (2nd input) will be inserted",
        description = "The ordering of the values in the domain might be relevant for downstream nodes, e.g. "
            + "predictor nodes, which append new columns representing the different possible values. This option "
            + "determines the ordering in the output, whereby the additional domain values (2nd input) can be "
            + "either inserted before or after existing domain values (1st input).")
    @Persistor(DomainValuePositionPersistor.class)
    DomainValuePosition m_domainValuePosition = DomainValuePosition.BEFORE;

    @Widget(title = "Maximum amount of possible domain values",
        description = "Sets the upper bound of the number of possible domain values. An extreme high amount of "
            + "possible domain values may influence and even crash the rest of the workflow, since table "
            + "specifications are held in memory. It is highly recommended to use the given default.")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = EditNominalDomainDicConfiguration.MAX_DOMAIN_VALUES)
    int m_maxDomainValues = EditNominalDomainDicConfiguration.DEFAULT_MAX_DOMAIN_VALUES;

    static final class ColumnFilterChoicesProvider extends CompatibleColumnsProvider {

        protected ColumnFilterChoicesProvider() {
            super(NominalValue.class);
        }

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {

        ColumnFilterPersistor() {
            super(EditNominalDomainDicConfiguration.DATA_COLUMN_FILTER_SPEC_KEY);
        }

    }

    static final class MissingColumnHandlingPersistor extends EnumBooleanPersistor<MissingColumnHandling> {

        MissingColumnHandlingPersistor() {
            super(EditNominalDomainDicConfiguration.IGNORE_NOT_PRESENT_COLS, MissingColumnHandling.class,
                MissingColumnHandling.IGNORE);
        }

    }

    static final class TypeMismatchHandlingPersistor extends EnumBooleanPersistor<TypeMismatchHandling> {

        TypeMismatchHandlingPersistor() {
            super(EditNominalDomainDicConfiguration.IGNORE_NOT_MATCHING_TYPES, TypeMismatchHandling.class,
                TypeMismatchHandling.IGNORE);
        }

    }

    static final class DomainValuePositionPersistor extends EnumBooleanPersistor<DomainValuePosition> {

        DomainValuePositionPersistor() {
            super(EditNominalDomainDicConfiguration.NEW_DOMAIN_VALUES_FIRST, DomainValuePosition.class,
                DomainValuePosition.BEFORE);
        }

    }

    enum MissingColumnHandling {

        @Label(value = "Fail")
        FAIL, //
        @Label(value = "Ignore column")
        IGNORE;

    }

    enum TypeMismatchHandling {

        @Label(value = "Fail")
        FAIL, //
        @Label(value = "Ignore column")
        IGNORE;

    }

    enum DomainValuePosition {

        @Label(value = "Before existing domain values (1st input)")
        BEFORE, //
        @Label(value = "After existing domain values (1st input)")
        AFTER;

    }

}
