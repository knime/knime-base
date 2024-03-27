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
 *   Jan 24, 2024 (kai): created
 */
package org.knime.base.node.preproc.rounddouble;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * Node factory for the 'Number Rounder' node
 *
 * @author Kai Franze, KNIME GmbH, Germany
 * @since 5.3
 */
@SuppressWarnings("restriction")
public class RoundDoubleNodeFactory extends WebUINodeFactory<RoundDoubleNodeModel> {

    private static final String DESC_SHORT = "Rounds numeric values supporting different rounding and output modes";

    private static final String JAVA_DOC_ROUNDING_MODE =
        "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/RoundingMode.html";

    private static final String DESC_FULL = """
            Rounds numeric values to the specified decimal place or significant figures, applying a rounding method to
            selected columns containing numeric values. The rounded values can be appended as additional columns, or
            replace the values in the original column. You can choose one of the following rounding methods:
            <ul>
                <li>.5 to even digit (HALF_EVEN)</li>
                <li>.5 away from zero (HALF_UP)</li>
                <li>.5 towards zero (HALF_DOWN)</li>
                <li>Away from zero (UP)</li>
                <li>Towards zero (DOWN)</li>
                <li>To larger (CEILING)</li>
                <li>To smaller (FLOOR)</li>
            </ul>
            For the detailed description of each rounding method please see the settings description in the node
            dialog or the <a href="%s">Java documentation</a>.
            """.formatted(JAVA_DOC_ROUNDING_MODE);

    private static final String DESC_INPUT_TABLE = "The input table containing numeric values to round.";

    private static final String DESC_OUTPUT_TABLE = "The output table containing the rounded values.";

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder() //
        .name("Number Rounder") //
        .icon("doublerounding.png") //
        .shortDescription(DESC_SHORT) //
        .fullDescription(DESC_FULL) //
        .modelSettingsClass(RoundDoubleNodeSettings.class)
        .addInputTable("Input table", DESC_INPUT_TABLE) //
        .addOutputTable("Output table", DESC_OUTPUT_TABLE) //
        .build();

    /**
     * Instantiates the 'Number Rounder' node
     */
    public RoundDoubleNodeFactory() {
        super(CONFIG);
    }

    @Override
    public RoundDoubleNodeModel createNodeModel() {
        return new RoundDoubleNodeModel(CONFIG);
    }

}
