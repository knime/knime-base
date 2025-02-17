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
 *   Feb 3, 2025 (david): created
 */
package org.knime.base.node.io.variablecreator;

import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * Web UI Node Factory for the Variable Creator Node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class VariableCreatorNodeFactory extends WebUINodeFactory<VariableCreatorNodeModel> {

    /**
     *
     */
    public VariableCreatorNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public VariableCreatorNodeModel createNodeModel() {
        return new VariableCreatorNodeModel(CONFIGURATION);
    }

    static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Variable Creator") //
        .icon("variable_creator.png") //
        .shortDescription("Creates flow variables for use in the workflow.") //
        .fullDescription("""
                <p>
                    This node allows the creation of flow variables of different types and custom values.
                    <br />
                    <i>Notes:</i>
                    <ul>
                        <li>
                            The variables defined by this node take precedence over upstream ones (on the
                            Variable Inport). Thus upstream variables will get overridden by defined
                            variables with the same name and type. The node will notify you in the case
                            this happens.
                        </li>
                        <li>
                            It is possible to define variables with the same names but different types
                            as already defined upstream variables. However, keep in mind that this will
                            probably lead to unexpected behavior downstream and should thus be avoided.
                        </li>
                        <li>
                            The node will automatically add a variable called &#8220;variable_1&#8221;
                            when it is initially created, even before you configure it.
                        </li>
                    </ul>
                </p>
                <p>
                    The possible data types for the created variables are:
                    <ul>
                        <li>
                            <b>String</b>: A string of characters. This is the default when a new variable \
                            is created. The default value is an empty string.
                        </li>
                        <li>
                            <b>Integer</b>: An integer number with possible values from 2&#179;&#185;-1 \
                            to -2&#179;&#185;. The value must be a valid number (consisting only of an \
                            optional sign (&#8220;+&#8221;/&#8220;-&#8221;) or \
                            &#8220;0&#8221;-&#8220;9&#8221;) and be in the range above. If the size of your \
                            value exceeds the limits above, you can try to use a <i>Long</i> or <i>Double</i> \
                            value instead. The default value is &#8220;0&#8221;.
                        </li>
                        <li>
                            <b>Long</b>: An integer number with possible values from 2&#8310;&#170;-1 \
                            to -2&#8310;&#170;. The value must be a valid number (consisting only of an \
                            optional sign (&#8220;+&#8221;/&#8220;-&#8221;) or \
                            &#8220;0&#8221;-&#8220;9&#8221;) and be in the range above. The default value \
                            is &#8220;0&#8221;.
                        </li>
                        <li>
                            <b>Double</b>: A floating point decimal number with possible values from around \
                            4.9&#183;10&#8315;&#179;&#178;&#8308; to 1.8&#183;10&#179;&#8304;&#8312; \
                            in both the positive and negative range. The value must be a valid number \
                            (consisting only of an optional sign (&#8220;+&#8221;/&#8220;-&#8221;) or \
                            &#8220;0&#8221;-&#8220;9&#8221;). You can specify an exponent by appending \
                            &#8220;e&#8221; followed by the exponent. Apart from a numeric value you can \
                            also specify one of the following three (case-sensitive) special values: \
                            <ul>
                                <li><i>Infinity</i> for positive infinity</li>
                                <li><i>-Infinity</i> for negative infinity</li>
                                <li><i>NaN</i> for &#8220;Not a Number&#8221;</li>
                            </ul>
                            If the number is too big or too small, it may be converted into one of these \
                            special values. (You will be warned if this happens). You should keep in mind \
                            that you may lose some precision for big values or values that are very close \
                            to zero. The default value is &#8220;0.0&#8221;.
                        </li>
                        <li>
                            <b>Boolean</b>: A boolean value, either &#8220;true&#8221; or \
                            &#8220;false&#8221;.  The default value is &#8220;false&#8221;.  Any value that \
                            is not equal (ignoring case) to 'true' will be treated as false.
                        </li>
                    </ul>
                </p>
                """) //
        .modelSettingsClass(VariableCreatorNodeSettings.class) //
        .addOutputPort("Created flow variables", FlowVariablePortObject.TYPE, "The created flow variables")
        .keywords("flow", "create") //
        .build();
}
