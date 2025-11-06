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

package org.knime.base.node.mine.smote;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LongAsStringPersistor;
import org.knime.node.parameters.updates.ButtonReference;
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
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveDoubleValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for SMOTE.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class SmoteNodeParameters implements NodeParameters {

    @Widget(title = "Class column", description = """
            Pick the column that contains the class information.
            """)
    @ChoicesProvider(StringColumnsProvider.class)
    @ValueProvider(ClassColumnProvider.class)
    @ValueReference(ClassColumnRef.class)
    @Persist(configKey = SmoteNodeModel.CFG_CLASS)
    String m_classColumn;

    @Widget(title = "# Nearest neighbor", description = """
            An option that determines how many nearest neighbors shall be considered. The algorithm picks an
            object from the target class, randomly selects one of its neighbors and draws the new synthetic
            example along the line between the sample and the neighbor.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = SmoteNodeModel.CFG_KNN)
    int m_nearestNeighbors = 5;

    @Widget(title = "Oversampling method", description = """
            Select the oversampling method.
            """)
    @ValueSwitchWidget
    @ValueReference(OversamplingMethodRef.class)
    @Persistor(OversamplingMethodPersistor.class)
    OversamplingMethod m_oversamplingMethod = OversamplingMethod.OVERSAMPLE_BY;

    @Widget(title = "Oversampling rate", description = """
            Specify how much synthetic data is introduced, e.g. a value of 2 will introduce two more portions
            for each class (if there are 50 rows in the input table labeled as "A"; the output will contain 150
            rows belonging to "A").
            """)
    @NumberInputWidget(minValidation = IsPositiveDoubleValidation.class)
    @Effect(predicate = OversamplingMethodIsOversampleBy.class, type = EffectType.SHOW)
    @Persist(configKey = SmoteNodeModel.CFG_RATE)
    double m_oversamplingRate = 2.0;

    @Persistor(SeedParametersPersistor.class)
    SeedParameters m_seedParameters = new SeedParameters();

    static final class SeedParameters implements NodeParameters {

        @Persist(hidden = true)
        @Widget(title = "Enable static seed", description = """
                Check this option if you want to use a seed for the random number generator. This will cause
                consecutive runs of the node to produce the same output data. If unchecked, each run of the node
                generates a new seed.
                """)
        @ValueReference(UseRandomSeedRef.class)
        boolean m_useRandomSeed;

        @Widget(title = "Random seed", description = """
                The seed value for the random number generator.
                """)
        @TextInputWidget(patternValidation = LongAsStringPersistor.IsLongInteger.class)
        @Effect(predicate = IsUseRandomSeed.class, type = EffectType.SHOW)
        @ValueProvider(NewSeedValueProvider.class)
        String m_randomSeed;

        @Widget(title = "New",
                description = "Generate a random seed and set it in the Random seed input above for reproducible runs.")
        @SimpleButtonWidget(ref = NewSeedButtonRef.class)
        @Effect(predicate = IsUseRandomSeed.class, type = EffectType.SHOW)
        Void m_newSeed;

    }

    enum OversamplingMethod {

        @Label(value = "Rate", description = "This option oversamples each class equally.")
        OVERSAMPLE_BY(SmoteNodeModel.METHOD_ALL), //

        @Label(value = "Minority classes", description = """
                This option adds synthetic examples to all classes that are not the majority class. The output contains
                the same number of rows for each of the possible classes.
                """)
        OVERSAMPLE_MINORITY_CLASSES(SmoteNodeModel.METHOD_MAJORITY);

        private String m_value;

        OversamplingMethod(final String value) {
            m_value = value;
        }

        String getValue() {
            return m_value;
        }

        static OversamplingMethod getFromValue(final String value) throws InvalidSettingsException {
            for (final OversamplingMethod condition : values()) {
                if (condition.getValue().equals(value)) {
                    return condition;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values = List.of(SmoteNodeModel.METHOD_ALL, SmoteNodeModel.METHOD_MAJORITY).stream()
                    .collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

    static final class ClassColumnRef implements ParameterReference<String> {
    }

    static final class OversamplingMethodRef implements ParameterReference<OversamplingMethod> {
    }

    static final class UseRandomSeedRef implements ParameterReference<Boolean> {
    }

    static final class NewSeedButtonRef implements ButtonReference {
    }

    static final class OversamplingMethodIsOversampleBy implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OversamplingMethodRef.class).isOneOf(OversamplingMethod.OVERSAMPLE_BY);
        }

    }

    static final class IsUseRandomSeed implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseRandomSeedRef.class).isTrue();
        }

    }

    static final class ClassColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected ClassColumnProvider() {
            super(ClassColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                    ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, StringValue.class);
            return compatibleColumns.isEmpty() ?
                Optional.empty() : Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    static final class NewSeedValueProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(NewSeedButtonRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            return Long.toString(Double.doubleToLongBits(Math.random()));
        }

    }

    static final class OversamplingMethodPersistor implements NodeParametersPersistor<OversamplingMethod> {

        @Override
        public OversamplingMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return OversamplingMethod.getFromValue(
                settings.getString(SmoteNodeModel.CFG_METHOD, SmoteNodeModel.METHOD_ALL));
        }

        @Override
        public void save(final OversamplingMethod param, final NodeSettingsWO settings) {
            settings.addString(SmoteNodeModel.CFG_METHOD, param.getValue());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SmoteNodeModel.CFG_METHOD}};
        }

    }

    static final class SeedParametersPersistor implements NodeParametersPersistor<SeedParameters> {

        @Override
        public SeedParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var seedParameters = new SeedParameters();
            var seedSetting = settings.getString(SmoteNodeModel.CFG_SEED, null);
            seedParameters.m_useRandomSeed = seedSetting != null;
            seedParameters.m_randomSeed = seedSetting;
            return seedParameters;
        }

        @Override
        public void save(final SeedParameters param, final NodeSettingsWO settings) {
            if (param.m_useRandomSeed) {
                settings.addString(SmoteNodeModel.CFG_SEED, param.m_randomSeed);
            } else {
                settings.addString(SmoteNodeModel.CFG_SEED, null);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SmoteNodeModel.CFG_SEED}};
        }

    }

}
