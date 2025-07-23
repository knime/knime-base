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
 *   Apr 23, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.sample;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;

/**
 * This class is a base to define common settings of sampling nodes. They can be implemented by nodes, while slight
 * modifications are possible via {@link Modification}.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 * @since 5.5
 */
@SuppressWarnings({"restriction", "javadoc"})
public abstract class AbstractSamplingNodeSettings implements NodeParameters {

    /**
     * Default constructor.
     */
    protected AbstractSamplingNodeSettings() {

    }

    /**
     * Determines a class column from the context if possible.
     *
     * @param context to retrieve the data table spec from
     */
    protected AbstractSamplingNodeSettings(final NodeParametersInput context) {
        final var firstCol = context.getInTableSpec(0) //
            .stream() //
            .flatMap(DataTableSpec::stream).filter(spec -> spec.getType().isCompatible(NominalValue.class)).findFirst();
        if (firstCol.isPresent()) {
            this.m_classColumn = firstCol.get().getName();
        }
    }

    public enum CountMode {

            @Label("Relative (%)")
            RELATIVE, //
            @Label("Absolute")
            ABSOLUTE;

        /**
         * Predicate if {@link CountModeRef} is Relative.
         *
         * @author Martin Sillye, TNG Technology Consulting GmbH
         */
        public static final class IsRelative implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer init) {
                return init.getEnum(CountModeRef.class).isOneOf(CountMode.RELATIVE);
            }
        }

        /**
         * Predicate if {@link CountModeRef} is Absolute.
         *
         * @author Martin Sillye, TNG Technology Consulting GmbH
         */
        public static final class IsAbsolute implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer init) {
                return init.getEnum(CountModeRef.class).isOneOf(CountMode.ABSOLUTE);
            }
        }

        /**
         * Reference for {@link CountMode} field.
         *
         * @author Martin Sillye, TNG Technology Consulting GmbH
         */
        public interface CountModeRef extends ParameterReference<CountMode> {
        }
    }

    @Widget(title = "Output size type", description = """
            Defines how the size of the output is specified: as a percentage of total rows (relative) or \
            as an absolute number of rows.
            """)
    @ValueSwitchWidget
    @ValueReference(CountMode.CountModeRef.class)
    @Migration(PartitioningModeMigration.class)
    @Modification.WidgetReference(ModeWidgetRef.class)
    public CountMode m_partitioningMode = CountMode.RELATIVE;

    @Widget(title = "Relative size", description = """
            Specifies the percentage of rows from the input table to be included in the output. Must be \
            between 0 and 100 (inclusive).
            """)
    @Effect(type = EffectType.SHOW, predicate = CountMode.IsRelative.class)
    @NumberInputWidget(minValidation = MinPercentageValidation.class, maxValidation = MaxPercentageValidation.class)
    @Migration(PercentageMigration.class)
    @Modification.WidgetReference(PercentageWidgetRef.class)
    public double m_percentage = 70;

    static final class MinPercentageValidation extends NumberInputWidgetValidation.MinValidation {

        @Override
        protected double getMin() {
            return 0;
        }
    }

    static final class MaxPercentageValidation extends NumberInputWidgetValidation.MaxValidation {

        @Override
        protected double getMax() {
            return 100;
        }
    }

    @Widget(title = "Number of rows", description = """
            Specifies the absolute number of rows to include in the output. If the input table contains \
            fewer rows than specified, all rows are placed in the output.
            """)
    @Effect(type = EffectType.SHOW, predicate = CountMode.IsAbsolute.class)
    @NumberInputWidget(minValidation = MinRowCountValidation.class)
    @Migration(CountMigration.class)
    @Modification.WidgetReference(CountWidgetRef.class)
    public long m_rowCount = 100;

    static final class MinRowCountValidation extends NumberInputWidgetValidation.MinValidation {

        @Override
        protected double getMin() {
            return 1;
        }
    }

    interface ModeWidgetRef extends Modification.Reference {
    }

    interface PercentageWidgetRef extends Modification.Reference {
    }

    interface CountWidgetRef extends Modification.Reference {
    }

    public enum SamplingMode {
            @Label(value = "Random", description = """
                    Randomly selects rows from the input table. You can optionally specify a random seed for \
                    reproducible results.
                    """)
            RANDOM, //
            @Label(value = "Stratified", description = """
                    Preserves the distribution of values in the selected group column. \
                    You can optionally specify a random seed for reproducible stratified sampling.
                    """)
            STRATIFIED, //
            @Label(value = "Linear", description = """
                    Selects rows evenly spaced across the input table, always including the first and last row. \
                    This method is useful for downsampling sorted columns while preserving boundary values.
                    """)
            LINEAR, //
            @Label(value = "First rows", description = """
                    Allows you to select the top-most rows of the input table.
                    """)
            FIRST_ROWS
    }

    public enum ActionOnEmptyInput {
            @Label("Fail")
            FAIL, //
            @Label("Output empty table(s)")
            OUTPUT_EMPTY
    }

    @Widget(title = "Sampling strategy", description = """
            Determines how rows are selected for the output. Strategies include random, stratified, linear, \
            and first rows (sequential).
            """)
    @ValueSwitchWidget
    @ValueReference(SamplingStrategyRef.class)
    @Migration(SamplingModeMigration.class)
    @Modification.WidgetReference(SamplingStrategyRef.class)
    public SamplingMode m_mode = SamplingMode.RANDOM;

    interface SamplingStrategyRef extends ParameterReference<SamplingMode>, Modification.Reference {
    }

    static final class UseGroupColumn implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SamplingStrategyRef.class).isOneOf(SamplingMode.STRATIFIED);
        }

    }

    static final class UseSeed implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SamplingStrategyRef.class).isOneOf(SamplingMode.STRATIFIED, SamplingMode.RANDOM);
        }

    }

    static final class NominalColumnChoicesProvider extends CompatibleColumnsProvider {

        NominalColumnChoicesProvider() {
            super(NominalValue.class);
        }

    }

    @Widget(title = "Group column", description = """
            Specifies the column whose value distribution should be preserved in stratified sampling. Ensures both \
            selected and non-selected rows reflect the same distribution of values.
            """)
    @Effect(type = EffectType.SHOW, predicate = UseGroupColumn.class)
    @ChoicesProvider(NominalColumnChoicesProvider.class)
    @Persist(configKey = "class_column")
    @Migrate(loadDefaultIfAbsent = true)
    @Modification.WidgetReference(ClassColumnRef.class)
    public String m_classColumn = "";

    interface UseRandomSeedRef extends ParameterReference<Boolean> {

    }

    @Widget(title = "Fixed random seed", description = """
            Optional seed value for random or stratified sampling. Using a seed ensures the same rows are selected \
            each time the node is executed. Without a seed, a different random selection will occur each time.
            """)
    @Migration(SeedMigration.class)
    @OptionalWidget(defaultProvider = SeedDefaultProvider.class)
    @Effect(predicate = UseSeed.class, type = EffectType.SHOW)
    public Optional<Long> m_seed = Optional.of(1678807467440L);

    static final class SeedDefaultProvider implements DefaultValueProvider<Long> {

        @Override
        public Long computeState(final NodeParametersInput context) throws StateComputationFailureException {
            return 1678807467440L;
        }

    }

    @Widget(title = "If input table is empty", description = """
            Defines how the node should behave when the input table has no rows. Options include generating empty \
            outputs or failing the execution.
            """)
    @ValueSwitchWidget
    @Migration(EmptyActionMigration.class)
    public ActionOnEmptyInput m_actionEmpty = ActionOnEmptyInput.FAIL;

    interface ClassColumnRef extends Modification.Reference {
    }

    /**
     * Base class to use for modifications.
     *
     * @author Martin Sillye, TNG Technology Consulting GmbH
     */
    public abstract static class SamplingModification implements Modification.Modifier {

        /**
         * @param group to find reference in.
         * @return a modifier for the {@link Widget} annotation of the field.
         */
        protected Modification.AnnotationModifier getCountModeWidgetModifier(final Modification.WidgetGroupModifier group) {
            return group.find(ModeWidgetRef.class).modifyAnnotation(Widget.class);
        }

        /**
         * @param group to find reference in.
         * @return a modifier for the {@link Widget} annotation of the field.
         */
        protected Modification.AnnotationModifier getPercentageWidgetModifier(final Modification.WidgetGroupModifier group) {
            return group.find(PercentageWidgetRef.class).modifyAnnotation(Widget.class);
        }

        /**
         * @param group to find reference in.
         * @return a modifier for the {@link Widget} annotation of the field.
         */
        protected Modification.AnnotationModifier getRowCountWidgetModifier(final Modification.WidgetGroupModifier group) {
            return group.find(CountWidgetRef.class).modifyAnnotation(Widget.class);
        }

        /**
         * @param group to find reference in.
         * @return a modifier for the {@link Widget} annotation of the field.
         */
        protected Modification.AnnotationModifier getSamplingModeWidgetModifier(final Modification.WidgetGroupModifier group) {
            return group.find(SamplingStrategyRef.class).modifyAnnotation(Widget.class);
        }

        /**
         * @param group to find reference in.
         * @return a modifier for the {@link Widget} annotation of the field.
         */
        protected Modification.AnnotationModifier getClassColumnWidgetModifier(final Modification.WidgetGroupModifier group) {
            return group.find(ClassColumnRef.class).modifyAnnotation(Widget.class);
        }
    }

    static final class PartitioningModeMigration implements NodeParametersMigration<CountMode> {

        private static final String KEY = "method";

        private static final String RELATIVE_VALUE = "Relative";

        private static final String ABSOLUTE_VALUE = "Absolute";

        private static CountMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getString(KEY);
            return switch (value) {
                case RELATIVE_VALUE -> CountMode.RELATIVE;
                case ABSOLUTE_VALUE -> CountMode.ABSOLUTE;
                default -> throw new InvalidSettingsException(String.format("Unknown partitioning type: %s", value));
            };
        }

        @Override
        public List<ConfigMigration<CountMode>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(PartitioningModeMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }

    }

    static final class PercentageMigration implements NodeParametersMigration<Double> {

        private static final String KEY = "fraction";

        private static double load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getDouble(KEY);
            return value * 100.0;
        }

        @Override
        public List<ConfigMigration<Double>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(PercentageMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }

    }

    static final class CountMigration implements NodeParametersMigration<Long> {

        private static final String KEY = "count";

        private static long load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getInt(KEY);
        }

        @Override
        public List<ConfigMigration<Long>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(CountMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }

    }

    static final class SamplingModeMigration implements NodeParametersMigration<SamplingMode> {

        private static final String METHOD_KEY = "samplingMethod";

        private static final String STRATIFIED_KEY = "stratified_sampling";

        private static final String RANDOM_KEY = "random";

        private static final String RANDOM_VALUE = "Random";

        private static final String STRATIFIED_VALUE = "Stratified";

        private static final String LINEAR_VALUE = "Linear";

        private static final String FIRST_VALUE = "First";

        private static SamplingMode loadMode(final NodeSettingsRO settings) {
            boolean random = settings.getBoolean(RANDOM_KEY, false);
            boolean stratified = settings.getBoolean(STRATIFIED_KEY, false);

            if (stratified) {
                return SamplingMode.STRATIFIED;
            } else if (random) {
                return SamplingMode.RANDOM;
            } else {
                return SamplingMode.FIRST_ROWS;
            }
        }

        private static SamplingMode loadMethod(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getString(METHOD_KEY);
            return switch (value) {
                case FIRST_VALUE -> SamplingMode.FIRST_ROWS;
                case RANDOM_VALUE -> SamplingMode.RANDOM;
                case STRATIFIED_VALUE -> SamplingMode.STRATIFIED;
                case LINEAR_VALUE -> SamplingMode.LINEAR;
                default -> throw new InvalidSettingsException(String.format("Unknown sampling strategy: %s", value));
            };
        }

        @Override
        public List<ConfigMigration<SamplingMode>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(SamplingModeMigration::loadMethod) //
                    .withMatcher(matcher -> matcher.containsKey(METHOD_KEY))//
                    .withDeprecatedConfigPath(METHOD_KEY)//
                    .withDeprecatedConfigPath(RANDOM_KEY)//
                    .withDeprecatedConfigPath(STRATIFIED_KEY)//
                    .build(),
                ConfigMigration.builder(SamplingModeMigration::loadMode) //
                    .withMatcher(matcher -> matcher.containsKey(RANDOM_KEY) || matcher.containsKey(STRATIFIED_KEY))//
                    .withDeprecatedConfigPath(RANDOM_KEY)//
                    .withDeprecatedConfigPath(STRATIFIED_KEY)//
                    .build());
        }

    }

    static final class SeedMigration implements NodeParametersMigration<Optional<Long>> {
        private static final String KEY = "random_seed";

        private static Optional<Long> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getString(KEY);
            if (StringUtils.isEmpty(value)) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(value));
        }

        @Override
        public List<ConfigMigration<Optional<Long>>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(SeedMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }
    }

    static final class EmptyActionMigration implements NodeParametersMigration<ActionOnEmptyInput> {
        private static final String KEY = "actionEmpty";

        /**
         * @param settings not used
         */
        private static ActionOnEmptyInput load(final NodeSettingsRO settings) {
            return ActionOnEmptyInput.OUTPUT_EMPTY;
        }

        @Override
        public List<ConfigMigration<ActionOnEmptyInput>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(EmptyActionMigration::load) //
                    .withMatcher(matcher -> !matcher.containsKey(KEY)).build());
        }
    }
}
