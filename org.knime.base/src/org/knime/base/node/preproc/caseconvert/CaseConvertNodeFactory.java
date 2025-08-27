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
 * --------------------------------------------------------------------------
 *
 * History
 *   19.06.2007 (cebron): created
 */
package org.knime.base.node.preproc.caseconvert;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * @author cebron, University of Konstanz
 * NodeFactory for the Case Converter Node. Modern Web-UI based factory.
 */
@SuppressWarnings({"restriction", "deprecation"})
public final class CaseConvertNodeFactory extends WebUINodeFactory<CaseConvertNodeModel> {

    /**
     *
     */
    public CaseConvertNodeFactory() {
        super(CONFIG);
    }

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Case Converter") //
        .icon("case_converter.png") //
        .shortDescription("Converts letters in selected string columns to upper- or lowercase.") //
        .fullDescription("""
                Converts the case of alphanumeric characters in the selected string-compatible columns.
                Select the columns and whether to convert to upper- or lowercase. Missing values are preserved.
                """
        )//
        .modelSettingsClass(CaseConvertNodeParameters.class) //
        .addInputPort("Input Table", BufferedDataTable.TYPE, "Table with string columns to convert.") //
        .addOutputPort("Transformed Table", BufferedDataTable.TYPE, "Table with converted columns.") //
        .nodeType(NodeType.Manipulator) //
        .keywords("Uppercase", "Lowercase", "Case", "String")//
        .sinceVersion(5, 8, 0)
        .build();

    /**
     * @since 5.8
     */
    @Override
    public CaseConvertNodeModel createNodeModel() {
        return new CaseConvertNodeModel();
    }

}
