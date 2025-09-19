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
package org.knime.base.node.preproc.columntrans2;

import org.knime.base.node.preproc.pmml.columntrans2.One2ManyCol2PMMLNodeModel;
import org.knime.core.data.NominalValue;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 * WebUI settings for the "One to Many" node.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@LoadDefaultsForAbsentFields
public final class One2ManyCol2NodeParameters implements NodeParameters {

    static final class NominalColumnsProvider extends CompatibleColumnsProvider {
        NominalColumnsProvider() {
            super(NominalValue.class);
        }
    }

    @SuppressWarnings("restriction")
    static final class ColumnsToTransform extends LegacyColumnFilterPersistor {
        ColumnsToTransform() {
            super(One2ManyCol2PMMLNodeModel.CFG_COLUMNS);
        }
    }

    @Widget(title = "Columns to transform", description = """
            Select the nominal columns that should be included in the transformation.
            For each included column extra columns are appended, one for each possible value.
            If no column name appears in the dialog but your input table contains nominal columns,
            you could use the Domain Calculator node and connect its output to this node.
            """)
    @ChoicesProvider(NominalColumnsProvider.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @TwinlistWidget(excludedLabel = "Available columns", includedLabel = "Columns to transform")
    @Persistor(ColumnsToTransform.class)
    ColumnFilter m_columnsToTransform = new ColumnFilter();

    @Widget(title = "Remove included columns from output",
        description = "When enabled, the original columns selected for transformation are removed from the output. "
            + "The included columns are replaced by the new generated columns.")
    @Persist(configKey = One2ManyCol2PMMLNodeModel.CFG_REMOVESOURCES)
    boolean m_removeSources;
}
