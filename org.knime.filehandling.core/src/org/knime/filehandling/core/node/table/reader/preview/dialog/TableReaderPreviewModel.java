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
 *   Aug 7, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.util.ViewUtils;

/**
 * The model of the table reader preview.</br>
 * It manages the table content as well as the analysis components (progress bar and warning/error labels)
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class TableReaderPreviewModel {

    private final TableContentModel m_previewTableModel = new TableContentModel();

    private final AnalysisComponentModel m_analysisComponentModel;

    private PreviewDataTable m_previewTable = null;

    /**
     * @param analysisComponent
     */
    public TableReaderPreviewModel(final AnalysisComponentModel analysisComponent) {
        m_analysisComponentModel = analysisComponent;
        m_analysisComponentModel.startUpdate()//
            .showProgressBar(false)//
            .showQuickScanButton(false)//
            .finishUpdate();
    }

    AnalysisComponentModel getAnalysisComponent() {
        return m_analysisComponentModel;
    }

    TableContentModel getPreviewTableModel() {
        return m_previewTableModel;
    }

    /**
     * Set a data table.
     *
     * @param previewTable the table to set
     */
    public void setDataTable(final PreviewDataTable previewTable) {
        final PreviewDataTable oldTable = m_previewTable;
        m_previewTable = previewTable;

        // register a listener for error messages
        // (because of lazy loading an error does not occur until the user scrolls down to the respective row)
        if (previewTable != null) {
            previewTable.addErrorListener((r, e) -> m_analysisComponentModel.setErrorLabel(e, r));
        }

        ViewUtils.invokeLaterInEDT(() -> {
            // set the new table in the view
            m_previewTableModel.setDataTable(previewTable);
            // properly dispose of the old table
            if (oldTable != null) {
                oldTable.close();
            }
        });
    }

    /**
     * Cancels potentially running threads and disposes of the allocated resources.
     */
    public void onClose() {
        if (m_previewTable != null) {
            m_previewTableModel.setDataTable(null);
            m_previewTable.close();
            m_previewTable = null;
        }
        m_analysisComponentModel.reset();
    }

    /**
     * Indicates whether a data table has been set.
     *
     * @return true if a data table is set
     */
    public boolean isDataTableSet() {
        return m_previewTable != null;
    }

}
