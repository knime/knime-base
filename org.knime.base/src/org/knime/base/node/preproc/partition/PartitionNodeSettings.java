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
 *   Apr 14, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.partition;

import java.util.List;
import java.util.stream.Stream;

import org.knime.base.util.prepoc.sample.SamplingUtil.CountMode;
import org.knime.base.util.prepoc.sample.SamplingUtil.SamplingMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 * @since 5.5
 */
@SuppressWarnings("restriction")
final class PartitionNodeSettings implements DefaultNodeSettings {

    PartitionNodeSettings() {

    }

    PartitionNodeSettings(final DefaultNodeSettingsContext context) {
        final var firstCol = context.getDataTableSpec(0) //
            .map(DataTableSpec::stream) //
            .orElseGet(Stream::empty) //
            .filter(spec -> spec.getType().isCompatible(NominalValue.class)).findFirst();
        if (firstCol.isPresent()) {
            this.m_classColumn = firstCol.get().getName();
        }
    }

    interface CountMethodRef extends Reference<CountMode> {
    }

    static final class IsRelative implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer init) {
            return init.getEnum(CountMethodRef.class).isOneOf(CountMode.RELATIVE);
        }
    }

    static final class IsAbsolute implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer init) {
            return init.getEnum(CountMethodRef.class).isOneOf(CountMode.ABSOLUTE);
        }
    }

    @Widget(title = "First partition type", description = """
            Defines how the size of the first partition is specified: as a percentage of total rows (relative) or \
            as an absolute number of rows.
            """)
    @ValueSwitchWidget
    @ValueReference(CountMethodRef.class)
    @Migration(PartitioningModeMigration.class)
    CountMode m_partitioningMode = CountMode.RELATIVE;

    @Widget(title = "Relative size", description = """
            Specifies the percentage of rows from the input table to be included in the first partition. Must be \
            between 0 and 100 (inclusive).
            """)
    @Effect(type = EffectType.SHOW, predicate = IsRelative.class)
    @NumberInputWidget(validation = {MinPercentageValidation.class, MaxPercentageValidation.class})
    @Migration(PercentageMigration.class)
    double m_percentage = 70;

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
            Specifies the absolute number of rows to include in the first partition. If the input table contains \
            fewer rows than specified, all rows are placed in the first table, and the second table will be empty.
            """)
    @Effect(type = EffectType.SHOW, predicate = IsAbsolute.class)
    @NumberInputWidget(validation = MinRowCountValidation.class)
    @Migration(CountMigration.class)
    int m_rowCount = 100;

    static final class MinRowCountValidation extends NumberInputWidgetValidation.MinValidation {

        @Override
        protected double getMin() {
            return 1;
        }
    }

    @Widget(title = "Sampling strategy", description = """
            Determines how rows are selected for the first partition. Strategies include random, linear, stratified, \
            and first rows (sequential).
            """)
    @ValueSwitchWidget
    @ValueReference(SamplingStrategyRef.class)
    @Migration(SamplingModeMigration.class)
    SamplingMode m_mode = SamplingMode.RANDOM;

    interface SamplingStrategyRef extends Reference<SamplingMode> {

    }

    static final class UseGroupColumn implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(SamplingStrategyRef.class).isOneOf(SamplingMode.STRATIFIED);
        }

    }

    static final class UseSeed implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(SamplingStrategyRef.class).isOneOf(SamplingMode.STRATIFIED, SamplingMode.RANDOM);
        }

    }

    static final class UseFixedSeed implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(SamplingStrategyRef.class).isOneOf(SamplingMode.STRATIFIED, SamplingMode.RANDOM)
                .and(i.getBoolean(UseRandomSeedRef.class).isTrue());
        }

    }

    static final class NominalColumnChoicesProvider implements ColumnChoicesProvider {

        @Override
        public List<DataColumnSpec> columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0) //
                .map(DataTableSpec::stream) //
                .orElseGet(Stream::empty) //
                .filter(spec -> spec.getType().isCompatible(NominalValue.class)).toList();
        }

    }

    @Widget(title = "Group column", description = """
            Specifies the column whose value distribution should be preserved in stratified sampling. Ensures both \
            output tables reflect the same distribution of values.
            """)
    @Effect(type = EffectType.SHOW, predicate = UseGroupColumn.class)
    @ChoicesProvider(NominalColumnChoicesProvider.class)
    @Persist(configKey = "class_column")
    String m_classColumn = "";

    //TODO Remove when UIEXT-1742
    @Widget(title = "Use fixed random seed", description = "If selected, a fixed random seed can be provided")
    @ValueReference(UseRandomSeedRef.class)
    @Effect(type = EffectType.SHOW, predicate = UseSeed.class)
    @Migration(UseRandomSeedMigration.class)
    boolean m_useFixedRandomSeed;

    interface UseRandomSeedRef extends Reference<Boolean> {

    }

    @Widget(title = "Fixed random seed", description = """
            Optional seed value for random or stratified sampling. Using a seed ensures the same rows are selected \
            each time the node is executed. Without a seed, a different random selection will occur each time.
            """)
    @Effect(type = EffectType.SHOW, predicate = UseFixedSeed.class)
    @Migration(SeedMigration.class)
    //TODO make this optional UIEXT-1742
    long m_seed = 1678807467440L;

    enum ActionOnEmptyInput {
            @Label("Fail")
            FAIL, //
            @Label("Output empty tables")
            OUTPUT_EMPTY
    }

    @Widget(title = "If input table is empty", description = """
            Defines how the node should behave when the input table has no rows. Options include generating empty \
            outputs or failing the execution.
            """)
    @ValueSwitchWidget
    @Migration(EmptyActionMigration.class)
    ActionOnEmptyInput m_actionEmpty = ActionOnEmptyInput.FAIL;

    static final class PartitioningModeMigration implements NodeSettingsMigration<CountMode> {

        private static final String KEY = "method";

        private static final String RELATIVE_VALUE = "Relative";

        private static final String ABSOLUTE_VALUE = "Absolute";

        private static CountMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getString(KEY);
            return switch (value) {
                case RELATIVE_VALUE -> CountMode.RELATIVE;
                case ABSOLUTE_VALUE -> CountMode.ABSOLUTE;
                default -> throw new InvalidSettingsException(String.format("Unknown partitioning type: %0", value));
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

    static final class PercentageMigration implements NodeSettingsMigration<Double> {

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

    static final class SamplingModeMigration implements NodeSettingsMigration<SamplingMode> {

        private static final String METHOD_KEY = "samplingMethod";

        private static final String STRATIFIED_KEY = "stratified_sampling";

        private static final String RANDOM_KEY = "random";

        private static final String FIRST_VALUE = "First";

        private static final String RANDOM_VALUE = "Random";

        private static final String STRATIFIED_VALUE = "Stratified";

        private static final String LINEAR_VALUE = "Linear";

        private static SamplingMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getString(METHOD_KEY, null);
            if (value == null) {
                boolean stratified = settings.getBoolean(STRATIFIED_KEY, false);

                if (stratified) {
                    return SamplingMode.STRATIFIED;
                } else {
                    return SamplingMode.RANDOM;
                }
            } else {
                return switch (value) {
                    case FIRST_VALUE -> SamplingMode.FIRST_ROWS;
                    case RANDOM_VALUE -> SamplingMode.RANDOM;
                    case STRATIFIED_VALUE -> SamplingMode.STRATIFIED;
                    case LINEAR_VALUE -> SamplingMode.LINEAR;
                    default -> throw new InvalidSettingsException(
                        String.format("Unknown sampling strategy: %0", value));
                };
            }

        }

        @Override
        public List<ConfigMigration<SamplingMode>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(SamplingModeMigration::load) //
                    .withMatcher(matcher -> matcher.containsKey(METHOD_KEY) || matcher.containsKey(RANDOM_KEY)
                        || matcher.containsKey(STRATIFIED_KEY))//
                    .withDeprecatedConfigPath(METHOD_KEY)//
                    .withDeprecatedConfigPath(RANDOM_KEY)//
                    .withDeprecatedConfigPath(STRATIFIED_KEY)//
                    .build());
        }

    }

    static final class CountMigration implements NodeSettingsMigration<Integer> {

        private static final String KEY = "count";

        private static int load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getInt(KEY);
            return value;
        }

        @Override
        public List<ConfigMigration<Integer>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(CountMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }

    }

    //TODO fix when UIEXT-1742
    static final class UseRandomSeedMigration implements NodeSettingsMigration<Boolean> {

        private static final String KEY = "random_seed";

        private static boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getString(KEY);
            if (value == null || "null".equals(value)) {
                return false;
            }
            return true;
        }

        @Override
        public List<ConfigMigration<Boolean>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(UseRandomSeedMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }
    }

    //TODO fix when UIEXT-1742
    static final class SeedMigration implements NodeSettingsMigration<Long> {
        private static final String KEY = "random_seed";

        private static long load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var value = settings.getString(KEY);
            if (value == null || "null".equals(value)) {
                return 1678807467440L;
            }
            return Long.parseLong(value);
        }

        @Override
        public List<ConfigMigration<Long>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(SeedMigration::load) //
                    .withDeprecatedConfigPath(KEY)//
                    .build());
        }
    }

    static final class EmptyActionMigration implements NodeSettingsMigration<ActionOnEmptyInput> {
        private static final String KEY = "actionEmpty";

        /**
         * @param settings not used
         */
        private static ActionOnEmptyInput load(final NodeSettingsRO settings) throws InvalidSettingsException {
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
