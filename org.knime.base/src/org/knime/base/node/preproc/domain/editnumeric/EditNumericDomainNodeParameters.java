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

package org.knime.base.node.preproc.domain.editnumeric;

import java.util.function.Supplier;

import org.knime.base.node.preproc.domain.editnumeric.EditNumericDomainNodeModel.DomainOverflowPolicy;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.FilteredInputTableColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;

/**
 * Node parameters for Edit Numeric Domain.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class EditNumericDomainNodeParameters implements NodeParameters {

    @Section(title = "Domain Settings")
    interface DomainSettingsSection {
    }

    @Widget(title = "Column selection", description = "Select the numeric columns for which to edit the domain.")
    @ChoicesProvider(NumericColumnsProvider.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_selectedColumns = new ColumnFilter();

    @Layout(DomainSettingsSection.class)
    @Widget(title = "Lower bound", description = "The lower bound of the domain.")
    @NumberInputWidget(maxValidationProvider = LowerBoundMaxValidation.class)
    @Persist(configKey = EditNumericDomainConfiguration.MIN_KEY)
    double m_lowerBound = EditNumericDomainConfiguration.DEFAULT_MIN;

    @Layout(DomainSettingsSection.class)
    @Widget(title = "Upper bound", description = "The upper bound of the domain.")
    @Persist(configKey = EditNumericDomainConfiguration.MAX_KEY)
    @ValueReference(UpperBoundRef.class)
    double m_upperBound = EditNumericDomainConfiguration.DEFAULT_MAX;

    @Layout(DomainSettingsSection.class)
    @Widget(title = "Out of domain policy",
        description = "Defines the behavior if the actual content of a selected column"
            + " does not fit in the given bounds.")
    @RadioButtonsWidget
    @Persist(configKey = EditNumericDomainConfiguration.DOMAIN_OVERFLOW_POLICY_KEY)
    DomainOverflowPolicy m_domainOverflowPolicy = EditNumericDomainConfiguration.DEFAULT_DOMAIN_OVERFLOW_POLICY;

    private static final class LowerBoundMaxValidation implements StateProvider<MaxValidation> {

        Supplier<Double> m_upperBoundSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_upperBoundSupplier = initializer.computeFromValueSupplier(UpperBoundRef.class);
        }

        @Override
        public MaxValidation computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var upperBound = m_upperBoundSupplier.get();
            if (upperBound == null) {
                return null;
            }

            return new MaxValidation() {

                @Override
                protected double getMax() {
                    return upperBound;
                }

                @Override
                public String getErrorMessage() {
                    return "Lower bound must be less than or equal to upper bound.";
                }

            };

        }

    }

    private static final class UpperBoundRef implements ParameterReference<Double> {
    }

    private static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {

        ColumnFilterPersistor() {
            super(EditNumericDomainConfiguration.DATA_COLUMN_FILTER_SPEC_KEY);
        }

    }

    private static final class NumericColumnsProvider implements FilteredInputTableColumnsProvider {

        @Override
        public boolean isIncluded(final DataColumnSpec col) {
            return LongCell.TYPE.equals(col.getType()) || IntCell.TYPE.equals(col.getType())
                || DoubleCell.TYPE.equals(col.getType());
        }

    }

}
