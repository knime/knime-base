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
package org.knime.time.node.convert.stringtodatetime;

import org.knime.core.webui.node.dialog.defaultdialog.widget.ComprehensiveDateTimeFormatProvider;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The node factory of the node which converts a date&time column to a string column.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public final class StringToDateTimeNodeFactory2 extends WebUINodeFactory<StringToDateTimeNodeModel2> {

    @Override
    public StringToDateTimeNodeModel2 createNodeModel() {
        return new StringToDateTimeNodeModel2(CONFIGURATION);
    }

    static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("String to Date&Time") //
        .icon("stringtotime.png") //
        .shortDescription("Parses date and/or time strings into Date&amp;Time cells.") //
        .fullDescription("""
                <p>
                    Parses the strings in the selected columns and converts them into \
                    Date&amp;Time cells. The expected format can be selected from a number \
                    of commonly used formats or specified manually. See the <a href="%s"> \
                    Java documentation</a> for more details about possible formats.
                </p>
                <p>
                    Since dates may contain localized terms such as month or weekday
                    names, you can additionally specify a locale.
                </p>
                """.formatted(ComprehensiveDateTimeFormatProvider.LINK_TO_FORMAT_JAVADOC)) //
        .modelSettingsClass(StringToDateTimeNodeSettings.class) //
        .addInputTable("Input table", "Input table.") //
        .addOutputTable("Output table", "Output table containing the parsed columns.") //
        .keywords("covert", "date-time", "string", "local", "parse", "hour", "minute", "second", "millisecond", "year",
            "month", "day", "week") //
        .build();

    /**
     *
     */
    public StringToDateTimeNodeFactory2() {
        super(CONFIGURATION);
    }

}
