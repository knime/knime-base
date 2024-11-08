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
 *   Sep 17, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.webui.reader;

import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.BaseAdvancedSettings.UseNewSchema;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"restriction", "java:S1214", "java:S103"})
public interface CommonReaderLayout {

    /**
     * The first section in the dialog containing the file selection.
     */
    @Section(title = "File")
    interface File {
        interface Source {
            // TODO NOSONAR will be updated in UIEXT-1764
            String DESCRIPTION = """
                    Select a file location which stores the data you want to read. When clicking on the browse button,
                    there are two default file system options to choose from:
                    <br/>
                    <ul>
                        <li><b>The current Hub space</b>: Allows to select a file relative to the Hub space on which the
                            workflow is run.</li>
                        <li><b>URL</b>: Allows to specify a URL (e.g. file://, http:// or knime:// protocol).</li>
                    </ul>
                    """;
        }
    }

    /**
     * For limiting the data.
     */
    @Section(title = "Data Area")
    @After(File.class)
    interface DataArea {
        interface SkipFirstDataRows {
            String DESCRIPTION =
                """
                        Use this option to skip the specified number of valid data rows. This has no effect on which row will be
                        chosen as a column header. Skipping rows prevents parallel reading of individual files.
                        """;
        }

        @After(SkipFirstDataRows.class)
        interface LimitNumberOfRows {
            String DESCRIPTION =
                """
                        If enabled, only the specified number of data rows are read. The column header row (if selected) is not
                        taken into account. Limiting rows prevents parallel reading of individual files.
                        """;
        }

        @After(LimitNumberOfRows.class)
        interface MaximumNumberOfRows {
            String DESCRIPTION = "Defines the maximum number of rows that are read.";
        }

        @After(MaximumNumberOfRows.class)
        interface UseExistingRowId {
            String DESCRIPTION = """
                    Check this box if the RowIDs from the input tables should be used for
                    the output tables. If unchecked, a new RowID is generated.
                    The generated RowID follows the schema "Row0", "Row1" and so on.
                                """;
        }
    }

    @SuppressWarnings("javadoc")
    @Section(title = "Column and Data Type Detection", advanced = true)
    @After(DataArea.class)
    interface ColumnAndDataTypeDetection {
        interface IfSchemaChanges {
            String DESCRIPTION = """
                    Specifies the node behavior if the content of the configured file/folder changes between executions,
                    i.e., columns are added/removed to/from the file(s) or their types change. The following options are
                    available:
                    """;

            String DESCRIPTION_FAIL =
                """
                        If set, the node fails if the column names in the file have changed. Changes in column types will not be
                        detected.
                        """;

            String DESCRIPTION_USE_NEW_SCHEMA =
                """
                        If set, the node will compute a new table specification for the current schema of the file at the time
                        when the node is executed. Note that the node will not output a table specification before execution and
                        that it will not apply transformations, therefore the transformation tab is disabled.
                        """;

            String DESCRIPTION_IGNORE =
                """
                        If set, the node tries to ignore the changes and outputs a table with the old table specification. This
                        option is deprecated and should never be selected for new workflows, as it may lead to invalid data in
                        the resulting table. Use one of the other options instead.
                        """;
        }
    }

    /**
     * For configuring multiple files
     */
    @Section(title = "Multiple File Handling", advanced = true)
    @After(ColumnAndDataTypeDetection.class)
    interface MultipleFileHandling {
        interface HowToCombineColumns {
            String DESCRIPTION =
                "Specifies how to deal with reading multiple files in which not all column names are identical.";

            String DESCRIPTION_FAIL =
                "The node will fail if multiple files are read and not all files have the same column names.";

            String DESCRIPTION_UNION = """
                    Any column that is part of any input file is considered. If a file is missing a column, it is filled
                    up with missing values.
                    """;

            String DESCRIPTION_INTERSECTION =
                "Only columns that appear in all files are considered for the output table.";
        }

        @After(HowToCombineColumns.class)
        interface AppendFilePathColumn {
            String DESCRIPTION =
                """
                        Select this box if you want to add a column containing the path of the file from which the row is read.
                        The node will fail if adding the column with the provided name causes a name collision with any of the
                        columns in the read table.
                        """;
        }

        @After(AppendFilePathColumn.class)
        interface FilePathColumnName {
            String DESCRIPTION = "The name of the column containing the file path.";
        }
    }

    /**
     * For adjusting inclusion, naming and types of the read columns and any unknown columns.
     */
    @Section(title = "Table Transformation", description = Transformation.DESCRIPTION, advanced = true)
    @Effect(predicate = UseNewSchema.class, type = EffectType.HIDE)
    @After(MultipleFileHandling.class)
    interface Transformation {
        String DESCRIPTION =
            """
                    Use this option to modify the structure of the table. You can deselect each column to filter it out of the
                    output table, use the arrows to reorder the columns, or change the column name or column type of each
                    column. Note that the positions of columns are reset in the dialog if a new file or folder is selected.
                    Whether and where to add unknown columns during execution is specified via the special row &lt;any unknown
                    new column&gt;. It is also possible to select the type new columns should be converted to. Note that the
                    node will fail if this conversion is not possible e.g. if the selected type is Integer but the new column is
                    of type Double.
                    """;

        interface EnforceTypes {
            @SuppressWarnings("hiding")
            String DESCRIPTION = """
                    Controls how columns whose type changes are dealt with.
                    If selected, the mapping to the KNIME type you configured is attempted.
                    The node will fail if that is not possible.
                    If unselected, the KNIME type corresponding to the new type is used.
                    """;
        }
    }

}
