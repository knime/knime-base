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
 *   Nov 14, 2024 (david): created
 */
package org.knime.time.node.create.createdatetime;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The node factory of the node which creates date and time cells.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class CreateDateTimeNodeFactory2 extends WebUINodeFactory<CreateDateTimeNodeModel2> {

    /**
     * Creates a new factory.
     */
    public CreateDateTimeNodeFactory2() {
        super(CONFIGURATION);
    }

    @Override
    public CreateDateTimeNodeModel2 createNodeModel() {
        return new CreateDateTimeNodeModel2(CONFIGURATION);
    }

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Date&Time Range Creator") //
        .icon("create_date_time.png") //
        .shortDescription("Generates date&amp;time values.") //
        .fullDescription("""
                Generates date&amp;time values, i.e. either a date, a time, a date&amp;time \
                (local) or a zoned date&amp;time. There are three creation modes:
                <ul>
                    <li><b>Duration &amp; end</b>: a starting point, an ending point and an interval is \
                    selected (the number of rows corresponds to how often the interval fits between start and end).</li>
                    <li><b>Number &amp; end</b>: a number of rows, a starting point, and an ending point \
                    is selected (the steps in between will be calculated).</li>
                    <li><b>Number &amp; duration</b>: a number of rows, a starting point and an interval is \
                    selected (the interval defines the steps between each row).</li>
                </ul> \
                """) //
        .modelSettingsClass(CreateDateTimeNodeSettings.class) //
        .addOutputTable("Output table", "Output table with created date range.") //
        .keywords("create", "date", "date-time", "range", "period", "interval", "duration") //
        .build();

}
