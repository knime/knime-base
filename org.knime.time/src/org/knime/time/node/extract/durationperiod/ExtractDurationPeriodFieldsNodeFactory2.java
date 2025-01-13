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
 *   Nov 20, 2024 (david): created
 */
package org.knime.time.node.extract.durationperiod;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class ExtractDurationPeriodFieldsNodeFactory2
    extends WebUINodeFactory<ExtractDurationPeriodFieldsNodeModel2> {

    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Duration Part Extractor (Labs)") //
        .icon("extractduration.png") //
        .shortDescription("Extracts duration fields from duration and period cells") //
        .fullDescription("""
                <p>
                Extracts the selected fields from a duration column and appends the values as \
                long columns. Valid fields are hours, minutes, seconds, milli- micro- and nanoseconds \
                for a time-based duration and years, months and days for a date-based duration. \
                </p>
                <p>
                Note that for a time-based duration, some subsecond parts can have both a 'part' and an 'all' \
                variant. The 'all' variant includes all parts of the duration that are smaller than one \
                second, while the 'part' variant only includes the part that is selected. For example:
                </p>
                <ul>
                    <li><strong>Nanos (all subseconds)</strong>: Nanoseconds of a duration, \
                    including all of the subseconds. In other words, a duration of 10.123456789 seconds \
                    would have 123456789 nanoseconds.</li>
                    <li><strong>Nanos</strong>: The nanoseconds component of a duration. In other words, \
                    a duration of 10.123456789 seconds would have 789 nanoseconds.</li>
                </ul>
                """) //
        .modelSettingsClass(ExtractDurationPeriodFieldsNodeSettings.class) //
        .addInputTable("Input table", "Input table.") //
        .addOutputTable("Output table", "Output table containing the extracted fields as appended columns.") //
        .keywords("extract", "duration", "period", "interval", "field", "part", "years", "months", "days", "hours",
            "minutes", "seconds", "milliseconds", "microseconds", "nanoseconds") //
        .build();

    public ExtractDurationPeriodFieldsNodeFactory2() {
        super(CONFIGURATION);
    }

    @Override
    public ExtractDurationPeriodFieldsNodeModel2 createNodeModel() {
        return new ExtractDurationPeriodFieldsNodeModel2(CONFIGURATION);
    }
}
