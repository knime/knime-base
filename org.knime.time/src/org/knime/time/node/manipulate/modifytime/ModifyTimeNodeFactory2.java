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
 *   Oct 28, 2016 (simon): created
 */
package org.knime.time.node.manipulate.modifytime;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The node factory of the node which modifies time.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class ModifyTimeNodeFactory2 extends WebUINodeFactory<ModifyTimeNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ModifyTimeNodeModel createNodeModel() {
        return new ModifyTimeNodeModel();
    }

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Time Modifier") //
        .icon("modify_time.png") //
        .shortDescription("Modifies time information of a date&amp;time cell.") //
        .fullDescription("""
            Modifies date&amp;time columns in three different ways:
            <ul>
                <li>
                    Append a time to local date columns
                    (output type is local or zoned date time column).
                </li>
                <li>
                    Change the time in local or zoned date&amp;time columns.
                </li>
                <li>
                    Remove the time from local or zoned date&amp;time columns
                    (output type is local date).
                </li>
            </ul>
            <b>Note:</b> This node is not used to add ("plus") or subtract ("minus") time.
            Use the <i>Date&amp;Time Shift</i> node for that.
            """) //
        .modelSettingsClass(ModifyTimeNodeSettings.class) //
        .addInputTable("Input table", "Input table.") //
        .addOutputTable("Output table", "Output table with modified time.") //
        .keywords("modify", "date-time", "fields", "hour", "minute", "second", "milli")//
        .build();

    /**
     *
     */
    public ModifyTimeNodeFactory2() {
        super(CONFIGURATION);
    }

}
