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
            <p>
                Rounds numeric values to the specified decimal place or significant
                figures, applying the specified rounding method. The columns
                containing the numeric values can be specified. The rounded values
                can be appended as additional columns, or the old values are
                replaced by the rounded values. If rounded values are appended as
                additional columns, a column suffix for the columns to append needs
                to be specified.<br />
                To round the values seven different rounding modes are available:
                <ul>
                    <li>away from zero (UP)</li>
                    <li>towards zero (DOWN)</li>
                    <li>to larger (CEILING)</li>
                    <li>to smaller (FLOOR)</li>
                    <li>.5 away from zero (HALF_UP)</li>
                    <li>.5 towards zero (HALF_DOWN)</li>
                    <li>.5 to even digit (HALF_EVEN)</li>
                </ul>
                For the detailed description of each rounding mode please see the <a href="%s">Java documentation</a>.
            </p>
            <p>
                The output formatting can be of different types. By default, the "Auto" option will set the output
                column type based on the input column type. The other options are described in the example below.
                Rounding the numbers 1.23501, 0.00000035239 and -3.123103E9 to 3 significant digits (.5 away from zero)
                will produce:
            </p>
            <table>
              <tr>
                <th>Input</th>
                <th>Auto</th>
                <th>Double</th>
                <th>Standard String</th>
                <th>Plain String</th>
                <th>Engineering String</th>
              </tr>
              <tr>
                <td>1.23501</td>
                <td>1.24</td>
                <td>1.24</td>
                <td>"1.24"</td>
                <td>"1.24"</td>
                <td>"1.24"</td>
              </tr>
              <tr>
                <td>0.00000035239</td>
                <td>0.000000352</td>
                <td>0.000000352</td>
                <td>"3.52E-7"</td>
                <td>"0.000000352"</td>
                <td>"352E-9"</td>
              </tr>
              <tr>
                <td>-3123103001</td>
                <td>-3120000000</td>
                <td>-3120000000</td>
                <td>"-3.12E+9"</td>
                <td>"-3120000000"</td>
                <td>"-3.12E+9"</td>
              </tr>
            </table>
            <p>
                Note that the "Double" output option may yield unexpected results due to numerical precision
                issue when representing floating point numbers. For example a number such as 0.1 can sometimes
                be represented as 0.09999999999999999.
            </p>
            <p>
                Also note that automatically converting "Number (long)" input columns to "Number (integer)"
                output columns might lead to inaccurate results due to potential integer overflows.
            </p>
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
