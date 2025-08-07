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
 */
package org.knime.base.node.preproc.crossjoin;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Settings for the Cross Joiner node.
 *
 * @author Alexander Fillbrunn, University of Konstanz
 * @author Iris Adae, University of Konstanz
 */
@SuppressWarnings("restriction")
public final class CrossJoinerNodeSettings implements NodeParameters {

    @Section(title = "General Settings")
    interface GeneralSection {
    }
    
    @Section(title = "Row ID Settings")
    interface RowIdSection {
    }

    @Layout(GeneralSection.class)
    @Widget(title = "Bottom table's column name suffix", 
            description = "Suffix to append to duplicate column names from the bottom table")
    @TextInputWidget
    @Persist(configKey = "rigthSuffix")
    public String rightColumnNameSuffix = " (#1)";

    @Layout(GeneralSection.class)
    @Widget(title = "Separator for new RowIds", 
            description = "String used to separate the row IDs when creating new row IDs")
    @TextInputWidget
    @Persist(configKey = "CFG_SEPARATOR")
    public String rowKeySeparator = "_";

    @Layout(GeneralSection.class)
    @Widget(title = "Chunk size", 
            description = "Number of rows to process in each chunk to control memory usage")
    @NumberInputWidget(min = 1, max = Integer.MAX_VALUE)
    @Persist(configKey = "CFG_CACHE")
    public int cacheSize = 1;

    @Layout(RowIdSection.class)
    @Widget(title = "Append top data table's RowIds", 
            description = "If checked, the row IDs from the top input table will be appended as a new column")
    @Persist(configKey = "CFG_SHOW_FIRST")
    public boolean showFirstRowIds = false;

    @Layout(RowIdSection.class)
    @Widget(title = "Column name (top)", 
            description = "Name of the column containing the row IDs from the top table")
    @TextInputWidget
    @Effect(signals = "showFirstRowIds", type = EffectType.SHOW)
    @Persist(configKey = "CFG_FIRST_COLUMNNAME")
    public String firstRowIdsColumnName = "FirstRowIDs";

    @Layout(RowIdSection.class)
    @Widget(title = "Append bottom data table's RowIds", 
            description = "If checked, the row IDs from the bottom input table will be appended as a new column")
    @Persist(configKey = "CFG_SHOW_SECOND")
    public boolean showSecondRowIds = false;

    @Layout(RowIdSection.class)
    @Widget(title = "Column name (bottom)", 
            description = "Name of the column containing the row IDs from the bottom table")
    @TextInputWidget
    @Effect(signals = "showSecondRowIds", type = EffectType.SHOW)
    @Persist(configKey = "CFG_SECOND_COLUMNNAME")
    public String secondRowIdsColumnName = "SecondRowIDs";
}
