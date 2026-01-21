/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 18, 2011 (wiswedel): created
 */
package org.knime.base.node.viz.property.color.extract;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModel;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange;
import org.knime.core.data.property.ColorModelRange2;
import org.knime.core.data.property.ColorModelRange2.SpecialColorType;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.ColorHandlerPortObject;

/**
 * NodeModel to Color Extractor.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class ColorExtractNodeModel extends NodeModel {

    /** Color Port Object in, Data Table out. */
    ColorExtractNodeModel() {
        super(new PortType[]{ColorHandlerPortObject.TYPE},
                new PortType[]{BufferedDataTable.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec colorSpec = (DataTableSpec)inSpecs[0];
        if (colorSpec == null) {
            return null;
        }
        try (final CloseableTable table = extractColorTable(colorSpec)) {
            return new DataTableSpec[] {table.getDataTableSpec()};
        }
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects,
            final ExecutionContext exec) throws Exception {
        ColorHandlerPortObject colorPO = (ColorHandlerPortObject)inObjects[0];
        DataTableSpec colorSpec = colorPO.getSpec();
        try (final CloseableTable table = extractColorTable(colorSpec)) {
            return new BufferedDataTable[]{exec.createBufferedDataTable(table, exec)};
        }
    }

    private CloseableTable extractColorTable(final DataTableSpec colorSpec)
        throws InvalidSettingsException {
        // first column has column handler (convention in ColorHandlerPO)
        ColorHandler clrHdl = colorSpec.getColumnSpec(0).getColorHandler();
        final ColorModel model = clrHdl.getColorModel();
        if (model.getClass() == ColorModelNominal.class) {
            ColorModelNominal nom = (ColorModelNominal) model;
            return extractColorTable(nom);
        } else if (model.getClass() == ColorModelRange.class) {
            ColorModelRange range = (ColorModelRange) model;
            return extractColorTable(range);
        } else if (model.getClass() == ColorModelRange2.class) {
            ColorModelRange2 range2 = (ColorModelRange2)model;
            return extractColorTable(range2);
        } else {
            throw new InvalidSettingsException("Unknown ColorModel class: "
                    + model.getClass());
        }
    }

    private CloseableTable extractColorTable(final ColorModelRange range) {
        DataTableSpec spec = createSpec(new DataColumnSpecCreator("value", DoubleCell.TYPE).createSpec());
        DataContainer cnt = new DataContainer(spec);
        RowKey[] keys = new RowKey[] {new RowKey("min"), new RowKey("max")};
        Color[] clrs = new Color[] {range.getMinColor(), range.getMaxColor()};
        double[] vals = new double[] {range.getMinValue(), range.getMaxValue()};
        for (int i = 0; i < 2; i++) {
            Color clr = clrs[i];
            cnt.addRowToTable(createDefaultRow(keys[i], clr, new DoubleCell(vals[i])));
        }
        cnt.close();
        return cnt.getCloseableTable();
    }

    private static CloseableTable extractColorTable(final ColorModelRange2 range2) {
        final var isPercentageBased = range2.isPercentageBased();
        final var spec = createSpec( //
            new DataColumnSpecCreator(isPercentageBased ? "value (%)" : "value", DoubleCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("value as string", StringCell.TYPE).createSpec());
        final var cnt = new DataContainer(spec);

        final var stopColors = range2.getStopColors();
        var stopValues = range2.getStopValues();
        if (stopValues.length == 0) {
            final var numStops = stopColors.length;
            stopValues = IntStream.range(0, stopColors.length).mapToDouble(v -> (v * 100.0) / (numStops - 1)).toArray();
        }

        final var stringColumnFormat = isPercentageBased ? "%s%%" : "%s";
        var rowCounter = 0;
        for (; rowCounter < stopValues.length; rowCounter++) {
            final var color = stopColors[rowCounter];
            final var value = stopValues[rowCounter];
            cnt.addRowToTable(createDefaultRow(RowKey.createRowKey(rowCounter), color, new DoubleCell(value),
                new StringCell(String.format(stringColumnFormat, value))));
        }

        final var specialColorsCIELab = range2.getSpecialColors();
        addSpecialColorRows(rowCounter, specialColorsCIELab, cnt);

        cnt.close();
        return cnt.getCloseableTable();
    }

    private static void addSpecialColorRows(int rowCounter, final Map<SpecialColorType, Color> specialColors,
        final DataContainer cnt) {
        for (var entry : specialColors.entrySet()) {
            final var color = entry.getValue();
            final var specialType = entry.getKey();
            final var values = switch (specialType) {
                case NAN -> new DataCell[]{new DoubleCell(Double.NaN), new StringCell("not a number")};
                case POSITIVE_INFINITY -> //
                        new DataCell[]{new DoubleCell(Double.POSITIVE_INFINITY), new StringCell("positive infinity")};
                case NEGATIVE_INFINITY -> //
                        new DataCell[]{new DoubleCell(Double.NEGATIVE_INFINITY), new StringCell("negative infinity")};
                case BELOW_MIN -> new DataCell[]{DataType.getMissingCell(), new StringCell("below minimum")};
                case ABOVE_MAX -> new DataCell[]{DataType.getMissingCell(), new StringCell("above maximum")};
                case MISSING -> new DataCell[]{DataType.getMissingCell(), new StringCell("missing")};
            };
            cnt.addRowToTable(createDefaultRow(RowKey.createRowKey(rowCounter), color, values));
            rowCounter++;
        }
    }

    private CloseableTable extractColorTable(final ColorModelNominal nom)
    throws InvalidSettingsException {
        DataType superType = null;
        for (DataCell c : nom.getValues()) {
            if (superType == null) {
                superType = c.getType();
            } else {
                superType = DataType.getCommonSuperType(superType, c.getType());
            }
        }
        if (superType == null) {
            throw new InvalidSettingsException("No nominal values in model");
        }
        DataTableSpec spec = createSpec(new DataColumnSpecCreator("value", superType).createSpec());
        DataContainer cnt = new DataContainer(spec);
        long counter = 0L;
        for (DataCell c : nom.getValues()) {
            Color clr = nom.getColorAttr(c).getColor();
            cnt.addRowToTable(createDefaultRow(RowKey.createRowKey(counter++), clr, c));
        }
        cnt.close();
        return cnt.getCloseableTable();
    }

    private static DefaultRow createDefaultRow(final RowKey rowKey, final Color color, final DataCell... values) {
        final var colorColumns = Stream.of( //
            new IntCell(color.getRed()), //
            new IntCell(color.getGreen()), //
            new IntCell(color.getBlue()), //
            new IntCell(color.getAlpha()), //
            new IntCell(color.getRGB()), //
            new StringCell(ColorModel.colorToHexString(color)));
        final var allColumn = Stream.concat(Arrays.stream(values), colorColumns).toArray(DataCell[]::new);
        return new DefaultRow(rowKey, allColumn);
    }

    private static final DataColumnSpec[] COLOR_COLUMNS = new DataColumnSpec[]{ //
        new DataColumnSpecCreator("R", IntCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("G", IntCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("B", IntCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("A", IntCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("RGBA", IntCell.TYPE).createSpec(), //
        new DataColumnSpecCreator("RGB (Hex)", StringCell.TYPE).createSpec()};

    private static DataTableSpec createSpec(final DataColumnSpec... valueColumns) {
        return new DataTableSpec(
            Stream.concat(Arrays.stream(valueColumns), Arrays.stream(COLOR_COLUMNS)).toArray(DataColumnSpec[]::new));
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // no settings
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // no settings
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // no settings
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals
    }

}
