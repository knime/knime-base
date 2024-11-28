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
 *   Nov 28, 2024 (tobias): created
 */
package org.knime.time.node.convert.datetimetostring;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The node factory of the node which converts date and time columns to strings.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class DateTimeToStringNodeFactory2 extends WebUINodeFactory<DateTimeToStringNodeModel2> {

    @Override
    public DateTimeToStringNodeModel2 createNodeModel() {
        return new DateTimeToStringNodeModel2(CONFIGURATION);
    }

    static final String DATE_TIME_FORMATTER_URL =
        "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/format/DateTimeFormatter.html";

    static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Date&Time to String") //
        .icon("timetostring.png") //
        .shortDescription("Converts Date&amp;Time cells into cells holding strings.") //
        .fullDescription("""
                Converts the time values in Date&amp;Time columns into strings
                using a user-provided format pattern as defined by
                <a href="%s">DateTimeFormatter</a>
                .
                """.formatted(DATE_TIME_FORMATTER_URL)) //
        .modelSettingsClass(DateTimeToStringNodeSettings.class) //
        .addInputTable("Input table", "Input table.") //
        .addOutputTable("Output table", "Output table containing columns converted to a string.") //
        .keywords("covert", "date-time", "string", "local", "format", "hour", "minute", "second", "millisecond", //
            "year", "month", "week", "day")//
        .build();

    /**
     *
     */
    public DateTimeToStringNodeFactory2() {
        super(CONFIGURATION);
    }

}
