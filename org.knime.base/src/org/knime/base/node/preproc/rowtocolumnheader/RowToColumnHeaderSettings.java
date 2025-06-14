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
 *   Dec 9, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.rowtocolumnheader;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelIntegerPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Settings class for the Row to Column Header node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class RowToColumnHeaderSettings implements DefaultNodeSettings {

    @Persistor(HeaderRowIndexPersistor.class)
    @Widget(title = "Number of rows before the header",
        description = "Number of rows in the input table that precede the row that should be used as new column header")
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    int m_headerRowIndex;

    @Persistor(DiscardBeforePersistor.class)
    @Widget(title = "Discard rows before header row",
        description = "Whether rows before the row containing the new column header should be discarded. "
            + "Otherwise they are treated as additional output rows.")
    boolean m_discardBefore;

    @Persistor(DetectTypesPersistor.class)
    @Widget(title = "Detect types of resulting columns",
        description = "Whether type analysis should be applied to the output table. "
            + "For each column, the most specific of the four column types <i>double</i> "
            + "(64-bit floating-point number), <i>long</i> (64-bit integral number), <i>int</i> "
            + "(32-bit integral number) and <i>String</i> is determined and the column is converted to this type.")
    boolean m_detectTypes;

    static final class HeaderRowIndexPersistor extends SettingsModelIntegerPersistor {
        HeaderRowIndexPersistor() {
            super("headerRowIndex");
        }
    }

    static final class DiscardBeforePersistor extends SettingsModelBooleanPersistor {
        DiscardBeforePersistor() {
            super("discardBefore");
        }
    }

    static final class DetectTypesPersistor extends SettingsModelBooleanPersistor {
        DetectTypesPersistor() {
            super("detectTypes");
        }
    }
}
