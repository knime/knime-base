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
 *   Oct 28, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.awt.Color;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.util.SharedIcons;

/**
 * Renderer for cells containing a {@link ProductionPath}.</br>
 * Represents a {@link ProductionPath} by its destination {@link DataType}.</br>
 * If {@code null} is provided (i.e. for the placeholder row for unknown columns) only the unknown icon is displayed.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
// This Sonar rule is impossible to satisfy when dealing with Swing
final class KnimeTypeFullProductionPathTableCellRenderer extends DefaultTableCellRenderer {//NOSONAR

    private static final long serialVersionUID = 1L;

    private static final Icon UNKNOWN = SharedIcons.TYPE_DEFAULT.get();

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
        final boolean hasFocus, final int row, final int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        final ProductionPathOrDataType prodPathOrDataType = (ProductionPathOrDataType)value;
        if (isSelected) {
            // use selection color from parent
        } else {
            setBackground(Color.WHITE);
        }
        if (prodPathOrDataType == null) {
            setText("");
            setIcon(UNKNOWN);
            return this;
        }
        if (prodPathOrDataType.hasProductionPath()) {
            displayProductionPath(prodPathOrDataType);
        } else if (prodPathOrDataType.hasDataType()) {
            displayDataType(prodPathOrDataType);
        } else {
            setText("Default");
            setIcon(UNKNOWN);
        }
        return this;
    }

    private void displayProductionPath(final ProductionPathOrDataType prodPathOrDataType) {
        final ProductionPath prodPath = prodPathOrDataType.getProductionPath();
        final JavaToDataCellConverterFactory<?> converterFactory = prodPath.getConverterFactory();
        final DataType knimeType = converterFactory.getDestinationType();
        final Class<?> sourceType = converterFactory.getSourceType();
        final String text;
        if (DataCell.class != sourceType ) {
            text = sourceType.getSimpleName() + " \u2192 " + knimeType.toPrettyString();
        } else {
            text = knimeType.toPrettyString();
        }
        setText(text);
        setIcon(knimeType.getIcon());
    }

    private void displayDataType(final ProductionPathOrDataType prodPathOrDataType) {
        final DataType knimeType = prodPathOrDataType.getDataType();
        setIcon(knimeType.getIcon());
        setText(knimeType.toPrettyString());
    }
}