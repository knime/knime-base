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
package org.knime.base.node.io.filehandling.webui.reader2;

import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Parameters for handling multiple files in a reader.
 *
 * @author Paul Bärnreuther
 * @since 5.10
 */
public final class MultiFileReaderParameters extends AppendFilePathColumnParameters {

    static class HowToCombineColumnsOptionRef implements ParameterReference<HowToCombineColumnsOption> {
    }

    /**
     * Reference this interface to position parameters relative to "How to combine columns".
     */
    @Before(AppendFilePathColumnParameters.AppendFilePathColumn.class)
    public interface HowToCombineColumns {
    }

    /**
     * Public for tests.
     */
    @Widget(title = "How to combine columns",
        description = "Specifies how to deal with reading multiple files in which not all column names are identical.")
    @ValueSwitchWidget
    @ValueReference(HowToCombineColumnsOptionRef.class)
    @Layout(HowToCombineColumns.class)
    public HowToCombineColumnsOption m_howToCombineColumns = HowToCombineColumnsOption.FAIL;

    /**
     * Save the settings to the given config.
     *
     * @param config the config to save to
     */
    @Override
    @SuppressWarnings("deprecation")
    public void saveToConfig(final AbstractMultiTableReadConfig<?, ?, ?, ?> config) {
        super.saveToConfig(config);
        config.setCheckSavedTableSpec(true); // the option to ignore saved table spec is deprecated

        config.setFailOnDifferingSpecs(m_howToCombineColumns == HowToCombineColumnsOption.FAIL);
        config.setSpecMergeMode(m_howToCombineColumns == HowToCombineColumnsOption.INTERSECTION
            ? SpecMergeMode.INTERSECTION : SpecMergeMode.UNION);
    }

    /**
     * Options for combining columns from multiple files.
     */
    public enum HowToCombineColumnsOption {
            /**
             * Fail if different column names are found.
             */
            @Label(value = "Fail if different", description = "The node will fail if multiple files are read and not "
                + "all files have the same column names.")
            FAIL(ColumnFilterMode.UNION),

            /**
             * Use the union of all columns.
             */
            @Label(value = "Union", description = """
                    Any column that is part of any input file is considered. If a file is missing a column, it is filled
                    up with missing values.
                    """)
            UNION(ColumnFilterMode.UNION),

            /**
             * Use the intersection of all columns.
             */
            @Label(value = "Intersection",
                description = "Only columns that appear in all files are considered for the output table.")
            INTERSECTION(ColumnFilterMode.INTERSECTION);

        private final ColumnFilterMode m_columnFilterMode;

        HowToCombineColumnsOption(final ColumnFilterMode columnFilterMode) {
            m_columnFilterMode = columnFilterMode;
        }

        /**
         * @return the column filter mode corresponding to this option
         */
        public ColumnFilterMode toColumnFilterMode() {
            return m_columnFilterMode;
        }
    }
}
