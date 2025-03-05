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
 *   Nov 26, 2024 (david): created
 */
package org.knime.time.node.convert.stringtodurationperiod;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * Node factory for the String to Duration/Period node, WebUI version.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class StringToDurationPeriodNodeFactory2 extends WebUINodeFactory<StringToDurationPeriodNodeModel2> {

    @Override
    public StringToDurationPeriodNodeModel2 createNodeModel() {
        return new StringToDurationPeriodNodeModel2(CONFIGURATION);
    }

    private static final String DURATION_PARSE_URL =
        "https://docs.oracle.com/javase/17/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-";

    private static final String PERIOD_PARSE_URL =
        "https://docs.oracle.com/javase/17/docs/api/java/time/Period.html#parse-java.lang.CharSequence-";

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("String to Duration") //
        .icon("stringtoduration.png") //
        .shortDescription("Converts a string to a duration/period.") //
        .fullDescription("""
                Converts string cells to duration/period cells. The string representation of these \
                durations needs to be in either the ISO 8601 representation (see  <a href="%s"> \
                date-based duration format</a> or <a href="%s">time-based duration format</a> \
                for details, e.g. 'P2Y3M1D'), the short letter representation (e.g. '2y 3M 1d', \
                where y means years, M means months, and d means days; or '2H 3m 1s', where \
                H means hours, m means minutes, and s means seconds) or the long word \
                representation (e.g. '2 years 3 months 1 day').
                """.formatted(PERIOD_PARSE_URL, DURATION_PARSE_URL)) //
        .modelSettingsClass(StringToDurationPeriodNodeSettings.class) //
        .addInputTable("Input table", "Input table.") //
        .addOutputTable("Output table", "Output table with strings parsed to durations/periods.") //
        .keywords("parse", "convert", "string", "duration", "period", "interval", "hour", "minute", "second",
            "millisecond", "year", "month", "week", "day") //
        .build();

    public StringToDurationPeriodNodeFactory2() {
        super(CONFIGURATION);
    }

}
