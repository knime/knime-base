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
 *   24 Jan 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row;

import org.knime.base.node.preproc.filter.row.RowFilterNodeSettings.FilterOperator;
import org.knime.base.node.preproc.filter.row.RowFilterNodeSettings.OutputMode;
import org.knime.base.node.preproc.filter.row.RowFilterNodeSettings.StringMatchingMode;
import org.knime.base.node.preproc.filter.row.rowfilter.AttrValueRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.MissingValueRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilterFactory;
import org.knime.base.node.preproc.filter.row.rowfilter.RowIDRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowNoRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.StringCompareRowFilter;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;

/**
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class LegacyRowFilterPersistor extends NodeSettingsPersistorWithConfigKey<RowFilterNodeSettings> {

    // TODO there seems to be some old thing we need to support: ColValFilterOldObsolete

    @Override
    public RowFilterNodeSettings load(final NodeSettingsRO nodeSettings) throws InvalidSettingsException {
        final var filter = RowFilterFactory.createRowFilter(nodeSettings.getNodeSettings(getConfigKey()));
        // TODO translate to settings
        final var settings = new RowFilterNodeSettings();
        if (filter instanceof AttrValueRowFilter avFilter) {
            final var colName = avFilter.getColName();
            // TODO get data type...
            settings.m_targetSelection = new ColumnSelection(colName, StringCell.TYPE);
            settings.m_outputMode = avFilter.getInclude() ? OutputMode.INCLUDE : OutputMode.EXCLUDE;
            settings.m_deepFiltering = avFilter.getDeepFiltering();

            if (avFilter instanceof StringCompareRowFilter scomp) {
                settings.m_operator = FilterOperator.EQ;

                settings.m_caseSensitive = scomp.getCaseSensitive();
                if (scomp.getHasWildcards()) {
                    settings.m_stringMatching = StringMatchingMode.WILDCARDS;
                } else if (scomp.getIsRegExpr()) {
                    settings.m_stringMatching = StringMatchingMode.REGEX;
                }
                final var pat = scomp.getPattern();
                settings.m_stringValue = pat;
            } else if (avFilter instanceof MissingValueRowFilter) {
                settings.m_operator = FilterOperator.IS_MISSING;
            }
        } else if (filter instanceof RowIDRowFilter) {
        } else if (filter instanceof RowNoRowFilter) {
        } else {
            // TODO are there any other concrete implementations that the Row Filter node produces?
            // NB: the RowFilterNodeDialogPane silently ignored unsupported filter implementations
        }

        return settings;
    }

    @Override
    public void save(final RowFilterNodeSettings obj, final NodeSettingsWO settings) {
        final IRowFilter iRowFilter = null; // TODO translate
        // iRowFilter.saveSettingsTo(settings.addNodeSettings(getConfigKey()));

        final var selectedColumn = obj.m_targetSelection;



    }

    @Override
    protected String getConfigKey() {
        return RowFilterNodeModel.CFGFILTER;
    }

}
