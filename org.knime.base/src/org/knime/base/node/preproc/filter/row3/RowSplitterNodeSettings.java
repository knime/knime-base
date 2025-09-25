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
 *   8 May 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3;

import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;

/**
 * Settings for the Row Splitter node based on the webui.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class RowSplitterNodeSettings extends AbstractRowFilterNodeSettings {

    RowSplitterNodeSettings() {
        super();
    }

    RowSplitterNodeSettings(final NodeParametersInput input) {
        super(input);
    }

    @Override
    boolean isSecondOutputActive() {
        return true;
    }

    @Override
    FilterMode outputMode() {
        return switch (m_outputMode) {
            case MATCHING -> FilterMode.MATCHING;
            case NON_MATCHING -> FilterMode.NON_MATCHING;
        };
    }

    @Widget(title = "Splitting behavior",
        description = "Determines whether matching rows are output at the first port and non-matching rows are "
            + "output at the second port, or vice-versa.")
    @RadioButtonsWidget
    @Layout(DialogSections.Output.OutputMode.class)
    SplitterMode m_outputMode = SplitterMode.MATCHING;

    // variants are named exactly as in `FilterMode` to make it easier to switch from Filter to Splitter when using
    // flow variables
    private enum SplitterMode {
            @Label("Matching rows at first output, non-matching at second output")
            MATCHING, @Label("Non-matching rows at first output, matching at second output")
            NON_MATCHING
    }

}
