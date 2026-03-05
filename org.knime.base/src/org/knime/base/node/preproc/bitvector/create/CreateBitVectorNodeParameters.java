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

package org.knime.base.node.preproc.bitvector.create;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.knime.base.node.preproc.bitvector.create.CreateBitVectorNodeModel.ColumnType;
import org.knime.base.node.preproc.bitvector.create.CreateBitVectorNodeModel.SetMatching;
import org.knime.base.node.preproc.bitvector.create.CreateBitVectorNodeModel.StringType;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
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
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.internal.StateProviderInitializerInternal;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * Node parameters for Create Bit Vector.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class CreateBitVectorNodeParameters implements NodeParameters {

    @Section(title = "Multiple Columns")
    @Effect(predicate = IsMultiColumn.class, type = EffectType.SHOW)
    interface MultiColumnSelectionSection {
    }

    @Section(title = "Single Column")
    @After(MultiColumnSelectionSection.class)
    @Effect(predicate = IsSingleColumn.class, type = EffectType.SHOW)
    interface SingleColumnSection {
    }

    @Section(title = "Output")
    @After(SingleColumnSection.class)
    interface OutputSection {
    }

    @Widget(title = "Input column(s)", description = "Select the input column(s) for creating bit vectors.")
    @Persist(configKey = CreateBitVectorNodeModel.CFG_COLUMN_TYPE)
    @ValueReference(ColumnTypeRef.class)
    ColumnType m_columnType = ColumnType.SINGLE_STRING;

    interface ColumnTypeRef extends ParameterReference<ColumnType> {
    }

    @Widget(title = "Pattern",
        description = "The pattern to search for in the data value of each selected string column.")
    @Layout(MultiColumnSelectionSection.class)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Persist(configKey = CreateBitVectorNodeModel.CFG_MSC_PATTERN)
    @Effect(predicate = IsMultiString.class, type = EffectType.SHOW)
    @ValueReference(PatternRef.class)
    String m_mscPattern;

    static final class PatternRef implements ParameterReference<String> {
    }

    @TextMessage(PatternMatchingValidationInfoProvider.class)
    @Layout(MultiColumnSelectionSection.class)
    Void m_patternValidationInfo;

    @Widget(title = "Case sensitive match",
        description = "A case sensitive matching is performed if this option is selected.")
    @Layout(MultiColumnSelectionSection.class)
    @Persist(configKey = CreateBitVectorNodeModel.CFG_MSC_CASE_SENSITIVE)
    @Effect(predicate = IsMultiString.class, type = EffectType.SHOW)
    boolean m_mscCaseSensitive;

    @Widget(title = "Pattern matching", description = """
            Select the pattern matching method.
            """)
    @Layout(MultiColumnSelectionSection.class)
    @ValueSwitchWidget
    @Persistor(PatternMatchingMethodPersistor.class)
    @Effect(predicate = IsMultiString.class, type = EffectType.SHOW)
    @ValueReference(PatternMatchingMethodRef.class)
    PatternMatchingMethod m_patternMatchingMethod = PatternMatchingMethod.MANUAL;

    static final class PatternMatchingMethodRef implements ParameterReference<PatternMatchingMethod> {
    }

    @Widget(title = "Set bit if pattern",
        description = """
                Sets the bit if the pattern:
                """)
    @Layout(MultiColumnSelectionSection.class)
    @ValueSwitchWidget
    @Persist(configKey = CreateBitVectorNodeModel.CFG_MSC_SET_MATCHING)
    @Effect(predicate = IsMultiString.class, type = EffectType.SHOW)
    SetMatching m_mscSetMatching = SetMatching.MATCHING;

    @Widget(title = "Threshold",
        description = """
                Choose how to define the threshold for setting bits when using multiple numeric columns.
                """)
    @Layout(MultiColumnSelectionSection.class)
    @ValueSwitchWidget
    @Persistor(ThresholdModePersistor.class)
    @ValueReference(ThresholdModeRef.class)
    @Effect(predicate = IsMultiNumeric.class, type = EffectType.SHOW)
    ThresholdMode m_thresholdMode = ThresholdMode.GLOBAL;

    static final class ThresholdModeRef implements ParameterReference<ThresholdMode> {
    }

    static final class UseMean implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ThresholdModeRef.class).isOneOf(ThresholdMode.MEAN)
                    .and(i.getPredicate(IsMultiNumeric.class));
        }

    }

    @Widget(title = "Global threshold",
        description = """
                Specify the global threshold. All values which are above or equal to this threshold
                will result in a 1 in the bit vector.
                """)
    @Layout(MultiColumnSelectionSection.class)
    @Persist(configKey = CreateBitVectorNodeModel.CFG_THRESHOLD)
    @NumberInputWidget(stepSize = 0.1)
    @Effect(predicate = UseThreshold.class, type = EffectType.SHOW)
    double m_threshold = 1.0;

    @Widget(title = "Percentage of the mean",
        description = """
                Specify which percentage of the mean a value should have in order to be set. For example,
                with a mean percentage of 50% and a column mean of 2, the bit is set if the value is
                above or equal to 1.
                """)
    @Layout(MultiColumnSelectionSection.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = CreateBitVectorNodeModel.CFG_MEAN_PERCENTAGE)
    @Effect(predicate = UseMean.class, type = EffectType.SHOW)
    int m_meanPercentage = 100;

    @Widget(title = "Column selection",
        description = """
                Select the columns to include for bit vector creation. When creating bit vectors from
                multiple string columns, include string columns. When creating from multiple numeric
                columns, include numeric columns. The position of a column in the selection corresponds
                to the bit position in the resulting bit vector.
                """)
    @Layout(MultiColumnSelectionSection.class)
    @Persistor(MultiColumnFilterPersistor.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @ChoicesProvider(StringOrNumericColumnsProvider.class)
    ColumnFilter m_multiColumns = new ColumnFilter();

    @Widget(title = "String representation format", description = """
            Select one of the three valid input formats for the single string column:
            """)
    @Layout(SingleColumnSection.class)
    @ValueSwitchWidget
    @Persist(configKey = CreateBitVectorNodeModel.CFG_SINGLE_STRING_TYPE)
    @Effect(predicate = IsSingleString.class, type = EffectType.SHOW)
    StringType m_singleStringColumnType = StringType.BIT;

    @Widget(title = "Column",
        description = """
                The column to parse for creating bit vectors. Select a string column when using a
                single string column source, or a collection column when using a single collection
                column source.
                """)
    @Persist(configKey = CreateBitVectorNodeModel.CFG_SINGLE_COLUMN)
    @Layout(SingleColumnSection.class)
    @ChoicesProvider(StringOrCollectionColumnsProvider.class)
    @ValueProvider(SingleColumnProvider.class)
    @ValueReference(SingleColumnRef.class)
    String m_singleColumn;

    static final class SingleColumnRef implements ParameterReference<String> {
    }

    @Widget(title = "Remove column(s) used for bit vector creation",
        description = """
                If checked, the generating column(s) (the included columns for multi-column modes,
                or the selected single column) are removed from the output table. If unchecked, the
                generated bit vectors are appended to the input table.
                """)
    @Layout(OutputSection.class)
    @Persist(configKey = CreateBitVectorNodeModel.CFG_REMOVE_COLUMNS)
    boolean m_removeColumns;

    @Widget(title = "Output column",
        description = "The name of the output column containing the generated bit vectors.")
    @Layout(OutputSection.class)
    @Persist(configKey = CreateBitVectorNodeModel.CFG_OUTPUT_COLUMN)
    @TextInputWidget(patternValidation = ColumnNameValidation.class)
    String m_outputColumn = "BitVector";

    @Widget(title = "Fail on invalid input",
        description = """
                If selected, the node will fail during execution if a data cell could not be converted
                to a bit set. If unselected, the node will skip invalid entries and insert a missing
                value instead.
                """)
    @Layout(OutputSection.class)
    @Persist(configKey = CreateBitVectorNodeModel.CFG_FAIL_ON_ERROR)
    boolean m_failOnError;

    @Widget(title = "Bit vector type", description = "The storage type for the generated bit vectors.")
    @Layout(OutputSection.class)
    @ValueSwitchWidget
    @Persist(configKey = CreateBitVectorNodeModel.CFG_VECTOR_TYPE)
    BitVectorTypeOption m_vectorType = BitVectorTypeOption.DENSE;

    static final class IsMultiColumn implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(IsMultiString.class).or(i.getPredicate(IsMultiNumeric.class));
        }

    }

    static final class IsSingleColumn implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ColumnTypeRef.class).isOneOf(ColumnType.SINGLE_STRING, ColumnType.SINGLE_COLLECTION);
        }

    }

    static final class IsMultiString implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ColumnTypeRef.class).isOneOf(ColumnType.MULTI_STRING);
        }

    }

    static final class IsMultiNumeric implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ColumnTypeRef.class).isOneOf(ColumnType.MULTI_NUMERICAL);
        }

    }

    static final class UseThreshold implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return not(i.getPredicate(UseMean.class)).and(i.getPredicate(IsMultiNumeric.class));
        }

    }

    static final class IsSingleString implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ColumnTypeRef.class).isOneOf(ColumnType.SINGLE_STRING);
        }

    }

    static final class PatternMatchingValidationInfoProvider implements StateProvider<Optional<TextMessage.Message>> {

        Supplier<ColumnType> m_columnTypeSupplier;

        Supplier<PatternMatchingMethod> m_patternMatchingMethodSupplier;

        Supplier<String> m_patternSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_columnTypeSupplier = initializer.computeFromValueSupplier(ColumnTypeRef.class);
            m_patternMatchingMethodSupplier = initializer.computeFromValueSupplier(PatternMatchingMethodRef.class);
            m_patternSupplier = initializer.computeFromValueSupplier(PatternRef.class);
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            if (m_columnTypeSupplier.get() != ColumnType.MULTI_STRING) {
                return Optional.empty();
            }

            String pattern = m_patternSupplier.get();
            try {
                final var patternMatchingMethod = m_patternMatchingMethodSupplier.get();
                if (patternMatchingMethod == PatternMatchingMethod.WILDCARDS) {
                    pattern = WildcardMatcher.wildcardToRegex(pattern);
                    Pattern.compile(pattern);
                } else if (patternMatchingMethod == PatternMatchingMethod.REGEX) {
                    Pattern.compile(pattern);
                }
                return Optional.empty();
            } catch (PatternSyntaxException pse) { // NOSONAR
                return setErrMsg("Error in pattern. ('" + pse.getMessage() + "')");
            }
        }

        private static Optional<Message> setErrMsg(final String msg) {
            return Optional.of(new TextMessage.Message("Invalid pattern", msg, TextMessage.MessageType.WARNING));
        }

    }

    static final class StringOrNumericColumnsProvider extends ReferenceColumnsChoicesProvider<ColumnType> {

        StringOrNumericColumnsProvider() {
            super(ColumnTypeRef.class, columnType -> switch (columnType) {
                case MULTI_STRING -> List.of(StringValue.class);
                case MULTI_NUMERICAL -> List.of(DoubleValue.class);
                default -> List.of(DataValue.class);
            });
        }

    }

    static final class StringOrCollectionColumnsProvider extends ReferenceColumnsChoicesProvider<ColumnType> {

        StringOrCollectionColumnsProvider() {
            super(ColumnTypeRef.class, columnType -> switch (columnType) {
                case SINGLE_STRING -> List.of(StringValue.class);
                case SINGLE_COLLECTION -> List.of(CollectionDataValue.class);
                default -> List.of(DataValue.class);
            });
        }

    }

    abstract static class ReferenceColumnsChoicesProvider<R> implements ColumnChoicesProvider {

        private final Class<? extends ParameterReference<R>> m_referenceClass;
        private final Function<R, Collection<Class<? extends DataValue>>> m_valueClassFunction;

        protected ReferenceColumnsChoicesProvider(final Class<? extends ParameterReference<R>> referenceClass,
            final Function<R, Collection<Class<? extends DataValue>>> valueClassFunction) {
            m_referenceClass = referenceClass;
            m_valueClassFunction = valueClassFunction;
        }

        Supplier<R> m_referenceSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            ColumnChoicesProvider.super.init(initializer);
            m_referenceSupplier = initializer.computeFromValueSupplier(m_referenceClass);
        }

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return context.getInTableSpec(0) //
                .map(spec -> spec.stream()
                    .filter(col -> hasCompatibleType(col, m_valueClassFunction.apply(m_referenceSupplier.get())))) //
                .orElseGet(Stream::empty) //
                .toList();
        }

        private static boolean hasCompatibleType(final DataColumnSpec col,
            final Collection<Class<? extends DataValue>> valueClasses) {
            return valueClasses.stream().anyMatch(valueClass -> col.getType().isCompatible(valueClass));
        }

    }

    static final class SingleColumnProvider implements StateProvider<String> {

        Supplier<ColumnType> m_columnTypeSupplier;

        Supplier<String> m_singleColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            ((StateProviderInitializerInternal)initializer).computeOnParametersLoaded();
            m_columnTypeSupplier = initializer.computeFromValueSupplier(ColumnTypeRef.class);
            m_singleColumnSupplier = initializer.getValueSupplier(SingleColumnRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return CreateBitVectorNodeModel.autoGuessSingleColumn(parametersInput.getInTableSpec(0).orElse(null),
                m_columnTypeSupplier.get(),
                m_singleColumnSupplier.get()).orElseThrow(StateComputationFailureException::new);
        }

    }

    static final class PatternMatchingMethodPersistor implements NodeParametersPersistor<PatternMatchingMethod> {

        @Override
        public PatternMatchingMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var isRegex = settings.getBoolean(CreateBitVectorNodeModel.CFG_MSC_IS_REGEX, false);
            final var hasWildcards = settings.getBoolean(CreateBitVectorNodeModel.CFG_MSC_HAS_WILDCARDS, false);
            if (isRegex && hasWildcards) {
                throw new InvalidSettingsException("Can't use 'Regex' and 'Wildcards'.");
            } else if (isRegex) {
                return PatternMatchingMethod.REGEX;
            } else if (hasWildcards) {
                return PatternMatchingMethod.WILDCARDS;
            } else {
                return PatternMatchingMethod.MANUAL;
            }
        }

        @Override
        public void save(final PatternMatchingMethod param, final NodeSettingsWO settings) {
            settings.addBoolean(CreateBitVectorNodeModel.CFG_MSC_IS_REGEX, param == PatternMatchingMethod.REGEX);
            settings.addBoolean(CreateBitVectorNodeModel.CFG_MSC_HAS_WILDCARDS,
                param == PatternMatchingMethod.WILDCARDS);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][] {{CreateBitVectorNodeModel.CFG_MSC_IS_REGEX},
                {CreateBitVectorNodeModel.CFG_MSC_HAS_WILDCARDS}};
        }

    }

    static final class ThresholdModePersistor extends EnumBooleanPersistor<ThresholdMode> {

        protected ThresholdModePersistor() {
            super(CreateBitVectorNodeModel.CFG_USE_MEAN, ThresholdMode.class, ThresholdMode.MEAN);
        }

    }

    static final class MultiColumnFilterPersistor extends LegacyColumnFilterPersistor {

        MultiColumnFilterPersistor() {
            super(CreateBitVectorNodeModel.CFG_MULTI_COLUMN_NAMES);
        }

    }

    enum PatternMatchingMethod {

        @Label(value = "Manual", description = "Define the pattern manually for simple string matching.")
        MANUAL, //
        @Label(value = "Wildcard", description = """
                Use wild cards in the pattern. Wildcard patterns contain
                '*' (matching any sequence of characters) and '?' (matching any one character).
                """)
        WILDCARDS, //
        @Label(value = "Regex", description = """
                Specify a regular expression. Examples: "^foo.*" matches anything
                that starts with "foo". "[0-9]*" matches any string of digits. For a complete explanation
                of regular expressions, see the java.util.regex.Pattern class documentation.
                """)
        REGEX;

    }

    enum ThresholdMode {
        @Label(value = "Global", description = """
                Specify the global threshold. All values which are above or equal to this threshold
                will result in a 1 in the bit vector.
                """)
        GLOBAL, //
        @Label(value = "Mean", description = """
                Specify a percentage of the mean of each column which is used as threshold.
                """)
        MEAN;
    }

    enum BitVectorTypeOption {

        @Label(value = "Dense", description = """
                Standard option recommended for dense vectors e.g. with more than 10% set bits
                """)
        DENSE, //
        @Label(value = "Sparse", description = "Option recommended for sparse vectors e.g. less than 10% set bits")
        SPARSE;

    }

}
