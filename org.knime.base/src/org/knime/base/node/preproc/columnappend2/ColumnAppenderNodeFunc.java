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
 *   Nov 10, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.columnappend2;

import org.knime.base.node.preproc.columnappend2.ColumnAppender2NodeModel.RowKeyMode;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.func.NodeFunc;
import org.knime.core.node.func.NodeFuncApi;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;

/**
 * The append_columns NodeFunc.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class ColumnAppenderNodeFunc implements NodeFunc {

    @Override
    public void saveSettings(final NodeSettingsRO arguments, final PortObjectSpec[] inputSpecs,
        final NodeSettingsWO settings) throws InvalidSettingsException {
        var appenderSettings = new ColumnAppenderSettings();
        appenderSettings.m_rowIdMode = RowKeyMode.GENERATE;
        DefaultNodeSettings.saveSettings(ColumnAppenderSettings.class, appenderSettings, settings);
    }

    @Override
    public NodeFuncApi getApi() {
        return NodeFuncApi.builder("append_columns")//
            .withDescription("Appends the two input tables along the column dimension.")
            .withInputTable("left", "The left input table.")//
            .withInputTable("right", "The right input table.")//
            .withOutputTable("appended_table", "The table consisting of the columns from both input tables.")//
            .build();
    }

    @Override
    public String getNodeFactoryClassName() {
        return ColumnAppender2NodeFactory.class.getName();
    }

}
