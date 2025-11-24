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

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Inside;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

/**
 * Parameters for handling multiple files in a reader.
 *
 * @author Paul Bärnreuther
 * @since 5.10
 */
@Inside(ReaderLayout.MultipleFileHandling.class)
public final class MultiFileReaderParameters implements NodeParameters {

    static class HowToCombineColumnsOptionRef implements ParameterReference<HowToCombineColumnsOption> {
    }

    /**
     * Reference this interface to position parameters relative to "How to combine columns".
     */
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

    static final class AppendPathColumnDefaultProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return "File Path";
        }
    }

    /**
     * Reference this interface to position parameters relative to "Append file path column".
     */
    @After(HowToCombineColumns.class)
    public interface AppendFilePathColumn {
    }

    private static final String APPEND_FILE_PATH_COLUMN_DESCRIPTION = """
            Select this box if you want to add a column containing the path of the file from which the row is read.
            The node will fail if adding the column with the provided name causes a name collision with any of the
            columns in the read table.
            """;

    @Widget(title = "Append file path column", description = APPEND_FILE_PATH_COLUMN_DESCRIPTION)
    @Layout(AppendFilePathColumn.class)
    @OptionalWidget(defaultProvider = AppendPathColumnDefaultProvider.class)
    @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
    Optional<String> m_appendPathColumn = Optional.empty();

    /**
     * Save the settings to the given config.
     *
     * @param config the config to save to
     */
    @SuppressWarnings("deprecation")
    public void saveToConfig(final AbstractMultiTableReadConfig<?, ?, ?, ?> config) {
        config.setCheckSavedTableSpec(true); // the option to ignore saved table spec is deprecated

        config.setFailOnDifferingSpecs(m_howToCombineColumns == HowToCombineColumnsOption.FAIL);
        config.setSpecMergeMode(m_howToCombineColumns == HowToCombineColumnsOption.INTERSECTION
            ? SpecMergeMode.INTERSECTION : SpecMergeMode.UNION);

        config.setAppendItemIdentifierColumn(m_appendPathColumn.isPresent());
        config.setItemIdentifierColumnName(m_appendPathColumn.orElse(""));
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_appendPathColumn.isPresent()) {
            final var columnName = m_appendPathColumn.get();
            ColumnNameValidationUtils.validateColumnName(columnName, invalidState -> { // NOSONAR complexity seems OK
                switch (invalidState) {
                    case EMPTY:
                        return "The file path column name must not be empty.";
                    case BLANK:
                        return "The file path column name must not be blank.";
                    case NOT_TRIMMED:
                        return "The file path column name must not start or end with whitespace.";
                    default:
                        throw new IllegalStateException("Unknown invalid column name state: " + invalidState);
                }
            });
        }
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
