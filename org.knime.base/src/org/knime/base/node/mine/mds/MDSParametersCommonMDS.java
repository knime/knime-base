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
 *   20 Jan 2026 (Robin Gerling, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.mine.mds;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Common MDS technique related parameters for MDS nodes.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @since 5.11
 */
public final class MDSParametersCommonMDS implements NodeParameters {

    @Persist(configKey = MDSConfigKeys.CFGKEY_EPOCHS)
    @Widget(title = "Epochs", description = """
            Specifies the number of epochs to train.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class, stepSize = 10)
    int m_epochs = MDSNodeModel.DEF_EPOCHS;

    @Persist(configKey = MDSConfigKeys.CFGKEY_OUTDIMS)
    @Widget(title = "Output dimensions", description = """
            Specifies the dimension of the mapped output data.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_outputDimensions = MDSNodeModel.DEF_OUTPUTDIMS;

    @Persist(configKey = MDSConfigKeys.CFGKEY_LEARNINGRATE)
    @Widget(title = "Learning rate", description = """
            Specifies the learning rate to use. The learning rate is decreased automatically
            over the trained epochs.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, maxValidation = IsMax1Validation.class,
        stepSize = 0.1)
    double m_learningRate = MDSNodeModel.DEF_LEARNINGRATE;

    @Persist(configKey = MDSConfigKeys.CFGKEY_SEED)
    @Widget(title = "Random seed", description = """
            Specifies the random seed to use, which allows to reproduce a mapping even if the
            initialization is done randomly.
            """)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, stepSize = 1000)
    int m_randomSeed = MDSManager.DEFAULT_SEED;

    private static final class IsMax1Validation extends NumberInputWidgetValidation.MaxValidation {
        @Override
        public double getMax() {
            return 1.0;
        }
    }

}
