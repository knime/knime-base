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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 19, 2023 (carlwitt): created
 */
package org.knime.base.node.flowvariable.credentialspropertiesextractor;

import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * Node that extracts non-confidential information from credentials flow variables.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public final class CredentialsPropertiesExtractorNodeFactory
    extends WebUINodeFactory<CredentialsPropertiesExtractorNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Credentials Properties Extractor")//
        .icon(null)//
        .shortDescription("""
                Extracts non-confidential information from credentials flow variables.
                """)//
        .fullDescription("""
                Extracts non-confidential information from credentials flow variables,
                for instance the user name and whether a non-empty password has been set.
                """)//
        .modelSettingsClass(CredentialsPropertyExtractorSettings.class)//
        .nodeType(NodeType.Other)//
        .addInputPort("Flow variable input", FlowVariablePortObject.TYPE, "Credentials flow variables.")
        .addOutputTable("Credentials properties",
            """
                    Lists properties for the selected credentials.
                    The output columns of the table have the following meaning.
                    "%s" enumerates credential identifiers, "%s" provides an identifier for the user,
                    "%s" indicates whether a non-empty password has been entered,
                    and "%s" denotes whether a non-empty second factor has been input.
                    """.formatted(CredentialsPropertiesExtractorNodeModel.NAME_COLUMN.getName(),
                CredentialsPropertiesExtractorNodeModel.LOGIN_COLUMN.getName(),
                CredentialsPropertiesExtractorNodeModel.PASSWORD_COLUMN.getName(),
                CredentialsPropertiesExtractorNodeModel.FACTOR_COLUMN.getName()))//
        .sinceVersion(5, 3, 0) //
        .keywords("credentials", "password", "login", "flow variable") //
        .build();

    /** Set the node description */
    public CredentialsPropertiesExtractorNodeFactory() {
        super(CONFIG);
    }

    @Override
    public CredentialsPropertiesExtractorNodeModel createNodeModel() {
        return new CredentialsPropertiesExtractorNodeModel(CONFIG, CredentialsPropertyExtractorSettings.class);
    }

}