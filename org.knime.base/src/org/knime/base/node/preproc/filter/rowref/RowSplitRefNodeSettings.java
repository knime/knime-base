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
 *   Feb 5, 2025 (david): created
 */
package org.knime.base.node.preproc.filter.rowref;

import org.knime.base.node.preproc.filter.rowref.SettingsUtils.DataColumnChoices;
import org.knime.base.node.preproc.filter.rowref.SettingsUtils.DataColumnPersistor;
import org.knime.base.node.preproc.filter.rowref.SettingsUtils.ReferenceColumnChoices;
import org.knime.base.node.preproc.filter.rowref.SettingsUtils.ReferenceColumnPersistor;
import org.knime.base.node.preproc.filter.rowref.SettingsUtils.UpdateDomainsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.SpecialColumns;

/**
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class RowSplitRefNodeSettings implements DefaultNodeSettings {

    @Persistor(DataColumnPersistor.class)
    @Widget(title = "Data column (in first input)",
        description = "The column from the table to be split that should be used for comparison.")
    @ChoicesWidget(choices = DataColumnChoices.class, showRowKeysColumn = true)
    String m_dataColumn = SpecialColumns.ROWID.getId();

    @Persistor(ReferenceColumnPersistor.class)
    @Widget(title = "Reference column (in second input)",
        description = "The column from the filter table that should be used for comparison.")
    @ChoicesWidget(choices = ReferenceColumnChoices.class, showRowKeysColumn = true)
    String m_referenceColumn = SpecialColumns.ROWID.getId();

    @Widget(title = "Update domains of all columns", description = """
            Advanced setting to enable recomputation of the domains of all \
            columns in the output table such that the domains' bounds exactly \
            match the bounds of the data in the output table.
            """, advanced = true)
    @Migrate(loadDefaultIfAbsent = true)
    @Persistor(UpdateDomainsPersistor.class)
    boolean m_updateDomains;
}
