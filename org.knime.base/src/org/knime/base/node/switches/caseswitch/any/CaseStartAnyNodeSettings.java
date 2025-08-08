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
package org.knime.base.node.switches.caseswitch.any;

import java.util.List;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

/**
 * Node settings for the Case Switch Start (Any Type) node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class CaseStartAnyNodeSettings implements NodeParameters {

    /**
     * Settings section for port selection.
     */
    @Section(title = "Port Selection")
    interface PortSelectionSection {
    }


    /**
     * Choices provider for output port selection.
     * Note: In a real implementation, this would be dynamically populated based on the actual output ports.
     */
    static final class OutputPortChoicesProvider implements StringChoicesProvider {

        @Override
        public List<String> choices(final NodeParametersInput context) {
            // This would ideally be populated dynamically based on the number of output ports
            // For now, provide a reasonable default range
            return StringChoicesProvider.super.choices(context);
            //new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        }
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        public List<String> choices(final NodeParametersOutput context) {
//            // TODO Auto-generated method stub
//            var numPorts = context.getgetOutPortObjects().length;
//            return IntStream.range(0, numPorts)
//                .mapToObj(Integer::toString)
//                .col    lect(Collectors.toList());
//            //return StringChoicesProvider.super.choices(context);
//        }
    }

    @Layout(PortSelectionSection.class)
    @Widget(title = "Active port",
            description = "Select which output port should be active and receive the input data. "
                        + "All other output ports will be inactive.")
    @ChoicesProvider(OutputPortChoicesProvider.class)
    @Persist(configKey = "PortIndex")
    String selectedPort = "0";


    @Widget(title = "Activate all outputs during configuration",
            description = "When enabled, all output ports are activated during the configuration step, "
                        + "allowing downstream nodes to be configured even if they are connected to inactive branches. "
                        + "This is useful during workflow design but should be disabled for production workflows.")
    @Persist(configKey = "activate_all_outputs_during_configure")
    boolean activateAllOutputsDuringConfigure = true;

}
