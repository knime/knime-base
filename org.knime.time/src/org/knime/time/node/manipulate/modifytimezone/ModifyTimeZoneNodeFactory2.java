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
 *   Oct 29, 2024 (david): created
 */
package org.knime.time.node.manipulate.modifytimezone;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The node dialog of the node which modifies timezone.
 *
 * @author David Hickey, TNG Technology Consulting
 */
@SuppressWarnings("restriction")
public final class ModifyTimeZoneNodeFactory2 extends WebUINodeFactory<ModifyTimeZoneNodeModel2> {

    @Override
    public ModifyTimeZoneNodeModel2 createNodeModel() {
        return new ModifyTimeZoneNodeModel2(CONFIGURATION);
    }

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Time Zone Modifier") //
        .icon("modify_time_zone.png") //
        .shortDescription("Modifies a time zone.") //
        .fullDescription("""
                Modifies date&amp;time columns in three different ways:
                <ul>
                    <li>
                        Add a time zone to local date&amp;time columns or change the time zone in zoned \
                        date&amp;time columns <i>(without adjusting the time!)</i>.
                    </li>
                    <li>
                        Shift the time zone in zoned date time columns by \
                        adjusting the time (and potentially date) according to the \
                        new time zone offset.
                    </li>
                    <li>
                        Remove the time zone from zoned date&amp;time columns.
                    </li>
                </ul>
                """) //
        .modelSettingsClass(ModifyTimeZoneNodeSettings.class) //
        .addInputTable("Input table", "Input table.") //
        .addOutputTable("Output table", "Output table with modified timezone.") //
        .keywords("modify", "date-time", "date")//
        .build();

    public ModifyTimeZoneNodeFactory2() {
        super(CONFIGURATION);
    }
}
