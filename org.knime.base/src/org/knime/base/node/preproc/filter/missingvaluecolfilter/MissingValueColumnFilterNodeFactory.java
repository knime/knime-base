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
package org.knime.base.node.preproc.filter.missingvaluecolfilter;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The factory for the missing value column filter node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @since 2.6
 */
@SuppressWarnings("restriction")
public class MissingValueColumnFilterNodeFactory
        extends WebUINodeFactory<MissingValueColumnFilterNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder() //
        .name("Missing Value Column Filter") //
        .icon("missingvalcolfilter.png") //
        .shortDescription("""
                The node removes selected columns from the input if they meet the specified threshold of missing values.
                """) //
        .fullDescription("""
                The node tests each selected column for missing values and removes it if it meets the specified
                threshold of absolute or relative number of missing values.
                Each column that is not selected for the test is passed on to the output unconditionally.
                    """) //
        .modelSettingsClass(MissingValueColumnFilterNodeSettings.class) //
        .nodeType(NodeType.Manipulator) //
        .addInputTable("Table to be filtered", "Table from which columns are filtered.") //
        .addOutputTable("Filtered table", """
                Table without selected columns matching the criterion regarding missing values.
                    """) //
        // version based on the @since tag of the original implementation of this factory class
        .sinceVersion(2, 6, 0) //
        .build();

    /**
     * Creates a new instance with the configuration.
     */
    public MissingValueColumnFilterNodeFactory() {
        super(CONFIG);
    }

    @Override
    public MissingValueColumnFilterNodeModel createNodeModel() {
        return new MissingValueColumnFilterNodeModel(CONFIG);
    }

}
