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
 *   Oct 24, 2024 (david): created
 */
package org.knime.time.node.manipulate.modifydate;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The node dialog of the node which modifies date.
 *
 * @author David Hickey, TNG Technology Consulting
 */
@SuppressWarnings("restriction")
public final class ModifyDateNodeFactory2 extends WebUINodeFactory<ModifyDateNodeModel2> {

    @Override
    public ModifyDateNodeModel2 createNodeModel() {
        return new ModifyDateNodeModel2(CONFIGURATION);
    }

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Date Modifier") //
        .icon("modify_date.png") //
        .shortDescription("Modifies date information of a date&amp;time cell.") //
        .fullDescription("""
                Modifies date&amp;time columns by appending, changing, or removing the date component. \
                You can append a date to time columns (output: Date&amp;time (Local) or Date&amp;time (Zoned)), \
                change the date in local or zoned date&amp;time columns, or remove the date (output: Time).<br/> \
                Useful for normalizing date columns or extracting time components for time-based analysis. \
                <br/><i>Note: This node does not perform shifting operations. Use the Date Shifter node for that \
                purpose.</i>
                """) //
        .modelSettingsClass(ModifyDateNodeSettings.class) //
        .addInputTable("Input table", "Input table containing date&amp;time columns.") //
        .addOutputTable("Output table", "Output table with modified date.") //
        .keywords("modify", "date", "time", "fields", "year", "month", "day") //
        .build();

    public ModifyDateNodeFactory2() {
        super(CONFIGURATION);
    }
}
