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

package org.knime.base.node.preproc.colcompare;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.preproc.colcompare.ColumnComparatorNodeDialogPane.ComparatorMethod;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.TypedStringChoice;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Column Comparator.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class ColumnComparatorNodeParameters implements NodeParameters {

    @Section(title = "Column and Operator")
    interface ColumnAndOperatorSection {
    }

    @Section(title = "Replacement Method")
    @After(ColumnAndOperatorSection.class)
    interface ReplacementMethodSection {
    }

    @Section(title = "Output Column")
    @After(ReplacementMethodSection.class)
    interface OutputColumnSection {
    }

    @Persist(configKey = ColumnComparatorNodeDialogPane.CFGKEY_FIRST_COLUMN)
    @Widget(title = "Left column", description = "Left column selected for comparison.")
    @ChoicesProvider(AllColumnsProvider.class)
    @Layout(ColumnAndOperatorSection.class)
    @ValueReference(FirstColumnRef.class)
    @ValueProvider(FirstColumnProvider.class)
    String m_firstColumn;

    interface FirstColumnRef extends ParameterReference<String> {
    }

    @Persistor(ComparatorOperatorPersistor.class)
    @Widget(title = "Operator", description = """
            Comparison is done based on the selected operator option.
            """)
    @Layout(ColumnAndOperatorSection.class)
    ComparatorMethod m_operator = ComparatorMethod.EQUAL;

    @Persist(configKey = ColumnComparatorNodeDialogPane.CFGKEY_SECOND_COLUMN)
    @Widget(title = "Right column", description = "Right column selected for comparison.")
    @ChoicesProvider(SecondColumnChoicesProvider.class)
    @Layout(ColumnAndOperatorSection.class)
    @ValueReference(SecondColumnRef.class)
    @ValueProvider(SecondColumnProvider.class)
    String m_secondColumn;

    interface SecondColumnRef extends ParameterReference<String> {
    }

    @Persist(configKey = ColumnComparatorNodeDialogPane.CFGKEY_MATCH_OPTION)
    @Layout(ReplacementMethodSection.class)
    @Widget(title = "Operator result 'true'", description = """
            Depending on the result of the selected operator, a certain value is set into the new column.
            Select one of the following options for the operator result 'true'.
            """)
    @ValueReference(MatchOptionRef.class)
    ReplacementOption m_matchOption = ReplacementOption.LEFT_VALUE;

    interface MatchOptionRef extends ParameterReference<ReplacementOption> {
    }

    @Persist(configKey = ColumnComparatorNodeDialogPane.CFGKEY_MATCH_VALUE)
    @Layout(ReplacementMethodSection.class)
    @Widget(title = "Tag", description = "The text to set in the result column when the operator result is 'true'.")
    @Effect(predicate = IsMatchUserDefined.class, type = EffectType.SHOW)
    String m_matchValue = "TRUE";

    @Persist(configKey = ColumnComparatorNodeDialogPane.CFGKEY_MISMATCH_OPTION)
    @Layout(ReplacementMethodSection.class)
    @Widget(title = "Operator result 'false'", description = """
            Depending on the result of the selected operator, a certain value is set into the new column.
            Select one of the following options for the operator result 'false'.
            """)
    @ChoicesProvider(MismatchOptionChoicesProvider.class)
    @ValueProvider(MismatchOptionProvider.class)
    @ValueReference(MismatchOptionRef.class)
    ReplacementOption m_mismatchOption = ReplacementOption.RIGHT_VALUE;

    interface MismatchOptionRef extends ParameterReference<ReplacementOption> {
    }

    @Persist(configKey = ColumnComparatorNodeDialogPane.CFGKEY_MISMATCH_VALUE)
    @Layout(ReplacementMethodSection.class)
    @Widget(title = "Tag", description = "The text to set in the result column when the operator result is 'false'.")
    @Effect(predicate = IsMismatchUserDefined.class, type = EffectType.SHOW)
    String m_mismatchValue = "FALSE";

    @Persist(configKey = ColumnComparatorNodeDialogPane.CFGKEY_NEW_COLUMN_NAME)
    @Widget(title = "Output column name", description = "Name of the output column.")
    @TextInputWidget(placeholder = "compare_result")
    @Layout(OutputColumnSection.class)
    String m_newColumn = "compare_result";

    static final class FirstColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected FirstColumnProvider() {
            super(FirstColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var inputColumns = ColumnSelectionUtil.getAllColumns(parametersInput, 0);
            return inputColumns.isEmpty() ? Optional.empty() : Optional.of(inputColumns.get(inputColumns.size() - 1));
        }

    }

    private static final class SecondColumnChoicesProvider implements ColumnChoicesProvider {

        private Supplier<String> m_firstColumn;

        @Override
        public void init(final StateProviderInitializer initializer) {
            ColumnChoicesProvider.super.init(initializer);
            m_firstColumn = initializer.computeFromValueSupplier(FirstColumnRef.class);
        }

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            final var firstColumn = m_firstColumn.get();
            return ColumnSelectionUtil.getAllColumnsOfFirstPort(context) //
                .stream() //
                .filter(colSpec -> !colSpec.getName().equals(firstColumn)) //
                .toList();
        }

    }

    static final class SecondColumnProvider implements StateProvider<String> {

        private Supplier<List<TypedStringChoice>> m_secondColumnChoices;

        private Supplier<String> m_secondColumn;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_secondColumnChoices = initializer.computeFromProvidedState(SecondColumnChoicesProvider.class);
            m_secondColumn = initializer.getValueSupplier(SecondColumnRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            final var secondColumnChoices = m_secondColumnChoices.get().stream().map(sc -> sc.id()).toList();
            final var secondColumn = m_secondColumn.get();
            if (secondColumnChoices.isEmpty() || secondColumn != null && secondColumnChoices.contains(secondColumn)) {
                throw new StateComputationFailureException();
            }
            return secondColumnChoices.get(secondColumnChoices.size() - 1);
        }

    }

    static final class IsMatchUserDefined implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(MatchOptionRef.class).isOneOf(ReplacementOption.USER_DEFINED);
        }

    }

    static final class IsMismatchUserDefined implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(MismatchOptionRef.class).isOneOf(ReplacementOption.USER_DEFINED);
        }

    }

    static final class MismatchOptionProvider implements StateProvider<ReplacementOption> {

        private Supplier<List<EnumChoice<ReplacementOption>>> m_misMatchOptionChoicesSupplier;

        private Supplier<ReplacementOption> m_misMatchOptionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_misMatchOptionSupplier = initializer.getValueSupplier(MismatchOptionRef.class);
            m_misMatchOptionChoicesSupplier = initializer.computeFromProvidedState(MismatchOptionChoicesProvider.class);
        }

        @Override
        public ReplacementOption computeState(final NodeParametersInput context)
            throws StateComputationFailureException {
            final var misMatchOption = m_misMatchOptionSupplier.get();
            final var misMatchOptions = m_misMatchOptionChoicesSupplier.get().stream().map(EnumChoice::id).toList();
            if (misMatchOptions.contains(misMatchOption)) {
                throw new StateComputationFailureException();
            }
            return misMatchOptions.get(0);
        }

    }

    static final class MismatchOptionChoicesProvider implements EnumChoicesProvider<ReplacementOption> {

        private Supplier<ReplacementOption> m_matchOptionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            EnumChoicesProvider.super.init(initializer);
            m_matchOptionSupplier = initializer.computeFromValueSupplier(MatchOptionRef.class);
        }

        @Override
        public List<ReplacementOption> choices(final NodeParametersInput context) {
            final var matchOption = m_matchOptionSupplier.get();
            return matchOption == null || matchOption.equals(ReplacementOption.USER_DEFINED)
                ? List.of(ReplacementOption.values())
                : List.of(ReplacementOption.values()).stream().filter(option -> option != matchOption).toList();
        }

    }

    static final class ComparatorOperatorPersistor implements NodeParametersPersistor<ComparatorMethod> {

        @Override
        public ComparatorMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ComparatorMethod.getMethod(settings
                .getString(ColumnComparatorNodeDialogPane.CFGKEY_COMPARATOR_METHOD, ComparatorMethod.EQUAL.toString()));
        }

        @Override
        public void save(final ComparatorMethod obj, final NodeSettingsWO settings) {
            settings.addString(ColumnComparatorNodeDialogPane.CFGKEY_COMPARATOR_METHOD, obj.toString());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{ColumnComparatorNodeDialogPane.CFGKEY_COMPARATOR_METHOD}};
        }

    }

    enum ReplacementOption {
            @Label(value = "Left value", description = "The value of the left column is set in the result column.")
            LEFT_VALUE, //
            @Label(value = "Right value", description = "The value of the right column is set in the result column.")
            RIGHT_VALUE, //
            @Label(value = "Missing", description = "A missing cell is set in the result column.")
            MISSING, //
            @Label(value = "User-defined",
                description = "The text entered in the 'Tag' field is set in the result column.")
            USER_DEFINED;
    }

}
