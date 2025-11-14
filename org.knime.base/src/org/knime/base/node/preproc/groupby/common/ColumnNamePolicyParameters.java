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
 *   Nov 14, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.groupby.common;

import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.base.node.preproc.groupby.Sections;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persistor;

@SuppressWarnings({"restriction", "javadoc"})
@LoadDefaultsForAbsentFields
public final class ColumnNamePolicyParameters implements NodeParameters {

    @Layout(Sections.Output.class)
    @Modification.WidgetReference(ColumnNamePolicyParameters.ColumnNamePolicyWidget.class)
    @Widget(title = "Column naming", description = """
            The name of the resulting aggregation column(s) depends on the
            selected naming schema.
            <ul>
                <li>Keep original name(s):
                Keeps the original column names.
                Note that you can use all aggregation columns only once with
                this column naming option to prevent duplicate column names.
                </li>
                <li>Aggregation method (column name):
                Uses the aggregation method first and appends the column name
                in brackets
                </li>
                <li>Column name (aggregation method):
                Uses the column name first and appends the aggregation method
                in brackets
                </li>
            </ul>
            All aggregation methods get a * appended if the missing value option
            is not ticked in the aggregation settings in order to distinguish
            between columns that considered missing values in the aggregation
            process and columns that do not.
            """)
    @Persistor(LegacyColumnNamePolicyPersistor.class)
    ColumnNamePolicy m_columnNamePolicy = ColumnNamePolicy.getDefault();

    interface ColumnNamePolicyWidget extends Modification.Reference {
    }

    /**
     * Use this modification to change the title of the ColumnNamePolicy widget.
     */
    public abstract static class ChangeColumnNamePolicyTitleModification implements Modification.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(ColumnNamePolicyParameters.ColumnNamePolicyWidget.class).modifyAnnotation(Widget.class)
                .withProperty("title", getColumnNamePolicyTitle()).modify();
        }

        /**
         * The new title for the ColumnNamePolicy widget.
         *
         * @return the new title
         */
        protected abstract String getColumnNamePolicyTitle();

    }
}
