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
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.base.data.statistics.StatisticCalculator;
import org.knime.base.data.statistics.calculation.DoubleMinMax;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange;
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
import org.knime.core.node.util.CheckUtils;

/**
 * Model used to set colors either based on the nominal values or ranges
 * (bounds) retrieved from the {@link org.knime.core.data.DataColumnSpec}.
 * The created {@link org.knime.core.data.property.ColorHandler} is then
 * set in the column spec.
 *
 * @see ColorManager2NodeDialogPane
 *
 * @author Thomas Gabriel, University of Konstanz
 */
class ColorManager2NodeModel extends NodeModel {

    static final String COLUMNS_CONTAINED_IN_TABLE_IDENTIFIER = "<_COLUMNS-CONTAINED-IN-TABLE_>";

    static final DataColumnSpec COLUMN_NAMES_SPEC =
            new DataColumnSpecCreator(COLUMNS_CONTAINED_IN_TABLE_IDENTIFIER, StringCell.TYPE).createSpec();

    /** 'Paired' palette, contributed from http://colorbrewer2.org. */
    private static final String[] PALETTE_SET1 = {"#33a02c", "#e31a1c", "#b15928", "#6a3d9a", "#1f78b4", "#ff7f00",
        "#b2df8a", "#fdbf6f", "#fb9a99", "#cab2d6", "#a6cee3", "#ffff99"};

    /** 'Set3' palette, contributed from http://colorbrewer2.org. */
    private static final String[] PALETTE_SET2 = {"#fb8072", "#bc80bd", "#b3de69", "#80b1d3", "#fdb462", "#8dd3c7",
        "#bebada", "#ffed6f", "#ccebc5", "#d9d9d9", "#fccde5", "#ffffb3"};

    /** Colorblind safe palette, contributed from Color Universal Design, http://jfly.iam.u-tokyo.ac.jp/color/. */
    private static final String[] PALETTE_SET3 = {"#E69F00", "#56B4E9", "#009E73", "#F0E442", "#0072B2", "#D55E00",
        "#CC79A7"};


    /**
     * Enum representing various options to deal with values that have not colors assigned yet.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */

    enum PaletteOption {

        /** Use user-defined set. */
        CUSTOM_SET("custom_set", null),

        /** Use color palette 1. */
        SET1("set_1", PALETTE_SET1),

        /** Use color palette 2. */
        SET2("set_2", PALETTE_SET2),

        /** Use color palette 3. */
        SET3("set_3", PALETTE_SET3);

        /** Missing name exception. */
        private static final String NAME_MUST_NOT_BE_NULL = "Name must not be null";

        /** IllegalArgumentException prefix. */
        private static final String ARGUMENT_EXCEPTION_PREFIX = "No PaletteOption constant with name: ";

        /** The settings representation of this enum. */
        private final String m_identifier;

        private final String[] m_palette;

        private final ColorAttr[] m_paletteAsColorAttr;

        /**
         * Constructor.
         *
         * @param identifier the settings name
         * @param palette The color palette or null.
         */
        PaletteOption(final String identifier, final String[] palette) {
            m_identifier = identifier;
            m_palette = palette;
            m_paletteAsColorAttr = palette == null ? null
                : Arrays.stream(palette).map(Color::decode).map(ColorAttr::getInstance).toArray(ColorAttr[]::new);
        }

        /**
         * Returns the name used for loading and saving.
         *
         * @return the settings name
         */
        String getSettingsName() {
            return m_identifier;
        }

        /**
         * @return the palette (or <code>null</code> for {@link PaletteOption#CUSTOM_SET}).
         */
        String[] getPalette() {
            return m_palette;
        }

        /**
         * @return the paletteAsColorAttr (or <code>null</code> for {@link PaletteOption#CUSTOM_SET}).
         */
        ColorAttr[] getPaletteAsColorAttr() {
            return m_paletteAsColorAttr;
        }

