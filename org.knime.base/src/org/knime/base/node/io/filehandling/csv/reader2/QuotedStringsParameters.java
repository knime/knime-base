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
 *   Nov 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.QuoteOption;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;

/**
 * Parameters for handling quoted strings.
 *
 * @author Paul Bärnreuther
 */
@Layout(ValuesSection.Quotes.class)
final class QuotedStringsParameters implements NodeParameters {

    static class ReplaceEmptyQuotedStringsByMissingValuesRef extends ReferenceStateProvider<Boolean> {
    }

    @Widget(title = "Replace empty quoted string by missing values",
        description = "Select this box if you want <b>quoted</b> empty strings to be replaced by missing value cells.")
    @ValueReference(ReplaceEmptyQuotedStringsByMissingValuesRef.class)
    boolean m_replaceEmptyQuotedStringsByMissingValues = true;

    /**
     * Options for handling quoted strings.
     */
    enum QuotedStringsOption {
            @Label(value = "Remove quotes and trim whitespace", description = "Quotes will be removed from the value "
                + "followed by trimming any leading/trailing whitespaces.") //
            REMOVE_QUOTES_AND_TRIM, //
            @Label(value = "Keep quotes",
                description = "Quotes of a value will be kept. Note: No trimming will be done inside the quotes.") //
            KEEP_QUOTES; //
    }

    static class QuotedStringsOptionRef extends ReferenceStateProvider<QuotedStringsOption> {
    }

    @Widget(title = "Quoted strings",
        description = "Specifies the behavior in case there are quoted strings in the input table.", advanced = true)
    @ValueReference(QuotedStringsOptionRef.class)
    @RadioButtonsWidget
    QuotedStringsOption m_quotedStringsOption = QuotedStringsOption.REMOVE_QUOTES_AND_TRIM;

    /**
     * Save the settings to the given config.
     *
     * @param csvConfig the config to save to
     */
    void saveToConfig(final CSVTableReaderConfig csvConfig) {
        csvConfig.setReplaceEmptyWithMissing(m_replaceEmptyQuotedStringsByMissingValues);

        csvConfig.setQuoteOption(m_quotedStringsOption == QuotedStringsOption.REMOVE_QUOTES_AND_TRIM
            ? QuoteOption.REMOVE_QUOTES_AND_TRIM : QuoteOption.KEEP_QUOTES);
    }
}
