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

package org.knime.base.node.mine.decisiontree2.image;

import org.knime.base.node.mine.decisiontree2.image.DecTreeToImageNodeSettings.Scaling;
import org.knime.base.node.mine.decisiontree2.image.DecTreeToImageNodeSettings.UnfoldMethod;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Decision Tree to Image.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
final class DecTreeToImageNodeParameters implements NodeParameters {

    @Widget(title = "Width (in Pixel)", description = """
            The width of the output image.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = DecTreeToImageNodeSettings.WIDTH)
    int m_width = 800;

    @Widget(title = "Height (in Pixel)", description = """
            The height of the output image.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Persist(configKey = DecTreeToImageNodeSettings.HEIGHT)
    int m_height = 600;

    @Widget(title = "Tree scaling", description = """
            Controls how the decision tree is scaled to fit into the image area.
            """)
    @ValueSwitchWidget
    @ValueReference(ScalingRef.class)
    @Persist(configKey = DecTreeToImageNodeSettings.SCALING)
    Scaling m_scaling = Scaling.shrink;

    static final class ScalingRef implements ParameterReference<Scaling> {
    }

    @Widget(title = "Zoom", description = """
            The zoom factor applied when <b>Fixed value</b> scaling is selected. Values range from 10% to 500%.
            """)
    @NumberInputWidget(minValidation = IsScaleFactorMinValidation.class,
        maxValidation = IsScaleFactorMaxValidation.class, stepSize = 0.1)
    @Effect(predicate = IsScalingFixed.class, type = EffectType.SHOW)
    @Persistor(ScaleFactorFloatPersistor.class)
    double m_scaleFactor = 100;

    static final class IsScaleFactorMinValidation extends MinValidation {

        @Override
        public double getMin() {
            return 10;
        }

    }

    static final class IsScaleFactorMaxValidation extends MaxValidation {

        @Override
        public double getMax() {
            return 500;
        }

    }

    @Widget(title = "Branch display method", description = """
            Controls which branches of the decision tree are rendered on the image.
            """)
    @ValueSwitchWidget
    @ValueReference(UnfoldMethodRef.class)
    @Persist(configKey = DecTreeToImageNodeSettings.UNFOLD_METHOD)
    UnfoldMethod m_unfoldMethod = UnfoldMethod.totalCoverage;

    static final class UnfoldMethodRef implements ParameterReference<UnfoldMethod> {
    }

    @Widget(title = "Unfold to level", description = """
            The tree is unfolded from the root (level 0) down to this level. For example, entering 2 will show the
            root, its children, and their children.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Effect(predicate = IsUnfoldByLevel.class, type = EffectType.SHOW)
    @Persist(configKey = DecTreeToImageNodeSettings.UNFOLD_TO_Level)
    int m_unfoldToLevel = 2;

    @Widget(title = "Unfold with data coverage", description = """
            All branches with a total data coverage greater than this threshold are unfolded.
            Values range from 0% to 100%.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, maxValidation = IsCoverageMaxValidation.class,
        stepSize = 0.01)
    @Effect(predicate = IsUnfoldByCoverage.class, type = EffectType.SHOW)
    @Persistor(UnfoldWithCoveragePersistor.class)
    double m_unfoldWithCoverage = 5;

    static final class IsCoverageMaxValidation extends MaxValidation {

        @Override
        public double getMax() {
            return 100;
        }

    }

    @Widget(title = "Display table", description = """
            If checked, each node of the decision tree will include a table showing the class distribution of the
            training data that reached that node.
            """)
    @Persist(configKey = DecTreeToImageNodeSettings.DISPLAY_TABLE)
    boolean m_displayTable = true;

    @Widget(title = "Display chart", description = """
            If checked, each node of the decision tree will include a bar chart visualizing the class distribution.
            Note that the chart is only available when the optional data input provides a column with color
            information.
            """)
    @Persist(configKey = DecTreeToImageNodeSettings.DISPLAY_CHART)
    boolean m_displayChart = true;

    static final class IsScalingFixed implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ScalingRef.class).isOneOf(Scaling.fixed);
        }

    }

    static final class IsUnfoldByLevel implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(UnfoldMethodRef.class).isOneOf(UnfoldMethod.level);
        }

    }

    static final class IsUnfoldByCoverage implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(UnfoldMethodRef.class).isOneOf(UnfoldMethod.totalCoverage);
        }

    }

    static final class ScaleFactorFloatPersistor implements NodeParametersPersistor<Double> {

        private static final String CFG_KEY = DecTreeToImageNodeSettings.SCALE_FACTOR;

        @Override
        public Double load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return (double)100*settings.getFloat(CFG_KEY);
        }

        @Override
        public void save(final Double param, final NodeSettingsWO settings) {
            final Double percentage = param / 100.0;
            settings.addFloat(CFG_KEY, percentage.floatValue());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY}};
        }

    }

    static final class UnfoldWithCoveragePersistor implements NodeParametersPersistor<Double> {

        private static final String CFG_KEY = DecTreeToImageNodeSettings.UNFOLD_WITH_COVERAGE;

        @Override
        public Double load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return 100*settings.getDouble(CFG_KEY);
        }

        @Override
        public void save(final Double param, final NodeSettingsWO settings) {
            settings.addDouble(CFG_KEY, param / 100.0);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY}};
        }

    }

}
