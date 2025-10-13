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
 *   Mar 6, 2025 (david): created
 */
package org.knime.base.node.preproc.filter.nominal;

import org.knime.base.node.preproc.filter.nominal.NominalValueRowCommonSettings.NominalValueRowSplitterNodeSettings;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * <code>WebUINodeFactory</code> for the "Nominal Value Row Splitter" Node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class NominalValueRowSplitterNodeFactory extends WebUINodeFactory<NominalValueRowSplitterNodeModel> {

    /**
     * Constructor for the node factory.
     */
    public NominalValueRowSplitterNodeFactory() {
        super(CONFIGURATION);
    }

    @Override
    public NominalValueRowSplitterNodeModel createNodeModel() {
        return new NominalValueRowSplitterNodeModel(CONFIGURATION);
    }

    private static final String DOMAIN_CALCULATOR_URL = "https://hub.knime.com/knime/extensions/"
        + "org.knime.features.base/latest/org.knime.base.node.preproc.domain.dialog2.DomainNodeFactory";

    private static final String DOMAIN_EDITOR_URL = "https://hub.knime.com/knime/extensions/"
        + "org.knime.features.base/latest/org.knime.base.node.preproc.domain.editnominal.EditNominalDomainNodeFactory";

    static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Category Row Splitter") //
        .icon("nominal_value_splitter.png") //
        .shortDescription("Splits rows on nominal attribute value") //
        .fullDescription("""
                <p>
                Splits the rows based on the selected value of a nominal attribute.
                A nominal column can be selected and one or more nominal values of this
                attribute. Rows which have this nominal value in the selected column are
                included in the output data in the first table, the rest in the second.
                </p>
                <p>
                In order for a nominal column to appear in the node dialog, its domain
                (the set of values that appear in the column) must be calculated. For
                columns with few values (less than 60) this is done automatically.
                To ensure the domain is properly set, use the
                <a href="%s">Domain Calculator</a> node or the
                <a href="%s">Category Domain Editor</a> node.
                </p>
                """.formatted(DOMAIN_CALCULATOR_URL, DOMAIN_EDITOR_URL)) //
        .modelSettingsClass(NominalValueRowSplitterNodeSettings.class) //
        .addInputTable("Input Table", "Data that should be split") //
        .addOutputTable("First Table", "Matching rows") //
        .addOutputTable("Second Table", "Non-matching rows") //
        .build();
}
