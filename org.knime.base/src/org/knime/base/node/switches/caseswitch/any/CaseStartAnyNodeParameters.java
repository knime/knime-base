/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com\; Email: contact@knime.com
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
 *  along with this program; if not, see <http://www.gnu.org/licenses\>.
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

package org.knime.base.node.switches.caseswitch.any;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.core.node.port.PortType;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;

/**
 * Node parameters for CASE Switch Start.
 *
 * @author Kai Franze, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
final class CaseStartAnyNodeParameters implements NodeParameters {

    @TextMessage(PortTypeNameMessageProvider.class)
    Void m_portTypeNameMessage;

    static final class PortTypeNameMessageProvider implements StateProvider<Optional<TextMessage.Message>> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();

        }

        @Override
        public Optional<TextMessage.Message> computeState(final NodeParametersInput context) {
            return Arrays.stream(context.getInPortTypes()).findFirst() //
                .map(PortType::getName) //
                .map(name -> new TextMessage.Message("Port type name", name, MessageType.INFO));
        }
    }

    @Widget(title = "Select active port", description = """
            Select the active output port. This can be controlled by an integer or string flow variable \
            and used to create a string flow variable.
            """)
    @ChoicesProvider(OutputPortChoicesProvider.class)
    @Persist(configKey = "PortIndex")
    String m_selectedPort = "0";

    static final class OutputPortChoicesProvider implements StringChoicesProvider {
        @Override
        public List<String> choices(final NodeParametersInput context) {
            return IntStream.range(0, context.getOutPortTypes().length).mapToObj(Integer::toString).toList();
        }
    }

    @Widget(title = "Activate all outputs during configuration step", description = """
            When set the node will keep all outputs active during workflow configuration (that is, while the \
            traffic light of the node is "yellow"). This allows the configuration of connected downstream \
            nodes and simplifies the workflow design at the expense of additional configuration calls of \
            nodes that are later on inactive. It's recommended to switch this flag off for production \
            workflows as this avoids unnecessary configuration calls of the inactive branches.
            """)
    @Persist(configKey = "activate_all_outputs_during_configure")
    boolean m_activateAllOutputsDuringConfigure = true;

}
