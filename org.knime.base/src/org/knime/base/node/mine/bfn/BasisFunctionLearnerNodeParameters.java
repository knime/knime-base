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
 *   Feb 24, 2026 (magnus): created
 */
package org.knime.base.node.mine.bfn;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.internal.StateProviderInitializerInternal;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;

/**
 * {@link NodeParameters} for the basis function learner nodes.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @since 5.12
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
public abstract class BasisFunctionLearnerNodeParameters implements NodeParameters {

    @Widget(title = "Missing values", description = "Select one method to handle missing values.")
    @Persistor(MissingValuePersistor.class)
    MissingValueReplacement m_missing = MissingValueReplacement.INCORP;

    @Widget(title = "Shrink after commit", description = """
            If selected, a shrink to reduce conflicting rules is executed
            immediately after a new rule is committed, i.e. the new rule is reduced so that conflicts with
            all other rules of different classes are avoided.
            """)
    @Persist(configKey = BasisFunctionLearnerNodeModel.SHRINK_AFTER_COMMIT)
    boolean m_shrinkAfterCommit;

    @Widget(title = "Use class with max coverage", description = """
            If selected, only the class with maximum coverage degree of the target columns is used during
            training, otherwise all class columns are considered for coverage.
            """)
    @Persist(configKey = BasisFunctionLearnerNodeModel.MAX_CLASS_COVERAGE)
    boolean m_maxClassCoverage = true;

    @Widget(title = "Use maximum number of epochs", description = """
            If selected, the option defines the maximum number of epochs the algorithm has to process the
            entire data set, otherwise it repeats this process until this rule model is stable, i.e. no new
            rule has been committed and/or no shrink is executed.
            """)
    @Persistor(MaxEpochsParametersPersistor.class)
    @OptionalWidget(defaultProvider = MaxEpochDefaultProvider.class)
    Optional<Integer> m_maxEpochsParameters = Optional.of(42);

    static final class MaxEpochDefaultProvider implements DefaultValueProvider<Integer> {

        @Override
        public Integer computeState(final NodeParametersInput parametersInput) {
            return 42;
        }

    }

    @Widget(title = "Target columns", description = """
            Select the target(s) to be used for classification. If more than one column (only numeric) is
            selected, the columns must contain class membership values between 0 and 1 for each class given
            by the column name.
            """)
    @TwinlistWidget
    @ChoicesProvider(AllColumnsProvider.class)
    @Persist(configKey = BasisFunctionLearnerNodeModel.TARGET_COLUMNS)
    @ValueProvider(TargetColumnsProvider.class)
    @ValueReference(TargetColumnsRef.class)
    @Migration(SingleTargetColumnNameMigration.class)
    String[] m_targetColumns = new String[0];

    static final class TargetColumnsRef implements ParameterReference<String[]> {
    }

    static final class SingleTargetColumnNameMigration implements NodeParametersMigration<String[]> {

        static final String LEGACY_TARGET_COLUMN = UUID.randomUUID().toString();

        static String[] loadTargetColumn(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new String[]{settings.getString(BasisFunctionLearnerNodeModel.TARGET_COLUMNS)};
        }

        @Override
        public List<ConfigMigration<String[]>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(SingleTargetColumnNameMigration::loadTargetColumn)
                .withMatcher(settings-> settings.containsKey(BasisFunctionLearnerNodeModel.TARGET_COLUMNS)
                    && !settings.getString(BasisFunctionLearnerNodeModel.TARGET_COLUMNS, LEGACY_TARGET_COLUMN)
                        .equals(LEGACY_TARGET_COLUMN))
                .build());
        }

    }

    // Invisible parameter still persisted for backward compatibility
    @Persist(configKey = BasisFunctionLearnerNodeModel.DISTANCE)
    int m_distance;

    static final class TargetColumnsProvider implements StateProvider<String[]> {

        Supplier<String[]> m_targetColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            ((StateProviderInitializerInternal)initializer).computeOnParametersLoaded();
            m_targetColumnsSupplier = initializer.getValueSupplier(TargetColumnsRef.class);
        }

        @Override
        public String[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var compatibleColumns = ColumnSelectionUtil.getAllColumnsOfFirstPort(parametersInput);
            final var targetColumns = m_targetColumnsSupplier.get();
            if (targetColumns == null || targetColumns.length == 0) {
                return compatibleColumns.isEmpty() ? new String[0] :
                    new String[]{compatibleColumns.get(compatibleColumns.size() - 1).getName()};
            }
            throw new StateComputationFailureException();
        }

    }

    static final class MaxEpochsParametersPersistor implements NodeParametersPersistor<Optional<Integer>> {

        @Override
        public Optional<Integer> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            int maxEpochs = settings.getInt(BasisFunctionLearnerNodeModel.MAX_EPOCHS, -1);
            return maxEpochs > 0 ? Optional.of(maxEpochs) : Optional.empty();
        }

        @Override
        public void save(final Optional<Integer> param, final NodeSettingsWO settings) {
            settings.addInt(BasisFunctionLearnerNodeModel.MAX_EPOCHS, param.isPresent() ? param.get() : -1);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{BasisFunctionLearnerNodeModel.MAX_EPOCHS}};
        }

    }

    static final class MissingValuePersistor implements NodeParametersPersistor<MissingValueReplacement> {

        @Override
        public MissingValueReplacement load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return MissingValueReplacement.getFromIndex(
                settings.getInt(BasisFunctionLearnerTable.MISSING, MissingValueReplacement.INCORP.getIndex()));
        }

        @Override
        public void save(final MissingValueReplacement param, final NodeSettingsWO settings) {
            settings.addInt(BasisFunctionLearnerTable.MISSING, param.getIndex());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{BasisFunctionLearnerTable.MISSING}};
        }

    }

    enum MissingValueReplacement {

        @Label(value = "Incorp", description = """
                May generate fuzzy rules with missing values, if no replacement value has been found during the
                learning process.
                """)
        INCORP(BasisFunctionLearnerTable.MISSINGS[0].toString(), 0), //
        @Label(value = "Best Guess", description = """
                Computes the optimal replacement value by projecting the fuzzy rule (with missing value(s)) onto the
                missing dimension(s) of all other rules.
                """)
        BEST_GUESS(BasisFunctionLearnerTable.MISSINGS[1].toString(), 1), //
        @Label(value = "Mean", description = "Replaces the missing value(s) with the column mean.")
        MEAN(BasisFunctionLearnerTable.MISSINGS[2].toString(), 2), //
        @Label(value = "Min", description = "Replaces the missing value(s) with the column min.")
        MIN(BasisFunctionLearnerTable.MISSINGS[3].toString(), 3), //
        @Label(value = "Max", description = "Replaces the missing value(s) with the column max.")
        MAX(BasisFunctionLearnerTable.MISSINGS[4].toString(), 4), //
        @Label(value = "Zero", description = "Replaces the missing value(s) with zero.")
        ZERO(BasisFunctionLearnerTable.MISSINGS[5].toString(), 5), //
        @Label(value = "One", description = "Replaces the missing value(s) with one.")
        ONE(BasisFunctionLearnerTable.MISSINGS[6].toString(), 6);

        private final String m_value;
        private final int m_index;

        MissingValueReplacement(final String value, final int index) {
            m_value = value;
            m_index = index;
        }

        String getValue() {
            return m_value;
        }

        Integer getIndex() {
            return m_index;
        }

        static MissingValueReplacement getFromIndex(final Integer value) throws InvalidSettingsException {
            for (final MissingValueReplacement missing : values()) {
                if (missing.getIndex().equals(value)) {
                    return missing;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final Integer index) {
            var values = Arrays.asList(MissingValueReplacement.values()).stream()
                    .map(missing -> "(%s := %s)".formatted(missing.getValue(), missing.getIndex()))
                    .collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", index, values);
        }

    }

}
