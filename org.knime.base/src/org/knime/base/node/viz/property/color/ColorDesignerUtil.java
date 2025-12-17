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
 *   8 Dec 2025 (robin): created
 */
package org.knime.base.node.viz.property.color;

import java.util.List;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModel;

/**
 * Common functionality of the Color Gradient Designer, Color Palette Designer, and Color Designer (Apply).
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
final class ColorDesignerUtil {

    private ColorDesignerUtil() {
        // utility class
    }

    record OutputSpecification(DataTableSpec dataSpec, DataTableSpec modelSpec, String portSummary) {
    }

    static DataTableSpec createOutputModelSpec(final ColorHandler colorHandler, final String columnName) {
        final var modelSpecCreator = new DataTableSpecCreator();
        final var columnSpecCreator = new DataColumnSpecCreator(columnName, StringCell.TYPE);
        columnSpecCreator.setColorHandler(colorHandler);
        final var outputColumnSpec = columnSpecCreator.createSpec();
        modelSpecCreator.addColumns(outputColumnSpec);
        return modelSpecCreator.createSpec();
    }

    static DataTableSpec createOutputTableSpec(final DataTableSpec spec, final List<DataColumnSpec> selectedColumnSpecs,
        final ColorHandler colorHandler) {
        final var tableSpecCreator = new DataTableSpecCreator(spec);
        selectedColumnSpecs.stream().forEach(columnSpec -> {
            final var columnSpecCreator = new DataColumnSpecCreator(columnSpec);
            columnSpecCreator.setColorHandler(colorHandler);

            final var colIndex = spec.findColumnIndex(columnSpec.getName());
            final var outputColumnSpec = columnSpecCreator.createSpec();
            tableSpecCreator.replaceColumn(colIndex, outputColumnSpec);
        });
        return tableSpecCreator.createSpec();
    }

    static OutputSpecification createOutputSpecs(final DataTableSpec spec,
        final List<DataColumnSpec> selectedColumnSpecs, final ColorModel colorModel, final boolean applyToColumnNames) {
        final var colorHandler = new ColorHandler(colorModel);
        final var outputModelSpec = createOutputModelSpec(colorHandler, "Color handler");
        final var outputTableSpec = createOutputTableSpec(spec, selectedColumnSpecs, colorHandler);
        final var portSummary = createPortSummary(selectedColumnSpecs, applyToColumnNames);
        return new OutputSpecification(outputTableSpec, outputModelSpec, portSummary);
    }

    private static final int MAX_COLUMNS_IN_SUMMARY = 3;

    static String createPortSummary(final List<DataColumnSpec> columnSpecs, final boolean applyToColumnNames) {
        final var summaryValues = Stream.concat( //
            applyToColumnNames ? Stream.of("column names") : Stream.empty(),
            columnSpecs.stream().map(colSpec -> "\"" + colSpec.getName() + "\"")).toList();
        final int numValues = summaryValues.size();
        if (numValues == 0) {
            throw new IllegalStateException(
                "At least one column or applying color to column names is required to create a port summary.");
        }

        if (numValues <= MAX_COLUMNS_IN_SUMMARY) {
            return "Coloring on " + joinOxford(summaryValues);
        }

        final var shown = String.join(", ", summaryValues.subList(0, MAX_COLUMNS_IN_SUMMARY));
        final int remaining = numValues - MAX_COLUMNS_IN_SUMMARY;
        return "Coloring on " + shown + ", and " + remaining + " more column" + (remaining > 1 ? "s" : "");
    }

    private static String joinOxford(final List<String> items) {
        return switch (items.size()) {
            case 0 -> "";
            case 1 -> items.get(0);
            case 2 -> items.get(0) + " and " + items.get(1);
            default -> String.join(", ", items.subList(0, items.size() - 1)) + ", and " + items.get(items.size() - 1);
        };
    }

}
