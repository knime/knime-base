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
 *   Dec 8, 2025 (paulbaernreuther): created
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Inside;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

/**
 * Parameters for appending a file path column to the read table.
 *
 * @author Paul Bärnreuther
 * @since 5.10
 */
public class AppendFilePathColumnParameters implements NodeParameters {

    /**
     * Reference this interface to position parameters relative to "Append file path column".
     */
    @Inside(ReaderLayout.MultipleFileHandling.class)
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

    static final class AppendPathColumnDefaultProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return "File Path";
        }
    }

    /**
     * Default constructor with empty path column.
     */
    public AppendFilePathColumnParameters() {
        // Default constructor
    }

    /**
     * Constructor with column name.
     *
     * @param columnName the column name to append, or null for empty
     */
    public AppendFilePathColumnParameters(final String columnName) {
        m_appendPathColumn = Optional.ofNullable(columnName);
    }

    /**
     * Save the settings to the given config.
     *
     * @param config the config to save to
     */
    public void saveToConfig(final AbstractMultiTableReadConfig<?, ?, ?, ?> config) {
        config.setAppendItemIdentifierColumn(m_appendPathColumn.isPresent());
        config.setItemIdentifierColumnName(m_appendPathColumn.orElse(""));
    }

    /**
     * Get the append path column value.
     *
     * @return the optional column name
     */
    public Optional<String> getAppendPathColumn() {
        return m_appendPathColumn;
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

}