        /**
         * Returns the {@link PaletteOption} represented by the provided name, which can either be the ui or settings
         * name.
         *
         * @param name the ui or settings name of the {@link PaletteOption}
         * @return the corresponding {@link PaletteOption}
         * @throws InvalidSettingsException if the given name is not associated with an {@link PaletteOption} value
         */
        static PaletteOption getEnum(final String name) throws InvalidSettingsException {
            CheckUtils.checkSettingNotNull(name, NAME_MUST_NOT_BE_NULL);
            return Arrays.stream(values())//
                    .filter(t -> t.m_identifier.equals(name))//
                    .findFirst()//
                    .orElseThrow(() -> new InvalidSettingsException(ARGUMENT_EXCEPTION_PREFIX + name));
        }
    }

    /** The selected column. */
    private String m_column;
    /** false if color ranges, true for discrete colors. */
    private boolean m_isNominal;

    /** Option for using palettes. */
    private PaletteOption m_paletteOption = PaletteOption.SET1;

    /** The missing config palette option. */
    static final PaletteOption MISSING_CFG_OPTION = PaletteOption.CUSTOM_SET;

    /** Stores the mapping from string column value to color. */
    private final Map<DataCell, ColorAttr> m_map;

    /** The selected column. */
    private String m_columnGuess;
    /** true if color ranges, false for discrete colors. */
    private boolean m_isNominalGuess;
    /** Stores the mapping from string column value to color. */
    private final Map<DataCell, ColorAttr> m_mapGuess;

    /** Keeps port number for the single input port. */
    static final int INPORT = 0;

    /** Keeps port number for the single input port. */
    static final int OUTPORT = 0;

    /** Keeps the selected column. */
    static final String SELECTED_COLUMN = "selected_column";

    /** The nominal column values. */
    static final String VALUES = "values";

    /** The minimum column value for range color settings. */
    static final String MIN_COLOR = "min_color";

    /** The maximum column value for range color settings. */
    static final String MAX_COLOR = "max_color";

    /** Palette option config key. */
    static final String CFG_PALETTE_OPTION = "palette_option";
    /** Type of color setting. */
    static final String IS_NOMINAL = "is_nominal";


    /** Key for minimum color value. */
    private static final DataCell MIN_VALUE = new StringCell("min_value");

    /** Key for maximum color value. */
    private static final DataCell MAX_VALUE = new StringCell("max_value");

