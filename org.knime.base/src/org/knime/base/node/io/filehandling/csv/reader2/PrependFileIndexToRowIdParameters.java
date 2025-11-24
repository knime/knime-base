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
 *   Nov 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;

/**
 * Parameters for prepending file index to RowID.
 *
 * @author Paul Bärnreuther
 */
public final class PrependFileIndexToRowIdParameters implements NodeParameters {

    @Widget(title = "Prepend file index to RowID", description = """
            Select this box if you want to prepend a prefix that depends on the index of the source file to the
            RowIDs. The prefix for the first file is "File_0_", for the second "File_1_" and so on. This option is
            useful if the RowIDs within a single file are unique but the same RowIDs appear in multiple files.
            Prepending the file index prevents parallel reading of individual files.
            """)
    @Layout(CSVTableReaderLayoutAdditions.MulitpleFileHandling.PrependFileIndexToRowId.class)
    boolean m_prependFileIndexToRowId;
    // TODO NOSONAR this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

    /**
     * Save the settings to the given config.
     *
     * @param tableReadConfig the config to save to
     */
    public void saveToConfig(final DefaultTableReadConfig<?> tableReadConfig) {
        tableReadConfig.setPrependSourceIdxToRowId(m_prependFileIndexToRowId);
    }
}
