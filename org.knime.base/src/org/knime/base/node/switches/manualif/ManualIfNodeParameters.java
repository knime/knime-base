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

package org.knime.base.node.switches.manualif;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;

/**
 * Node parameters for IF Switch.
 *
 * @author Kai Franze, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
final class ManualIfNodeParameters implements NodeParameters {

    enum PortChoice {
            @Label("Both")
            BOTH("both"),

            @Label("Top")
            TOP("top"),

            @Label("Bottom")
            BOTTOM("bottom");

        final String m_value;

        PortChoice(final String value) {
            m_value = value;
        }

        static PortChoice fromValue(final String value) {
            for (PortChoice choice : values()) {
                if (choice.m_value.equals(value)) {
                    return choice;
                }
            }
            return BOTH; // Default to BOTH for unknown values
        }
    }

    @Persistor(PortChoicePersistor.class)
    @Widget(title = "Select active port", description = "Select the active output port")
    @RadioButtonsWidget(horizontal = true)
    PortChoice m_portChoice = PortChoice.BOTH;

    @Widget(title = "Activate all outputs during configuration step", description = """
            When set the node will keep all outputs active during workflow configuration (that is, while the \
            traffic light of the node is 'yellow'). This allows the configuration of connected downstream \
            nodes and simplifies the workflow design at the expense of additional configuration calls of \
            nodes that are later on inactive. It's recommended to switch this flag off for production \
            workflows as this avoids unnecessary configuration calls of the inactive branches.\
            """)
    @Persist(configKey = ManualIfNodeModel.ACTIVATE_OUTPUT_CFG)
    boolean m_activateAllOutputsDuringConfigure = true;

    static final class PortChoicePersistor implements NodeParametersPersistor<PortChoice> {

        private static final String CFG_KEY = "PortChoice";

        @Override
        public PortChoice load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return PortChoice.fromValue(settings.getString(CFG_KEY, ""));
        }

        @Override
        public void save(final PortChoice choice, final NodeSettingsWO settings) {
            settings.addString(CFG_KEY, choice.m_value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_KEY}};
        }
    }

}
