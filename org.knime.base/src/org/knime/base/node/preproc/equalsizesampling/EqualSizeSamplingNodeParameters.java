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

package org.knime.base.node.preproc.equalsizesampling;

import java.util.Optional;
import java.util.Random;

import org.knime.base.node.preproc.equalsizesampling.EqualSizeSamplingConfiguration.SamplingMethod;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
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
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Equal Size Sampling.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class EqualSizeSamplingNodeParameters implements NodeParameters {

    /**
     * Default constructor.
     */
    EqualSizeSamplingNodeParameters() {
    }

    /**
     * Determines a class column from the context if possible.
     *
     * @param context to retrieve the data table spec from
     */
    EqualSizeSamplingNodeParameters(final NodeParametersInput context) {
        final var firstNominalCol = context.getInTableSpec(0).stream()
            .flatMap(DataTableSpec::stream)
            .filter(col -> col.getType().isCompatible(NominalValue.class))
            .findFirst();
        if (firstNominalCol.isPresent()) {
            m_classColumn = firstNominalCol.get().getName();
        }
    }

    @Persist(configKey = EqualSizeSamplingConfiguration.CFGKEY_CLASS_COLUMN)
    @Widget(title = "Nominal column", description = """
            Select the class column here. The node will run over the data set once to count the occurrences
            in this selected column and then do the filtering in a second pass. Note that missing values in
            this column are treated as a separate category (can also build the minority class).
            """)
    @ChoicesProvider(value = NominalColumnsProvider.class)
    @ValueReference(ClassColumnRef.class)
    @ValueProvider(ClassColumnProvider.class)
    String m_classColumn;

    @Persist(configKey = EqualSizeSamplingConfiguration.CFGKEY_SAMPLING_METHOD)
    @Widget(title = "Sampling method", description = "Choose the sampling method for equal size sampling:")
    @RadioButtonsWidget
    SamplingMethod m_samplingMethod = SamplingMethod.Exact;

    @Persistor(SeedParametersPersistor.class)
    SeedParameters m_seedParameters = new SeedParameters();

    static final class ClassColumnRef implements ParameterReference<String> {
    }

    static final class UseRandomSeedRef implements ParameterReference<Boolean> {
    }

    static final class DrawSeedButtonRef implements ButtonReference {
    }

    static final class IsUseRandomSeed implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseRandomSeedRef.class).isTrue();
        }
    }

    static final class SeedValueProvider implements StateProvider<String> {

        static final Random RANDOM = new Random();

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(DrawSeedButtonRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return Long.toString(RANDOM.nextLong());
        }

    }

    static final class NominalColumnsProvider extends CompatibleColumnsProvider {

        protected NominalColumnsProvider() {
            super(NominalValue.class);
        }

    }

    static final class ClassColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected ClassColumnProvider() {
            super(ClassColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(parametersInput, NominalValue.class);
        }

    }

    static final class SeedParameters implements NodeParameters {

        @Persist(hidden = true)
        @Widget(title = "Enable static seed", description = """
                If selected, the removal of rows is driven by a static seed for reproducible results.
                """)
        @ValueReference(UseRandomSeedRef.class)
        boolean m_useRandomSeed;

        @Persist(configKey = EqualSizeSamplingConfiguration.CFGKEY_SEED)
        @Widget(title = "Random seed", description = """
                The seed value for the random number generator.
                """)
        @TextInputWidget(patternValidation = LongAsStringPersistor.IsLongInteger.class)
        @Effect(predicate = IsUseRandomSeed.class, type = EffectType.SHOW)
        @ValueProvider(SeedValueProvider.class)
        String m_randomSeed;

        @Widget(title = "Draw seed",
            description = "Generate a random seed and set it in the Random seed input above for reproducible runs.")
        @SimpleButtonWidget(ref = DrawSeedButtonRef.class)
        @Effect(predicate = IsUseRandomSeed.class, type = EffectType.SHOW)
        Void m_drawSeed;

    }

    static final class SeedParametersPersistor implements NodeParametersPersistor<SeedParameters> {

        @Override
        public SeedParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            SeedParameters param = new SeedParameters();
            String seedS = settings.getString(EqualSizeSamplingConfiguration.CFGKEY_SEED, null);
            param.m_randomSeed = seedS;
            param.m_useRandomSeed = seedS != null;
            return param;
        }

        @Override
        public void save(final SeedParameters param, final NodeSettingsWO settings) {
            if (param.m_useRandomSeed) {
                settings.addString(EqualSizeSamplingConfiguration.CFGKEY_SEED, param.m_randomSeed);
            } else {
                settings.addString(EqualSizeSamplingConfiguration.CFGKEY_SEED, null);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{EqualSizeSamplingConfiguration.CFGKEY_SEED}};
        }

    }

}
