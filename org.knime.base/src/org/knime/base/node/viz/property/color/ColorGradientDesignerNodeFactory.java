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
 *   28 Nov 2025 (Robin Gerling): created
 */
package org.knime.base.node.viz.property.color;

import static org.knime.base.node.viz.property.color.ColorDesignerUtil.createOutputModelSpec;
import static org.knime.base.node.viz.property.color.ColorDesignerUtil.createOutputSpecs;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.base.node.viz.property.color.ColorDesignerUtil.OutputSpecification;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ColorGradientWrapper;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ValueScale;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModel;
import org.knime.core.data.property.ColorModelRange;
import org.knime.core.data.property.ColorModelRange.SpecialColorType;
import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.RowReadUtil;
import org.knime.core.data.v2.TableExtractorUtil;
import org.knime.core.data.v2.TableExtractorUtil.Extractor;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.port.viewproperty.ColorHandlerPortObject;
import org.knime.node.DefaultModel.ConfigureInput;
import org.knime.node.DefaultModel.ConfigureOutput;
import org.knime.node.DefaultModel.ExecuteInput;
import org.knime.node.DefaultModel.ExecuteOutput;
import org.knime.node.DefaultNode;
import org.knime.node.DefaultNodeFactory;
import org.knime.node.RequirePorts.ConfigurablePort;
import org.knime.node.RequirePorts.RequireInputPortName;
import org.knime.node.RequirePorts.RequirePortName;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;

/**
 * The node factory of the Color Gradient Designer node.
 *
 * @author Robin Gerling
 * @since 5.10
 */
public final class ColorGradientDesignerNodeFactory extends DefaultNodeFactory {

    private static final DefaultNode NODE = DefaultNode.create() //
        .name("Color Gradient Designer") //
        .icon("./color-gradient-designer-icon.png") //
        .shortDescription("Creates a color gradient for numerical values.") //
        .fullDescription("""
                Creates a customizable gradient-based color mapping for numeric values. Colors are assigned by mapping \
                each value to a position on a selected or user-defined gradient. The same gradient is applied to all \
                selected columns.<br/>
                Assignment behavior:<br/>
                Values are colorized based on their position within the chosen gradient scale (percentage or \
                absolute). For a percentage based scale, the joint domain of the selected columns is used to transform \
                it into an absolute scale. Special values such as missing values, NaN, or infinities can be assigned \
                specific colors. Out-of-bounds values can also be assigned specific colors.""") //
        .sinceVersion(5, 10, 0) //
        .dynamicPorts(p -> p //
            .addInputAndOutputPortGroup("Table", ColorGradientDesignerNodeFactory::createInputOutputTablePortGroup)
            .addOutputPortGroup("model", ColorGradientDesignerNodeFactory::createOutputModelPort) //
        ) //
        .model(m -> m //
            .parametersClass(ColorGradientDesignerNodeParameters.class) //
            .configure(ColorGradientDesignerNodeFactory::configure) //
            .execute(ColorGradientDesignerNodeFactory::execute) //
        ) //
        .nodeType(NodeType.Visualizer);

    /**
     * Default constructor for the node factory.
     */
    public ColorGradientDesignerNodeFactory() {
        super(NODE);
    }

    private static ConfigurablePort createInputOutputTablePortGroup(final RequireInputPortName requireInputPortName) {
        return requireInputPortName.inputName("Table") //
            .inputDescription("Input table containing numeric columns to which colors will be applied."
                + " At least one numerical column is required for percentage-based coloring.") //
            .outputName("Table with color information") //
            .outputDescription("Returns the input table with color handlers added to the selected columns.") //
            .optional() //
            .supportedTypes(BufferedDataTable.TYPE);

    }

    private static ConfigurablePort createOutputModelPort(final RequirePortName requirePortName) {
        return requirePortName.name("Color gradient") //
            .description("A color gradient object containing the gradient definition.") //
            .fixed(ColorHandlerPortObject.TYPE);
    }

    static void configure(final ConfigureInput in, final ConfigureOutput out) throws InvalidSettingsException {
        final var parameters = in.<ColorGradientDesignerNodeParameters> getParameters();
        final var specs = in.getInTableSpecs();
        final var hasInputTable = specs.length > 0;
        final var inTableSpec = hasInputTable ? specs[0] : null;

        if (inTableSpec != null) {
            /** see {@link ColorGradientDesignerNodeParameters#validate()} for spec independent parameter validation */
            validateParameters(parameters, inTableSpec);
        }

        try {
            final var computedOutputSpecs = computeOutputSpecs(parameters, inTableSpec, null, null);
            final DataTableSpec[] outSpecs = hasInputTable //
                ? new DataTableSpec[]{computedOutputSpecs.dataSpec(), computedOutputSpecs.modelSpec()}
                : new DataTableSpec[]{computedOutputSpecs.modelSpec()};

            out.setOutSpecs(outSpecs);
        } catch (final CanceledExecutionException | KNIMEException e) { // NOSONAR
            /**
             * Both exceptions can only occur during execution when the table passed to {@link computeOutputSpecs} is
             * not null, which is not the case during configure.
             */
        }
    }

