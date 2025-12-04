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

package org.knime.base.node.preproc.pmml.missingval.compute;

import java.util.Optional;

import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;

/**
 * Node parameters for Missing Value.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
class MissingValueHandlerNodeParameters implements NodeParameters {
    @Section(title = "Treatment by Column")
    interface ByColumnSection {

    }

    @Section(title = "Treatment by Data Type")
    @After(ByColumnSection.class)
    interface ByDataTypeSection {
    }

    @TextMessage(NoOrEmptyTableMessage.class)
    Void m_noOrEmptyTableMessage;

    @Layout(ByColumnSection.class)
    @Widget(title = "Treatment by Column", description = "Specify how to treat missing values for a specific columns.")
    @ArrayWidget(elementTitle = "Column Treatment", addButtonText = "Add Column Treatment")
    MissingValueColumnTreatment[] m_columnSettings = new MissingValueColumnTreatment[0];

    @Layout(ByDataTypeSection.class)
    MissingValueDataTypeParameters m_dataTypeSettings = new MissingValueDataTypeParameters();

    @SuppressWarnings("restriction")
    static final class NoOrEmptyTableMessage implements StateProvider<Optional<TextMessage.Message>> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var inTableSpec = parametersInput.getInTableSpec(0);
            if (inTableSpec.isEmpty()) {
                return Optional.of(new TextMessage.Message("No input table connected.",
                    "Please connect an input table to configure missing value handling.",
                    TextMessage.MessageType.WARNING));
            }
            if (inTableSpec.get().getNumColumns() == 0) {
                return Optional.of(new TextMessage.Message("Input table has no columns.",
                    "Please provide an input table with at least one column to configure missing value handling.",
                    TextMessage.MessageType.WARNING));
            }
            return Optional.empty();
        }
    }

}
