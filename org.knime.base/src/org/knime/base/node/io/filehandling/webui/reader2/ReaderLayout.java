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
package org.knime.base.node.io.filehandling.webui.reader2;

import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters.UseNewSchema;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;

/**
 * Layout definition for reader dialogs. Descriptions are defined in the individual parameter classes.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public interface ReaderLayout {

    /**
     * The first section in the dialog containing the file selection.
     */
    @Section(title = "File")
    interface File {
        interface Source {
        }
    }

    /**
     * For limiting the data.
     */
    @Section(title = "Data Area")
    @After(File.class)
    interface DataArea {
        interface SkipFirstDataRows {
        }

        @After(SkipFirstDataRows.class)
        interface LimitNumberOfRows {
        }

        @After(LimitNumberOfRows.class)
        interface UseExistingRowId {
        }
    }

    @Section(title = "Column and Data Type Detection")
    @Advanced
    @After(DataArea.class)
    interface ColumnAndDataTypeDetection {
        interface IfSchemaChanges {
        }
    }

    /**
     * For configuring multiple files
     */
    @Section(title = "Multiple File Handling")
    @Advanced
    @After(ColumnAndDataTypeDetection.class)
    interface MultipleFileHandling {
        interface HowToCombineColumns {
        }

        @After(HowToCombineColumns.class)
        interface AppendFilePathColumn {
        }
    }

    /**
     * For adjusting inclusion, naming and types of the read columns and any unknown columns.
     */
    @Section(title = "Table Transformation", description = Transformation.DESCRIPTION)
    @Advanced
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
