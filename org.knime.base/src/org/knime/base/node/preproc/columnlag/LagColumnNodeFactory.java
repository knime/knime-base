/*
 * ------------------------------------------------------------------------
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
 * Created on Mar 17, 2013 by wiswedel
 */
package org.knime.base.node.preproc.columnlag;

import org.knime.base.node.preproc.column.renamer.ColumnRenamerSettings;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 *
 * @author wiswedel
 */
@SuppressWarnings("restriction")
public final class LagColumnNodeFactory extends WebUINodeFactory<LagColumnNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("Lag Column")//
        .icon("./lag_column.png")//
        .shortDescription("Copies column values from preceding rows into the current row.")//
        .fullDescription(
            """
                     Copies column values from preceding rows into the current row. The node can be used to
                     <ol>
                       <li>make a copy of the selected column and shift the cells <i>I</i> steps up (<i>I</i> = lag interval)</li>
                       <li>make <i>L</i> copies of the selected column and shift the cells of each copy
                         1, 2, 3, ... <i>L</i>-1 steps up (<i>L</i> = lag = number of copies)
                       </li>
                     </ol>
                    <p>
                      The option "Number of copies" (<i>L</i>) in this node is useful for time series prediction. If the rows are sorted in time
                      increasing order, to apply a lag <i>D</i> to the selected column means to place <i>D</i>-1 past values of the column
                      and the current value of the column on one row. The data table can then be used for time series prediction.
                    </p>
                    <p>
                      The lag interval option <i>I</i> (periodicity or seasonality) in this node is useful to compare values
                      from the past to the current values. Again if the rows are sorted in time increasing order, to apply a
                      lag interval <i>I</i> means to set aside on the same row the current value and the value
                      occurring <i>I</i> steps before.
                    </p>
                    <p>
                      <i>L</i> and <i>I</i> can be combined to obtain <i>L</i>-1 copies of the selected column,
                      each one shifted <i>I</i>, 2*<i>I</i>, 3*<i>I</i>, ... (<i>L</i>-1)*<i>I</i> steps backwards.
                    </p>""")//
        .modelSettingsClass(ColumnRenamerSettings.class)//
        .addInputTable("Input", "Input data")//
        .addOutputTable("Output", "Input data with additional columns copying the values from preceding rows.")//
        .keywords("Copy column", "Shift", "Offset", "Previous")//
        .build();

    @SuppressWarnings("javadoc")
    public LagColumnNodeFactory() {
        super(CONFIG);
    }

    @Override
    public LagColumnNodeModel createNodeModel() {
        return new LagColumnNodeModel(CONFIG);
    }

}
