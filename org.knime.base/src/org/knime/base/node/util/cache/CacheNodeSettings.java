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
 *   Aug 11, 2024 (wiswedel): created
 */
package org.knime.base.node.util.cache;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings for Cache node.
 *
 * @author Bernd Wiswedel
 * @since 5.4
 */
@SuppressWarnings("restriction")
final class CacheNodeSettings implements DefaultNodeSettings {

    enum CopyImplementation {
            @Label(value = "Automatic",
                description = "Determines the algorithm automatically based on the table backend "
                    + "used by the current workflow")
            AUTO, @Label(value = "Columnar (Full Row)", description = "Uses the Columnar Table API, best used when the "
                + "workflow configuration is set to use the Columnar Table Backend.")
            COLUMNAR_BY_ROW,
            @Label(value = "Columnar (Cell By Cell)", description = "Uses the new Columnar Table API, in addition"
                + "copies each value in a row individually.")
            COLUMNAR_BY_CELL,
            @Label(value = "Row-based (Full Row)",
                description = "Uses the new Row Table Backend, using the old row-based API, which "
                    + "is known to be inefficient when the Columnar Table is set on a workflow.")
            ROW_BASED_BY_ROW
    }

    enum ColumnDomains {
            @Label("Retain")
            RETAIN, @Label("Compute")
            COMPUTE
    }

    @Widget(title = "Column domains", description = """
            <p>Specify whether to take domains of all input columns as output domains as-is or compute them on the
            output rows.</p>

            <p>
            Depending on the use case, one or the other setting may be preferable:
            <ul>
                <li><em>Retaining</em> input columns can be useful, if the axis limits of a view should be derived from
                domain bounds, and that bounds should stay stable even when the displayed data is filtered.
                </li>
                <li><em>Computing</em> domains can be useful when a selection widget consumes the output and should only
                display actually present options to users.</li>
            </ul>
            </p>

            <p>
            If column domains are irrelevant for a particular use case, the &quot;Retain&quot; option should be used
            since it does not incur computation costs.
            </p>
            """)
    @ValueSwitchWidget
    @Migrate(loadDefaultIfAbsent = true)
    ColumnDomains m_domains = ColumnDomains.RETAIN;

    @Widget(title = "Copy Implementation",
        description = "Select the copy implementation to use when copying. "
            + "In most cases leave it as automatic unless you are running performance tests or similar. For backward "
            + "compatibility reasons, the value for existing nodes is kept as 'Row-based'.",
        advanced = true)
    @Migration(DefaultCopyImplementationProvider.class)
    CopyImplementation m_implementation = CopyImplementation.AUTO;

    /** provider for backward compatibility. */
    static final class DefaultCopyImplementationProvider implements DefaultProvider<CopyImplementation> {
        @Override
        public CopyImplementation getDefault() {
            return CopyImplementation.ROW_BASED_BY_ROW;
        }
    }

}
