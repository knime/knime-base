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

package org.knime.base.node.preproc.targetshuffling;

import org.apache.commons.lang.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LongAsStringPersistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.UpdateOnOpenValueProvider;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for Target Shuffling.
 *
 * @author Ali Asghar Marvi, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class TargetShufflingNodeParameters implements NodeParameters {

    TargetShufflingNodeParameters() {
    }

    TargetShufflingNodeParameters(final NodeParametersInput input) {
        m_columnName = guessColumnName(input);
    }

    private static String guessColumnName(final NodeParametersInput input) {
        return ColumnSelectionUtil.getFirstColumnOfFirstPort(input).map(DataColumnSpec::getName).orElse("");
    }

    static final class ColumnsNameValueProvider extends UpdateOnOpenValueProvider<String> {
        @Override
        protected String getValueOnOpen(final String currentValue, final NodeParametersInput parametersInput) {
            return StringUtils.stripToNull(currentValue) == null ? guessColumnName(parametersInput) : currentValue;
        }
    }

    /**
     * The column to shuffle.
     */
    @Widget(title = "Column to shuffle",
        description = "Select the column whose values should be randomly shuffled. This breaks the relationship "
            + "between this column and other columns in the table, which is useful for creating negative controls "
            + "in machine learning experiments.")
    @ValueReference(ColumnsNameValueProvider.class)
    @ValueProvider(ColumnsNameValueProvider.class)
    @ChoicesProvider(AllColumnsProvider.class)
    String m_columnName = "";

    static final class UseSeedRef implements BooleanReference {
    }

    /**
     * Whether to use a fixed seed for reproducible shuffling.
     */
    @Widget(title = "Use seed",
        description = "When enabled, the shuffling will use a fixed seed value, making the randomization "
            + "reproducible across multiple executions. When disabled, each execution will produce different "
            + "random shuffling results.")
    @ValueReference(UseSeedRef.class)
    @Persist(configKey = TargetShufflingSettings.CFGKEY_USESEED)
    boolean m_useSeed = false; // NOSONAR explicit default

    private static final class UseSeedPredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(UseSeedRef.class);
        }
    }

    static final class SeedPersistor extends LongAsStringPersistor {
        SeedPersistor() {
            super(TargetShufflingSettings.CFGKEY_SEED);
        }
    }

    /**
     * Provides a new random seed whenever the draw-seed button is clicked.
     */
    static final class SeedValueProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            // Update only when the button is clicked
            initializer.computeOnButtonClick(DrawSeedButtonRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            // copied from TargetShufflingNodeDialog
            long l1 = Double.doubleToLongBits(Math.random());
            long l2 = Double.doubleToLongBits(Math.random());
            long l = ((0xFFFFFFFFL & l1) << 32) + (0xFFFFFFFFL & l2);
            return Long.toString(l);
        }
    }

    /**
     * The seed value for reproducible shuffling.
     */
    @Persistor(SeedPersistor.class)
    @Widget(title = "Random seed",
        description = "The seed value used for random number generation. Use the same seed to get identical "
            + "shuffling results across multiple executions. You can enter a custom value or use the random "
            + "seed generation button below.")
    @TextInputWidget(patternValidation = LongAsStringPersistor.IsLongInteger.class)
    @Effect(predicate = UseSeedPredicate.class, type = EffectType.SHOW)
    @ValueProvider(SeedValueProvider.class)
    String m_seed = "0";

    /** Button reference used to trigger random seed generation. */
    static final class DrawSeedButtonRef implements ButtonReference {
    }

    /**
     * Button to draw a random seed and set it to the seed field.
     */
    @Widget(title = "Draw seed",
        description = "Generate a random seed and set it in the Random seed input above for reproducible runs.")
    @SimpleButtonWidget(ref = DrawSeedButtonRef.class)
    @Effect(predicate = UseSeedPredicate.class, type = EffectType.SHOW)
    Void m_drawSeed;
}
