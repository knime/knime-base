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
 */
package org.knime.base.node.io.filehandling.webui.reader2.tutorial;

import org.knime.base.node.io.filehandling.webui.reader2.ReaderLayout;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Section;

/**
 * TODO (#6): Adjust or delete this class based on your layout needs.
 *
 * Uses {@link ReaderLayout} as root layout. Define additional layouts and their position here.
 *
 * Convention: Interfaces that serve as @Layout for parameters to be added to a ReaderLayout section should be nested
 * inside an interface named the same as the ReaderLayout section (e.g., DataArea). This is not required but improves
 * readability.
 *
 * @author KNIME AG, Zurich, Switzerland
 */
interface TutorialReaderLayoutAdditions {

    // TODO (#6): Example - Add a custom section between File and DataArea. Adjust or remove as needed.
    @Section(title = "File Format")
    @After(ReaderLayout.File.class)
    @Before(ReaderLayout.DataArea.class)
    interface FileFormat {
        // Add layout elements for file format settings here
    }

    // TODO (#6): This interface groups layouts for parameters added to the ReaderLayout.DataArea section
    interface DataArea {
        // Example: Add "First row contains column names" before SkipFirstDataRows
        @Before(ReaderLayout.DataArea.SkipFirstDataRows.class)
        interface FirstRowContainsColumnNames {
        }

        // Example: Add "If row has less columns" after UseExistingRowId
        @After(ReaderLayout.DataArea.UseExistingRowId.class)
        interface IfRowHasLessColumns {
        }
    }

}
