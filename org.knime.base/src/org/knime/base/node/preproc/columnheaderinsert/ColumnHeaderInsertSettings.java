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
 *   Jan 6, 2023 (Jonas Klotz, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.columnheaderinsert;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings of the Insert Column Header (Dictionary) Node. Only used for the Web UI dialog, please double-check
 * backwards compatible loading if this class is ever used in the NodeModel.
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class ColumnHeaderInsertSettings implements DefaultNodeSettings {

    // TODO: UIEXT-1007 migrate String to ColumnSelection

    @Persist(configKey = ColumnHeaderInsertConfig.CFG_LOOKUP_COLUMN)
    @Widget(title = "Lookup column",
        description = "The column in the 2nd input table containing the \"old\" names of the columns.")
    @ChoicesWidget(choices = StringColumnsSecondTable.class, showRowKeysColumn = true)
    String m_lookupColumn;

    @Persist(configKey = ColumnHeaderInsertConfig.CFG_VALUE_COLUMN)
    @Widget(title = "Names column",
        description = "The column in the 2nd input table containing the \"new\" names of the columns.")
    @ChoicesWidget(choices = StringColumnsSecondTable.class)
    String m_valueColumn;

    @Persist(configKey = ColumnHeaderInsertConfig.CFG_FAIL_IF_NO_MATCH)
    @Widget(title = "Fail if no assignment in dictionary table",
        description = "If selected, the node fails if there is no matching entry of a column name"
            + " in the dictionary table. Otherwise it will keep the original column name.")
    boolean m_failIfNoMatch;

    private static final class StringColumnsSecondTable implements ChoicesProvider {

        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            var spec = context.getDataTableSpecs()[1];
            if (spec == null) {
                return new String[0];
            } else {
                return spec.stream()//
                    .filter(c -> c.getType().isCompatible(StringValue.class))//
                    .map(DataColumnSpec::getName)//
                    .toArray(String[]::new);
            }
        }

    }
}
