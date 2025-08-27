/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.base.node.preproc.domain.dialog2;

import java.util.Optional;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Settings for the Domain Calculator node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // LegacyColumnFilterPersistor is not API
final class DomainNodeParameters implements NodeParameters {

    // ====== Layout

    @Section(title = "Possible values")
    interface PossibleValuesSection {
    }

    @Section(title = "Minimum & maximum values")
    @After(PossibleValuesSection.class)
    interface MinMaxSection {
    }

    // ====== Possible Values Settings

    static final class NominalColumnsProvider extends CompatibleColumnsProvider {
        protected NominalColumnsProvider() {
            super(NominalValue.class);
        }
    }

    static final class PossibleValuesColumnsPersistor extends LegacyColumnFilterPersistor {
        PossibleValuesColumnsPersistor() {
            super(DomainNodeModel.CFG_POSSVAL_COLS);
        }
    }

    @Widget(title = "Columns", description = """
            Include the columns, for which possible values shall be determined and
            be put in the table specification. The default dialog option selects
            all columns, which can represent themselves as String. For all
            non-selected columns (the Exclude list) the possible value domain will
            be dropped or retained, depending on the selection of the corresponding
            buttons below the Exclude list.
            """)
    @ColumnFilterWidget(choicesProvider = NominalColumnsProvider.class)
    @TwinlistWidget(includedLabel = "Calculate possible values", excludedLabel = "Do not calculate")
    @Persistor(PossibleValuesColumnsPersistor.class)
    @Layout(PossibleValuesSection.class)
    ColumnFilter m_possibleValuesColumns = new ColumnFilter();

    static final class PossibleValuesUnselectedHandlingPersistor
        extends EnumSettingsModelBooleanPersistor<UnselectedDomainHandling> {
        PossibleValuesUnselectedHandlingPersistor() {
            super(DomainNodeModel.CFG_POSSVAL_RETAIN_UNSELECTED, UnselectedDomainHandling.class,
                UnselectedDomainHandling.RETAIN);
        }
    }

    @Widget(title = "Excluded columns", description = """
            Choose what to do with the possible value domain for columns in the exclude list.
            You can either retain the existing domain information from the input table or drop it completely.
            """)
    @ValueSwitchWidget
    @Layout(PossibleValuesSection.class)
    @Persistor(PossibleValuesUnselectedHandlingPersistor.class)
    UnselectedDomainHandling m_possibleValuesUnselectedHandling = UnselectedDomainHandling.RETAIN;

    static final class MaxPossibleValuesEnabledRef implements ParameterReference<Boolean> {
    }

    static final class MaxPossibleValuesEnabledPredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer init) {
            return init.getBoolean(MaxPossibleValuesEnabledRef.class).isTrue();
        }
    }

    // Note on backwards compatibility <5.8:
    // The Swing version did not serialize the state of the checkbox to enable/disable "max possible values".
    // Instead it checked it if the maximum value was non-negative.
    // In case the checkbox was not checked, the max value was overwritten to -1.
    static final class MaxPossibleValuesPersistor implements NodeParametersPersistor<Optional<Integer>> {

        @Override
        public void save(final Optional<Integer> val, final NodeSettingsWO settings) {
            // old dialog used -1 as sentinel value for "unlimited"
            settings.addInt(DomainNodeModel.CFG_MAX_POSS_VALUES, val.orElse(-1));
        }

        @Override
        public Optional<Integer> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var maxValue = settings.getInt(DomainNodeModel.CFG_MAX_POSS_VALUES); // expected to be always present
            if (maxValue < 0) {
                // dialog used -1 as sentinel value for "unlimited"
                return Optional.empty();
            }
            return Optional.of(maxValue);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DomainNodeModel.CFG_MAX_POSS_VALUES}};
        }
    }

    static final class MaxPossibleValuesDefault implements DefaultValueProvider<Integer> {
        @Override
        public Integer computeState(final NodeParametersInput parametersInput) {
            return DataContainerSettings.MAX_POSSIBLE_VALUES;
        }
    }

    @Widget(title = "Maximum number of possible values", description = """
            Restrict the number of different values that will be determined for each column.
            If a column has more possible values than this limit, all values will be discarded and the column's
            metadata won't support querying possible values. Leave unchecked for unlimited values.
                """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(PossibleValuesSection.class)
    @Persistor(MaxPossibleValuesPersistor.class)
    @OptionalWidget(defaultProvider = MaxPossibleValuesDefault.class)
    Optional<Integer> m_maxPossibleValues = Optional.of(DataContainerSettings.MAX_POSSIBLE_VALUES);

    // ======== Min/Max Values Settings

    static final class BoundedColumnsProvider extends CompatibleColumnsProvider {
        protected BoundedColumnsProvider() {
            super(BoundedValue.class);
        }
    }

    static final class MinMaxColumnsPersistor extends LegacyColumnFilterPersistor {
        MinMaxColumnsPersistor() {
            super(DomainNodeModel.CFG_MIN_MAX_COLS);
        }
    }

    @Widget(title = "Columns", description = """
                Select all columns for which min and max values shall be determined. The
                default dialog option includes all &quot;boundable&quot; columns, for
                instance numeric ones.
            """)
    @ColumnFilterWidget(choicesProvider = BoundedColumnsProvider.class)
    @TwinlistWidget(includedLabel = "Calculate min/max values", excludedLabel = "Do not calculate")
    @Persistor(MinMaxColumnsPersistor.class)
    @Layout(MinMaxSection.class)
    ColumnFilter m_minMaxColumns = new ColumnFilter();

    static final class MinMaxUnselectedHandlingPersistor
        extends EnumSettingsModelBooleanPersistor<UnselectedDomainHandling> {
        MinMaxUnselectedHandlingPersistor() {
            super(DomainNodeModel.CFG_MIN_MAX_RETAIN_UNSELECTED, UnselectedDomainHandling.class,
                UnselectedDomainHandling.RETAIN);
        }
    }

    @Widget(title = "Excluded columns", description = """
                Choose what to do with the min/max domain information for columns in the exclude list.
                You can either retain the existing min/max domain from the input table or drop it completely.
            """)
    @ValueSwitchWidget
    @Layout(MinMaxSection.class)
    @Persistor(MinMaxUnselectedHandlingPersistor.class)
    UnselectedDomainHandling m_minMaxUnselectedHandling = UnselectedDomainHandling.RETAIN;

    // ====== Helpers

    // this node parameter was originally a boolean, but we want to show a nice value switch with labels
    enum UnselectedDomainHandling {
            @Label("Retain domain") //
            RETAIN, //
            @Label("Drop domain") //
            DROP
    }

}
