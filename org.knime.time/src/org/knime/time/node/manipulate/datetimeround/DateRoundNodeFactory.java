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
 *   Nov 18, 2024 (tobias): created
 */
package org.knime.time.node.manipulate.datetimeround;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The node factory of the node which rounds dates.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class DateRoundNodeFactory extends WebUINodeFactory<DateRoundNodeModel> {

    @Override
    public DateRoundNodeModel createNodeModel() {
        return new DateRoundNodeModel(CONFIGURATION);
    }

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Date Rounder") //
        .icon("date-rounder.png") //
        .shortDescription("Rounds a date.") //
        .fullDescription("""
                The Date Rounder Node is designed for rounding date-based data within a table. \
                It offers a flexible interface to adjust and transform date column values based \
                on user-defined strategies and precision settings. This node is highly adaptable, \
                enabling users to replace or append transformed columns to the input table, \
                facilitating seamless integration into data workflows.
                """) //
        .modelSettingsClass(DateRoundNodeSettings.class) //
        .addInputTable("Input table", "Input table.") //
        .addOutputTable("Output table", "Output table with rounded dates.") //
        .keywords("modify", "ceil", "floor", "trunc", "fields", "hour", "minute", "second", "milli", "precision", "day",
            "weekday", "month", "quater", "decade", "week", "time")//
        .build();

    public DateRoundNodeFactory() {
        super(CONFIGURATION);
    }

}