    static void execute(final ExecuteInput in, final ExecuteOutput out)
        throws CanceledExecutionException, KNIMEException {
        final var parameters = in.<ColorGradientDesignerNodeParameters> getParameters();
        final var inTables = in.getInTables();
        final var hasInputTable = inTables.length > 0;

        final var inTable = hasInputTable ? inTables[0] : null;
        final var inTableSpec = inTable == null ? null : inTable.getDataTableSpec();

        final var computedOutputSpecs = computeOutputSpecs(parameters, inTableSpec, inTable, in.getExecutionContext());

        final var exec = in.getExecutionContext();
        final var outTableSpec = computedOutputSpecs.dataSpec();
        final var outputModel =
            new ColorHandlerPortObject(computedOutputSpecs.modelSpec(), computedOutputSpecs.portSummary());

        if (hasInputTable) {
            final var outputTable =
                outTableSpec.equals(inTableSpec) ? inTable : exec.createSpecReplacerTable(inTable, outTableSpec);

            out.setOutData(outputTable, outputModel);
        } else {
            out.setOutData(outputModel);
        }
    }

    private static double extractDoubleFromCell(final DataCell cell) {
        return ((DoubleValue)cell).getDoubleValue();
    }

    private static OutputSpecification computeOutputSpecs(final ColorGradientDesignerNodeParameters parameters,
        final DataTableSpec spec, final BufferedDataTable table, final ExecutionContext exec)
        throws CanceledExecutionException, KNIMEException {
        final var specialColors = Map.of( //
            SpecialColorType.MISSING, Color.decode(parameters.m_missingValueColor), //
            SpecialColorType.NAN, Color.decode(parameters.m_nanColor), //
            SpecialColorType.NEGATIVE_INFINITY, Color.decode(parameters.m_negativeInfinityColor), //
            SpecialColorType.BELOW_MIN, Color.decode(parameters.m_belowMinColor), //
            SpecialColorType.ABOVE_MAX, Color.decode(parameters.m_aboveMaxColor), //
            SpecialColorType.POSITIVE_INFINITY, Color.decode(parameters.m_positiveInfinityColor));

        if (spec == null) {
            final ColorModel colorModel;
            if (parameters.m_gradient == ColorGradientWrapper.CUSTOM) {
                final var stopValues = extractStopValues(parameters);
                final var stopColors = extractStopColors(parameters);
                colorModel = new ColorModelRange(specialColors, stopValues, stopColors,
                    parameters.m_valueScale == ValueScale.PERCENTAGE);
            } else {
                colorModel = new ColorModelRange(specialColors, parameters.m_gradient.getColorGradient());
            }
            final var modelSpec = createOutputModelSpec(new ColorHandler(colorModel), "Color handler");
            return new OutputSpecification(null, modelSpec, null);
        }

        final var selectedColumnSpecs = getSelectedNumericColumns(parameters.m_columnFilter, spec);
        ColorModelRange colorModel;
        if (parameters.m_gradient == ColorGradientWrapper.CUSTOM) {
            final var stopValues = extractStopValues(parameters);
            final var stopColors = extractStopColors(parameters);
            if (parameters.m_valueScale == ValueScale.ABSOLUTE) {
                colorModel = new ColorModelRange(specialColors, stopValues, stopColors, false);
                return createOutputSpecs(spec, selectedColumnSpecs, colorModel, false);
            }
            colorModel = new ColorModelRange(specialColors, stopValues, stopColors, true);
        } else {
            colorModel = new ColorModelRange(specialColors, parameters.m_gradient.getColorGradient());
        }

        final var minMax = extractMinMaxFromDomainAndTable(selectedColumnSpecs, table, spec, exec);
        if (minMax.length == 2) {
            colorModel = colorModel.applyToDomain(minMax[0], minMax[1]);
        }
        return createOutputSpecs(spec, selectedColumnSpecs, colorModel, false);
    }

    static double[] extractMinMaxFromDomainAndTable(final List<DataColumnSpec> selectedColumnSpecs,
        final BufferedDataTable table, final DataTableSpec spec, final ExecutionContext exec)
        throws CanceledExecutionException, KNIMEException {
        final var columnSpecsPartitionedHasDomain = selectedColumnSpecs.stream() //
            .collect(Collectors.partitioningBy(col -> col.getDomain().hasBounds()));

        final var domainMinimum = columnSpecsPartitionedHasDomain.get(true).stream() //
            .map(DataColumnSpec::getDomain) //
            .map(domain -> extractDoubleFromCell(domain.getLowerBound())) //
            .min(Double::compare).orElse(null); //

        final var domainMaximum = columnSpecsPartitionedHasDomain.get(true).stream() //
            .map(DataColumnSpec::getDomain) //
            .map(domain -> extractDoubleFromCell(domain.getUpperBound())) //
            .max(Double::compare).orElse(null); //

        if ((domainMinimum != null && Double.isInfinite(domainMinimum))
            || (domainMaximum != null && Double.isInfinite(domainMaximum))) {
            throw new KNIMEException(String.format(
                "Cannot compute color gradient for selected columns, because their domain"
                    + " includes infinity (minimum: %s, maximum: %s).",
                formatDouble(domainMinimum), formatDouble(domainMaximum)));
        }

        final var hasCommonDomain = domainMinimum != null && domainMaximum != null;
        if (table == null) {
            return hasCommonDomain ? new double[]{domainMinimum, domainMaximum} : new double[0];
        }

        if (table.size() == 0) {
            if (hasCommonDomain) {
                return new double[]{domainMinimum, domainMaximum};
            }
            throw new KNIMEException("Cannot compute color gradient for selected columns, because they do not"
                + " have a domain and the input table is empty.");
        }
        return extractMinMaxFromTable(columnSpecsPartitionedHasDomain, domainMinimum, domainMaximum, spec, table, exec);
    }

