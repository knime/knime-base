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

package org.knime.base.node.mine.neural.rprop;

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for RProp MLP Learner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class RPropNodeParameters implements NodeParameters {

    @Persist(configKey = RPropNodeModel.MAXITER_KEY)
    @Widget(title = "Maximum number of iterations", description = """
            The number of learning iterations.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidation = MaxNumberOfIterationsValidation.class)
    int m_maxIterations = RPropNodeModel.DEFAULTITERATIONS;

    static final class MaxNumberOfIterationsValidation extends MaxValidation {

        @Override
        public double getMax() {
            return RPropNodeModel.MAXNRITERATIONS;
        }

    }

    @Persist(configKey = RPropNodeModel.HIDDENLAYER_KEY)
    @Widget(title = "Number of hidden layers", description = """
            Specifies the number of hidden layers in the architecture of the neural network.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidation = MaxNumberOfHiddenLayersAndNeuronsValidation.class)
    int m_hiddenLayers = RPropNodeModel.DEFAULTHIDDENLAYERS;

    @Persist(configKey = RPropNodeModel.NRHNEURONS_KEY)
    @Widget(title = "Number of hidden neurons per layer", description = """
            Specifies the number of neurons contained in each hidden layer.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidation = MaxNumberOfHiddenLayersAndNeuronsValidation.class)
    int m_hiddenNeuronsPerLayer = RPropNodeModel.DEFAULTNEURONSPERLAYER;

    static final class MaxNumberOfHiddenLayersAndNeuronsValidation extends MaxValidation {

        @Override
        public double getMax() {
            return 100;
        }

    }

    @Persist(configKey = RPropNodeModel.CLASSCOL_KEY)
    @Widget(title = "Class column", description = """
            Choose the column that contains the target variable: it can either be nominal or numerical. All
            nominal class values are extracted and assigned to output neurons. If you use a numerical target
            variable (regression), please make sure it is normalized!
            """)
    @ChoicesProvider(AllColumnsProvider.class)
    @ValueProvider(ClassColumnProvider.class)
    @ValueReference(ClassColumnRef.class)
    String m_classColumn = "";

    static final class ClassColumnRef implements ParameterReference<String> {
    }

    @Persist(configKey = RPropNodeModel.IGNOREMV_KEY)
    @Widget(title = "Ignore missing values", description = """
            If this checkbox is set, rows with missing values will not be used for training.
            """)
    boolean m_ignoreMissingValues;

    @Persist(configKey = RPropNodeModel.USE_SEED_KEY)
    @Widget(title = "Use seed for random initialization", description = """
            If this checkbox is set, a seed (see next field) can be set for initializing the weights and
            thresholds.
            """)
    @ValueReference(UseRandomSeedRef.class)
    boolean m_useRandomSeed;

    static final class UseRandomSeedRef implements BooleanReference {
    }

    @Widget(title = "Random seed", description = """
            Seed for the random number generator.
            """)
    @Persist(configKey = RPropNodeModel.SEED_KEY)
    @Effect(predicate = UseRandomSeedRef.class, type = EffectType.SHOW)
    @ValueProvider(SeedValueProvider.class)
    int m_randomSeed;

    @Widget(title = "New", description = """
            Generate a random seed and set it in the Random seed input above for reproducible runs.
            """)
    @SimpleButtonWidget(ref = NewSeedButtonRef.class)
    @Effect(predicate = UseRandomSeedRef.class, type = EffectType.SHOW)
    Void m_newSeed;

    static final class NewSeedButtonRef implements ButtonReference {
    }

    static final class ClassColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected ClassColumnProvider() {
            super(ClassColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns = ColumnSelectionUtil.getAllColumnsOfFirstPort(parametersInput);
            return compatibleColumns.isEmpty() ? Optional.empty() :
                Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

    static final class SeedValueProvider implements StateProvider<Integer> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(NewSeedButtonRef.class);
        }

        @Override
        public Integer computeState(final NodeParametersInput parametersInput) {
            return (int)(2 * (Math.random() - 0.5) * Integer.MAX_VALUE); // NOSONAR legacy behavior
        }

    }

}
