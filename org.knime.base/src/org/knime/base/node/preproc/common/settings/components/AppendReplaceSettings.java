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
 *   17 Jan 2024 (jasper): created
 */
package org.knime.base.node.preproc.common.settings.components;

import java.util.Objects;

import org.knime.base.node.preproc.common.settings.SingleColumnOutputMode;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.LayoutGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 *
 * @author jasper
 */
public class AppendReplaceSettings implements DefaultNodeSettings, LayoutGroup {

    public AppendReplaceSettings() {
    }

    private AppendReplaceSettings(final SingleColumnOutputMode outputMode, final String defaultNewColName) {
        m_outputMode = outputMode;
        m_newColumnName = defaultNewColName;
    }

    public static AppendReplaceSettings withDefaults(final SingleColumnOutputMode outputMode,
        final String defaultNewColName) {
        Objects.requireNonNull(outputMode);
        return new AppendReplaceSettings(outputMode, defaultNewColName);
    }

    @Widget(title = "Output column",
        description = "Choose whether to append the output column or replace the input column.")
    @ValueSwitchWidget
    @Signal(id = SingleColumnOutputMode.IsReplace.class, condition = SingleColumnOutputMode.IsReplace.Condition.class)
    @Persist(optional = true)
    public SingleColumnOutputMode m_outputMode = SingleColumnOutputMode.DEFAULT;

    @Widget(title = "Output column name", description = "Choose a name for the output column")
    @TextInputWidget(minLength = 1)
    @Effect(signals = SingleColumnOutputMode.IsReplace.class, type = EffectType.HIDE)
    @Persist(optional = true)
    public String m_newColumnName = "Output";

}
