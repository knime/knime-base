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
package org.knime.base.node.mine.knn;

import java.util.Optional;

import org.apache.commons.lang.ArrayUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.NominalValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for K Nearest Neighbor.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
final class KnnNodeFactory2Parameters implements NodeParameters {

    /**
     * Provides columns compatible with {@link NominalValue} from the first input port
     * (training data).
     */
    static final class NominalColumnsProvider extends CompatibleColumnsProvider {

        NominalColumnsProvider() {
            super(NominalValue.class);
        }
    }

    /**
     * Custom validation for k to ensure maximum of 1000.
     */
    static final class MaxKValidation extends MaxValidation {
        @Override
        protected double getMax() {
            return 1000;
        }
    }

    @Widget(title = "Column with class labels", description = //
            "Select the column to be used as classification attribute. This column must contain nominal values.")
    @ChoicesProvider(NominalColumnsProvider.class)
    @Persist(configKey = KnnSettings2.CFG_CLASS_COLUMN)
    @ValueReference(ClassColumnRef.class)
    @ValueProvider(AutoGuessClassColumnProvider.class)
    String m_classColumn;

    @Widget(title = "Number of neighbors to consider (k)",
        description = """
            The number of nearest neighbors used to classify a new instance. An odd number is recommended to avoid ties.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidation = MaxKValidation.class)
    @Persist(configKey = KnnSettings2.CFG_K)
    int m_k = 3;

    @Widget(title = "Weight neighbors by distance",
        description = """
            If enabled, the distance of each neighbor to the query pattern influences its weight
            in the classification. Closer neighbors have greater influence on the result.
            Note: Only k neighbors are considered, regardless of weighting.""")
    @Persist(configKey = KnnSettings2.CFG_WEIGHT_BY_DISTANCE)
    boolean m_weightByDistance;

    @Widget(title = "Output class probabilities",
        description = """
            If enabled, additional columns containing the class probabilities for each predicted class
            will be appended to the output.""")
    @Persist(configKey = KnnSettings2.CFG_OUTPUT_CLASS_PROBABILITIES)
    @Migrate(loadDefaultIfAbsent = true) // added in 2.6
    boolean m_outputClassProbabilities;

    static final class ClassColumnRef implements ParameterReference<String> {
    }

    @SuppressWarnings("restriction")
    static final class AutoGuessClassColumnProvider extends ColumnNameAutoGuessValueProvider {

        AutoGuessClassColumnProvider() {
            super(ClassColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            if (ArrayUtils.contains(parametersInput.getInPortSpecs(), null)) {
                return Optional.empty(); // only guess when fully connected
            }
            // last column is usually the class column
            return ColumnSelectionUtil.getCompatibleColumns(parametersInput, 0, NominalValue.class).stream()
                .reduce((first, second) -> second);
        }
    }
}
