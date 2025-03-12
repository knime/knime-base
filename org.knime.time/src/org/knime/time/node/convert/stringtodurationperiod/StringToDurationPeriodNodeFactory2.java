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

    private static final String BASE_URL = "https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/";

    private static final String DURATION_PARSE_URL = BASE_URL + "Duration.html#parse(java.lang.CharSequence)";

    private static final String PERIOD_PARSE_URL = BASE_URL + "Period.html#parse(java.lang.CharSequence)";

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("String to Duration") //
        .icon("stringtoduration.png") //
        .shortDescription("Converts a string to a duration.") //
        .fullDescription("""
                Converts string cells to duration cells. The input strings can follow the ISO 8601 format \
                (see  <a href="%s">date-based duration format</a> or <a href="%s">time-based duration format</a> \
                for details, e.g. <tt>P2Y3M1D</tt>), a short letter format (e.g., <tt>2y 3M 1d or 2H 3m 1s</tt>), \
                or a long word format (e.g., <tt>2 years 3 months 1 day</tt>).<br/>\
                Commonly applied when preparing data containing text-based durations for calculation or further \
                processing.
                """.formatted(PERIOD_PARSE_URL, DURATION_PARSE_URL)) //
        .modelSettingsClass(StringToDurationPeriodNodeSettings.class) //
        .addInputTable("Input table", "Input table containing string columns with duration information.") //
        .addOutputTable("Output table", "Output table with strings parsed to durations.") //
        .keywords("parse", "convert", "string", "duration", "period", "interval", "hour", "minute", "second",
            "millisecond", "year", "month", "week", "day") //
        .build();

    public StringToDurationPeriodNodeFactory2() {
        super(CONFIGURATION);
    }

}
