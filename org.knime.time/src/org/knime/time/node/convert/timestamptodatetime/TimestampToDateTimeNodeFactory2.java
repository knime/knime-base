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
 *   Mar 28, 2025: Paul Bärnreuther
 */
package org.knime.time.node.convert.timestamptodatetime;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * The node factory of the node which converts timestamps to the new date&time types.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public final class TimestampToDateTimeNodeFactory2 extends WebUINodeFactory<TimestampToDateTimeNodeModel2> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("UNIX Timestamp to Date&Time")//
        .icon("./timestamptotime.png")//
        .shortDescription("Parses integer or long values into into date&amp;time cells.")//
        .fullDescription("Parses the integer or long values in the selected columns and converts them into "
            + "date&amp;time cells. The inputs should be unix timestamps either in seconds, milli-, micro- or "
            + "nanoseconds since the epoch (1.1.1970).<br/>"
            + "For the output you may choose between local and zoned date and time formats and if necessary add "
            + "the timezone.")//
        .modelSettingsClass(TimestampToDateTimeNodeSettings.class)//
        .addInputTable("Input table", "Input table.")//
        .addOutputTable("Output table", "Output table containing the parsed columns.")//
        .build();

    @SuppressWarnings("javadoc")
    public TimestampToDateTimeNodeFactory2() {
        super(CONFIG);
    }

    @Override
    public TimestampToDateTimeNodeModel2 createNodeModel() {
        return new TimestampToDateTimeNodeModel2(CONFIG);
    }

}
