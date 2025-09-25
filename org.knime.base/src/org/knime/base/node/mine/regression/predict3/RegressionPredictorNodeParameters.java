/*
 * ------------------------------------------------------------------------
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
package org.knime.base.node.mine.regression.predict3;

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

/**
 * Node parameters for Regression Predictor.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class RegressionPredictorNodeParameters implements NodeParameters {

    @Widget(title = "Custom prediction column name",
        description = "Allows you to specify a customized name for the prediction column "
            + "that is appended to the input table. If not checked, \"Prediction (target)\" "
            + "(where target is the name of the target column of the provided regression model) "
            + "is used as default.")
    @Persistor(CustomPredictionNamePersistor.class)
    @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
    Optional<String> m_customPredictionName = Optional.empty();

    static final class CustomPredictionNamePersistor implements NodeParametersPersistor<Optional<String>> {

        @Override
        public Optional<String> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var useCustomName =
                settings.getBoolean(RegressionPredictorSettings.CFG_HAS_CUSTOM_PREDICTION_NAME, false);
            if (useCustomName) {
                return Optional.ofNullable(settings.getString(RegressionPredictorSettings.CFG_CUSTOM_PREDICTION_NAME, ""));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public void save(final Optional<String> param, final NodeSettingsWO settings) {
            settings.addBoolean(RegressionPredictorSettings.CFG_HAS_CUSTOM_PREDICTION_NAME, param.isPresent());
            settings.addString(RegressionPredictorSettings.CFG_CUSTOM_PREDICTION_NAME, param.orElse(""));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{//
                {RegressionPredictorSettings.CFG_HAS_CUSTOM_PREDICTION_NAME}, //
                {RegressionPredictorSettings.CFG_CUSTOM_PREDICTION_NAME} //
            };
        }

    }
}