    private static double[] extractMinMaxFromTable(
        final Map<Boolean, List<DataColumnSpec>> columnSpecsPartitionedHasDomain, final Double domainMinimum,
        final Double domainMaximum, final DataTableSpec spec, final BufferedDataTable table,
        final ExecutionContext exec) throws KNIMEException, CanceledExecutionException {
        final var columnIndices = columnSpecsPartitionedHasDomain.get(false).stream() //
            .map(DataColumnSpec::getName) //
            .mapToInt(spec::findColumnIndex).toArray();
        final var extractor = new CommonMinMaxExtractor(columnIndices);
        TableExtractorUtil.extractData(table, exec, extractor);
        final var extractedMinMax = extractor.getResult();

        if (extractedMinMax == null) {
            if (domainMinimum == null || domainMaximum == null) {
                throw new KNIMEException(
                    "Cannot compute color gradient for selected columns, because they do not have a"
                        + " domain and the column values are either missing or NaN.");
            }
            return new double[]{domainMinimum, domainMaximum};
        }

        final var computedMinimum = extractedMinMax[0];
        final var minimum = domainMinimum == null ? computedMinimum : Math.min(domainMinimum, computedMinimum);

        final var computedMaximum = extractedMinMax[1];
        final var maximum = domainMaximum == null ? computedMaximum : Math.max(domainMaximum, computedMaximum);

        if (Double.isInfinite(minimum) || Double.isInfinite(maximum)) {
            throw new KNIMEException(String.format(
                "Cannot compute color gradient for selected columns, because at least one column"
                    + " value is infinite (minimum: %s, maximum: %s).",
                formatDouble(computedMinimum), formatDouble(computedMaximum)));
        }
        return new double[]{minimum, maximum};
    }

    private static double[] extractStopValues(final ColorGradientDesignerNodeParameters parameters) {
        return Arrays.stream(parameters.m_customGradient) //
            .mapToDouble(stopValueColor -> stopValueColor.m_stopValue) //
            .toArray();
    }

    private static Color[] extractStopColors(final ColorGradientDesignerNodeParameters parameters) {
        return Arrays.stream(parameters.m_customGradient) //
            .map(stopValueColor -> Color.decode(stopValueColor.m_color)) //
            .toArray(Color[]::new);
    }

    static List<DataColumnSpec> getSelectedNumericColumns(final ColumnFilter columnFilter, final DataTableSpec spec) {
        final var selectedColumns = columnFilter.filterFromFullSpec(spec);
        return Arrays.stream(selectedColumns) //
            .map(spec::getColumnSpec) //
            .filter(colSpec -> colSpec.getType().isCompatible(DoubleValue.class)) //
            .toList();
    }

    private static void validateParameters(final ColorGradientDesignerNodeParameters parameters,
        final DataTableSpec spec) throws InvalidSettingsException {
        final var selectedColumns = getSelectedNumericColumns(parameters.m_columnFilter, spec);
        if (selectedColumns.isEmpty()) {
            throw new InvalidSettingsException("No columns selected for gradient creation.");
        }
    }

    private static String formatDouble(final Double d) {
        if (d == null) {
            return "";
        }
        if (d.longValue() == d) {
            return Long.toString(d.longValue());
        }
        return Double.toString(d);
    }

    private static final class CommonMinMaxExtractor implements Extractor {

        private final int[] m_colIndices;

        private double[] m_result;

        public CommonMinMaxExtractor(final int[] colIndices) {
            m_colIndices = colIndices;
        }

        @Override
        public void init(final int size) {
            // nothing to do
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            for (int colIndex : m_colIndices) {
                if (row.isMissing(colIndex)) {
                    return;
                }
                final var currentValue = RowReadUtil.readPrimitiveDoubleValue(row, colIndex);
                if (Double.isNaN(currentValue)) {
                    return;
                }
                if (m_result == null) {
                    m_result = new double[]{currentValue, currentValue};
                } else {
                    m_result[0] = Math.min(m_result[0], currentValue);
                    m_result[1] = Math.max(m_result[1], currentValue);
                }
            }
        }

        @Override
        public int[] getColumnIndices() {
            return m_colIndices;
        }

        double[] getResult() {
            return m_result;
        }

    }

}