    /**
     * Creates a new model for mapping colors. The model has one input and two
     * outputs.
     *
     */
    public ColorManager2NodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{
                BufferedDataTable.TYPE, ColorHandlerPortObject.TYPE});
        m_map = new LinkedHashMap<DataCell, ColorAttr>();
        m_mapGuess = new LinkedHashMap<DataCell, ColorAttr>();
    }

    /**
     * Is invoked during the node's execution to make the color settings.
     *
     * @param data the input data array
     * @param exec the execution monitor
     * @return the same input data table whereby the RowKeys contain color info
     *         now
     * @throws CanceledExecutionException if user canceled execution
     */
    @Override
    protected PortObject[] execute(final PortObject[] data,
            final ExecutionContext exec) throws CanceledExecutionException, InvalidSettingsException {
        final var in = (BufferedDataTable)data[0];
        final var inSpec = in.getDataTableSpec();
        final var outputSpecification = computeOutputSpecification(inSpec, in, exec.createSubExecutionContext(0.0));
        final var outputTableSpec = outputSpecification.dataSpec;
        final var outputTable = outputTableSpec == inSpec ? in : exec.createSpecReplacerTable(in, outputTableSpec);
        final var outputModelSpec = outputSpecification.modelSpec;
        final var outputModel = new ColorHandlerPortObject(outputModelSpec, "Coloring on \"" + m_column + "\"");
        return new PortObject[]{outputTable, outputModel};
    }

    /**
     * Attaches the given <code>ColorHandler</code> to the given
     * <code>DataTableSpec</code> for the given column.
     * @param spec to which the ColorHandler is appended
     * @param columnName for this column
     * @param colorHdl ColorHandler
     * @return Record of output table spec and model spec.
     */
    static final OutputSpecification getOutputDataSpecification(final DataTableSpec spec,
            final String columnName, final ColorHandler colorHdl) {
        final var tableSpecCreator = new DataTableSpecCreator(spec);
        final DataTableSpec outputTableSpec;
        final DataColumnSpec outputColumnSpec;
        if (COLUMNS_CONTAINED_IN_TABLE_IDENTIFIER.equals(columnName)) {
            tableSpecCreator.setColumnNamesColorHandler(colorHdl);
            final var colorColSpec = new DataColumnSpecCreator(COLUMNS_CONTAINED_IN_TABLE_IDENTIFIER, StringCell.TYPE);
            colorColSpec.setDomain(new DataColumnDomainCreator(columnNamesToDataCellSet(spec)).createDomain());
            colorColSpec.setColorHandler(colorHdl);
            outputColumnSpec = colorColSpec.createSpec();
        } else {
            final var colIndex = spec.findColumnIndex(columnName);
            final var columnSpec = spec.getColumnSpec(colIndex);
            final var columnSpecCreator = new DataColumnSpecCreator(columnSpec);
            columnSpecCreator.setColorHandler(colorHdl);
            outputColumnSpec = columnSpecCreator.createSpec();
            tableSpecCreator.replaceColumn(colIndex, outputColumnSpec);
        }
        outputTableSpec = tableSpecCreator.createSpec();
        return new OutputSpecification(outputTableSpec, new DataTableSpec(outputColumnSpec));
    }

    /**
     * @param inSpecs the input specs passed to the output port
     * @return the same as the input spec
     *
     * @throws InvalidSettingsException if a column is not available
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        final DataTableSpec spec = (DataTableSpec)inSpecs[INPORT];
        CheckUtils.checkSettingNotNull(spec, "No input");
        OutputSpecification outputSpec;
        try {
            outputSpec = computeOutputSpecification(spec, null, null);
        } catch (CanceledExecutionException e) {
            throw new InternalError("Can't fail as data is not iterated", e);
        }
        return new PortObjectSpec[] {outputSpec.dataSpec, outputSpec.modelSpec};
    }

    /**
     * Record representing the specs of both output ports.
     */
    record OutputSpecification(DataTableSpec dataSpec, DataTableSpec modelSpec) {}

    private OutputSpecification computeOutputSpecification(final DataTableSpec spec, final BufferedDataTable table,
        final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        // check null column
        if (m_column == null) {
            // find first nominal column with possible values
            String column = DataTableSpec.guessNominalClassColumn(spec, false);
            CheckUtils.checkSettingNotNull(column, "No column selected and no categorical column available.");
            m_columnGuess = column;
            m_isNominalGuess = true;
            final Set<DataCell> set = spec.getColumnSpec(column).getDomain().getValues();
            m_mapGuess.clear();
            m_mapGuess.putAll(ColorManager2DialogNominal.createColorMapping(set, m_paletteOption));
            setWarningMessage("Selected column \"" + column + "\" with default nominal color mapping.");
            return getOutputDataSpecification(spec, column, createNominalColorHandler(m_mapGuess, m_paletteOption));
        }

        var isColumnNameModel = COLUMNS_CONTAINED_IN_TABLE_IDENTIFIER.equals(m_column);
        // check column in spec
        CheckUtils.checkSetting(spec.containsName(m_column) || isColumnNameModel, "Column \"%s\" not found.", m_column);
        // either set colors by ranges or discrete values
        if (isColumnNameModel || m_isNominal) {
            final Set<DataCell> values;
            final String columnName;
            final ColorHandler colorHandler;
            if (isColumnNameModel) {
                values = columnNamesToDataCellSet(spec);
                columnName = COLUMNS_CONTAINED_IN_TABLE_IDENTIFIER;
            } else {
                // check if all values set are in the domain of the column spec
                DataColumnDomain domain = spec.getColumnSpec(m_column).getDomain();
                columnName = m_column;
                values = domain.getValues();
                CheckUtils.checkSettingNotNull(values,
                    "Column \"%s\" has no nominal values set: execute predecessor or add Binner.", m_column);
            }
            checkValuesForCustomPalette(values);
            if (m_paletteOption == PaletteOption.CUSTOM_SET) {
                colorHandler = createNominalColorHandler(m_map, m_paletteOption);
            } else {
                final Set<DataCell> set = new LinkedHashSet<>(values);
                m_map.clear();
                m_map.putAll(ColorManager2DialogNominal.createColorMapping(set, m_paletteOption));
                colorHandler = createNominalColorHandler(m_map, m_paletteOption);
            }
            return getOutputDataSpecification(spec, columnName, colorHandler);
        } else { // range coloring
            final DataColumnSpec cspec = spec.getColumnSpec(m_column);
            CheckUtils.checkSetting(cspec.getType().isCompatible(DoubleValue.class),
                "Column \"%s\" is not numeric (but %s) and not valid for numeric range coloring",
                m_column, cspec.getType().toPrettyString());
            CheckUtils.checkSetting(m_map.size() == 2, "Color settings not available.");
            DataColumnDomain dom = cspec.getDomain();
            final DataCell lower;
            final DataCell upper;
            if (dom.hasBounds()) {
                lower = dom.getLowerBound();
                upper = dom.getUpperBound();
            } else if (table != null) {
                final var doubleMinMax = new DoubleMinMax(true, m_column);
                var statCalculator = new StatisticCalculator(spec, doubleMinMax);
                statCalculator.evaluate(table, exec);
                lower = new DoubleCell(doubleMinMax.getMin(m_column));
                upper = new DoubleCell(doubleMinMax.getMax(m_column));
            } else {
                lower = null;
                upper = null;
            }

            final var colorHandler = createRangeColorHandler(lower, upper, m_map);
            return getOutputDataSpecification(spec, m_column, colorHandler);
        }
    }

    private void checkValuesForCustomPalette(final Set<DataCell> colNamesSet) throws InvalidSettingsException {
        CheckUtils.checkSetting(m_paletteOption != PaletteOption.CUSTOM_SET || m_map.keySet().containsAll(colNamesSet),
            "Color mapping does not match possible values.");
    }

    /**
     * Load color settings.
     * @param settings Used to read color settings from.
     * @throws InvalidSettingsException If a color property with the settings
     *         is invalid.
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        assert (settings != null);
        // load settings for choosing palettes
        m_paletteOption =
            PaletteOption.getEnum(settings.getString(CFG_PALETTE_OPTION, MISSING_CFG_OPTION.getSettingsName()));
        // remove all color mappings
        m_map.clear();
        // read settings and write into the map
        m_column = settings.getString(SELECTED_COLUMN);
        if (m_column != null) {
            m_isNominal = settings.getBoolean(IS_NOMINAL);
            if (m_isNominal) {
                DataCell[] values = settings.getDataCellArray(VALUES, new DataCell[0]);
                for (int i = 0; i < values.length; i++) {
                    m_map.put(values[i], ColorAttr.getInstance(new Color(
                            settings.getInt(values[i].toString()), true)));
                }
            } else { // range
                // lower color
                Color c0 = new Color(settings.getInt(MIN_COLOR), true);
                m_map.put(MIN_VALUE, ColorAttr.getInstance(c0));
                // upper color
                Color c1 = new Color(settings.getInt(MAX_COLOR), true);
                m_map.put(MAX_VALUE, ColorAttr.getInstance(c1));
                if (c0.equals(c1)) {
                    getLogger().info("Lower and upper color are equal: " + c0);
                }
            }
        }
    }

    /**
     * Save color settings.
     * @param settings Used to write color settings into.
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString(CFG_PALETTE_OPTION, m_paletteOption.getSettingsName());
        if (m_column != null) {
            settings.addString(SELECTED_COLUMN, m_column);
            settings.addBoolean(IS_NOMINAL, m_isNominal);
            // nominal
            if (m_isNominal) {
                if (m_paletteOption == PaletteOption.CUSTOM_SET) { // predefined palette will have colors hard-coded
                    DataCell[] values = new DataCell[m_map.size()];
                    int id = -1;
                    for (DataCell c : m_map.keySet()) {
                        settings.addInt(c.toString(), m_map.get(c).getColor().getRGB());
                        values[++id] = c;
                    }
                    settings.addDataCellArray(VALUES, values);
                }
            } else { // range
                assert m_map.size() == 2;
                settings.addInt(MIN_COLOR, m_map.get(MIN_VALUE).getColor()
                        .getRGB());
                settings.addInt(MAX_COLOR, m_map.get(MAX_VALUE).getColor()
                        .getRGB());
            }
        } else {
            if (m_columnGuess != null) {
                assert (m_isNominalGuess);
                settings.addString(SELECTED_COLUMN, m_columnGuess);
                settings.addBoolean(IS_NOMINAL, m_isNominalGuess);
                DataCell[] values = new DataCell[m_mapGuess.size()];
                int id = -1;
                for (DataCell c : m_mapGuess.keySet()) {
                    settings.addInt(c.toString(), m_mapGuess.get(c).getColor()
                            .getRGB());
                    values[++id] = c;
                }
                settings.addDataCellArray(VALUES, values);
            } else {
                settings.addString(SELECTED_COLUMN, m_column);
            }
        }
    }

    /**
     * Validate the color settings, that are, column name must be available, as
     * well as, a color model either nominal or range that contains a color
     * mapping, from each possible value to a color or from min and max
     * value to color, respectively.
     * @param settings Color settings to validate.
     * @throws InvalidSettingsException If a color property read from the
     *         settings is invalid.
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        String column = settings.getString(SELECTED_COLUMN);
        if (column != null) {
            boolean nominalSelected = settings.getBoolean(IS_NOMINAL);
            if (nominalSelected) {
                DataCell[] values = settings.getDataCellArray(VALUES, new DataCell[0]);
                for (int i = 0; i < values.length; i++) {
                    new Color(settings.getInt(values[i].toString()));
                }
            } else {
                new Color(settings.getInt(MIN_COLOR));
                new Color(settings.getInt(MAX_COLOR));
            }
        }
    }

    private static final ColorHandler createNominalColorHandler(final Map<DataCell, ColorAttr> map,
        final PaletteOption paletteOption) {
        return new ColorHandler(new ColorModelNominal(map, paletteOption.getPaletteAsColorAttr()));
    }

    private static final ColorHandler createRangeColorHandler(
            final DataCell lower, final DataCell upper,
            final Map<DataCell, ColorAttr> map) {
        assert map.size() == 2;
        Color c0 = map.get(MIN_VALUE).getColor();
        Color c1 = map.get(MAX_VALUE).getColor();
        double d0 = Double.NaN;
        if (lower != null && !lower.isMissing()
                && lower.getType().isCompatible(DoubleValue.class)) {
            d0 = ((DoubleValue)lower).getDoubleValue();
        }
        double d1 = Double.NaN;
        if (upper != null && !upper.isMissing()
                && upper.getType().isCompatible(DoubleValue.class)) {
            d1 = ((DoubleValue)upper).getDoubleValue();
        }
        return new ColorHandler(new ColorModelRange(d0, c0, d1, c1));
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * Column names of the non-null argument spec in a set containing {@link StringCell}.
     */
    static Set<DataCell> columnNamesToDataCellSet(final DataTableSpec dts) {
        return Stream.of(dts.getColumnNames()) //
            .map(StringCell::new) //
            .collect(Collectors.toCollection(LinkedHashSet<DataCell>::new)); // order preserving
    }

}
