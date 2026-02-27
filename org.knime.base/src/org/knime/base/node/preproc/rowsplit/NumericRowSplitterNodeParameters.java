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

package org.knime.base.node.preproc.rowsplit;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;

/**
 * Node parameters for Numeric Row Splitter.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class NumericRowSplitterNodeParameters implements NodeParameters {

    @Persist(configKey = "column_selection")
    @Widget(title = "Column", description = """
            Select one numeric column whose values will be tested against the specified bounds.
            """)
    @ChoicesProvider(DoubleColumnsProvider.class)
    @ValueReference(ColumnSelectionRef.class)
    String m_columnSelection;

    static final class ColumnSelectionRef implements ParameterReference<String> {
    }

    @Persist(configKey = "lower_bound_defined")
    @Widget(title = "Use lower bound", description = """
            If enabled, only rows whose column value satisfies the lower bound condition are directed to the first
            output port.
            """)
    @ValueReference(IsLowerBoundEnabled.class)
    boolean m_lowerBoundDefined = true;

    static final class IsLowerBoundEnabled implements BooleanReference {
    }

    @Persist(configKey = "lower_bound_value")
    @Widget(title = "Lower bound value", description = """
            The lower bound value. Only rows whose column value satisfies the lower bound condition are directed to
            the first output port.
            """)
    @NumberInputWidget(maxValidationProvider = LowerBoundMaxValueValidationProvider.class)
    @Effect(predicate = IsLowerBoundEnabled.class, type = EffectType.SHOW)
    double m_lowerBoundValue;

    @Persistor(LowerBoundOperatorPersistor.class)
    @Widget(title = "Lower bound operator", description = "Defines how the lower bound is applied to the column value.")
    @Effect(predicate = IsLowerBoundEnabled.class, type = EffectType.SHOW)
    LowerBoundOperator m_lowerBoundOperator = LowerBoundOperator.GREATER_THAN_OR_EQUAL;

    @Persist(configKey = "upper_bound_defined")
    @Widget(title = "Use upper bound", description = """
            If enabled, only rows whose column value satisfies the upper bound condition are directed to the first
            output port.
            """)
    @ValueReference(IsUpperBoundEnabled.class)
    boolean m_upperBoundDefined = true;

    static final class IsUpperBoundEnabled implements BooleanReference {
    }

    @Persist(configKey = "upper_bound_value")
    @Widget(title = "Upper bound value", description = """
            The upper bound value. Only rows whose column value satisfies the upper bound condition are directed to
            the first output port.
            """)
    @Effect(predicate = IsUpperBoundEnabled.class, type = EffectType.SHOW)
    @ValueReference(UpperBoundValueRef.class)
    double m_upperBoundValue;

    static final class UpperBoundValueRef implements ParameterReference<Double> {
    }

    @Persistor(UpperBoundOperatorPersistor.class)
    @Widget(title = "Upper bound operator", description = "Defines how the upper bound is applied to the column value.")
    @Effect(predicate = IsUpperBoundEnabled.class, type = EffectType.SHOW)
    UpperBoundOperator m_upperBoundOperator = UpperBoundOperator.LESS_THAN_OR_EQUAL;

    static final class ColumnSelectionProvider extends ColumnNameAutoGuessValueProvider {

        protected ColumnSelectionProvider() {
            super(ColumnSelectionRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                    ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, DoubleCell.class);
            return compatibleColumns.isEmpty() ? Optional.empty() :
                Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    static final class LowerBoundMaxValueValidationProvider implements StateProvider<MaxValidation> {

        Supplier<Double> m_upperBoundValueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_upperBoundValueSupplier = initializer.computeFromValueSupplier(UpperBoundValueRef.class);
        }

        @Override
        public MaxValidation computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var upperBoundValue = m_upperBoundValueSupplier.get();
            if (upperBoundValue == null) {
                throw new StateComputationFailureException();
            }

            return new MaxValidation() {

                @Override
                protected double getMax() {
                    return upperBoundValue;
                }

            };
        }

    }

    static final class LowerBoundOperatorPersistor implements NodeParametersPersistor<LowerBoundOperator> {

        @Override
        public LowerBoundOperator load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return LowerBoundOperator.fromLegacyValue(
                settings.getString("lower_bound", LowerBoundOperator.GREATER_THAN_OR_EQUAL.m_legacyValue));
        }

        @Override
        public void save(final LowerBoundOperator obj, final NodeSettingsWO settings) {
            settings.addString("lower_bound", obj.m_legacyValue);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"lower_bound"}};
        }

    }

    static final class UpperBoundOperatorPersistor implements NodeParametersPersistor<UpperBoundOperator> {

        @Override
        public UpperBoundOperator load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return UpperBoundOperator.fromLegacyValue(
                settings.getString("upper_bound", UpperBoundOperator.LESS_THAN_OR_EQUAL.m_legacyValue));
        }

        @Override
        public void save(final UpperBoundOperator obj, final NodeSettingsWO settings) {
            settings.addString("upper_bound", obj.m_legacyValue);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"upper_bound"}};
        }

    }

    enum LowerBoundOperator {

            @Label(value = "Greater than or equal", description = """
                    Rows where the column value is greater than or equal to the lower bound are matched.
                    """)
            GREATER_THAN_OR_EQUAL(">="), //
            @Label(value = "Greater than", description = """
                    Rows where the column value is strictly greater than the lower bound are matched.
                    """)
            GREATER_THAN(">");

        final String m_legacyValue;

        LowerBoundOperator(final String legacyValue) {
            m_legacyValue = legacyValue;
        }

        static LowerBoundOperator fromLegacyValue(final String value) throws InvalidSettingsException {
            for (LowerBoundOperator op : values()) {
                if (op.m_legacyValue.equals(value)) {
                    return op;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values = Arrays.stream(LowerBoundOperator.values()).map(op -> op.m_legacyValue)
                .collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

    enum UpperBoundOperator {

            @Label(value = "Less than or equal", description = """
                    Rows where the column value is less than or equal to the upper bound are matched.
                    """)
            LESS_THAN_OR_EQUAL("<="), //
            @Label(value = "Less than", description = """
                    Rows where the column value is strictly less than the upper bound are matched.
                    """)
            LESS_THAN("<");

        final String m_legacyValue;

        UpperBoundOperator(final String legacyValue) {
            m_legacyValue = legacyValue;
        }

        static UpperBoundOperator fromLegacyValue(final String value) throws InvalidSettingsException {
            for (UpperBoundOperator op : values()) {
                if (op.m_legacyValue.equals(value)) {
                    return op;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values = Arrays.stream(UpperBoundOperator.values()).map(op -> op.m_legacyValue)
                .collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

}
