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
import static org.knime.base.node.viz.property.color.ColorDesignerUtil.createOutputTableSpec;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ColorGradientWrapper;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ValueScale;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModelRange2;
import org.knime.core.data.property.ColorModelRange2.SpecialColorType;
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
                each value to a position on a selected or user-defined gradient. If a table is connected, the same \
                gradient will be applied to all selected columns.<br/>
                <h3>Assignment behavior:</h3>
                Values are colorized based on their position within the chosen gradient scale. Special values such as \
                missing values, NaN, infinities, or out-of bounds values can be assigned specific colors.
                <h3>Modes:</h3>
                <ul>
                <li><b>Without input table:</b> Generates a standalone color gradient model that needs to be applied \
                later via the Color Designer (Apply) node.
                </li>
                <li><b>With input table:</b> Applies the gradient directly to the selected columns of the input table. \
                If the columns have defined domains, their joint domain will be used to map values to the color \
                gradient. For columns without domain, the node will compute a temporary domain during execution.
                </li>
                </ul>
                """) //
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

    record ColorModelAndDependentSpecs(ColorModelRange2 colorModel, List<DataColumnSpec> specs) {
    }

    record MinMax(Double min, Double max) {
    }

    static void configure(final ConfigureInput in, final ConfigureOutput out) throws InvalidSettingsException {
        final var parameters = in.<ColorGradientDesignerNodeParameters> getParameters();
        final var specs = in.getInTableSpecs();
        final var inTableSpec = specs.length > 0 ? specs[0] : null;

        if (inTableSpec == null) {
            out.setOutSpecs(computeModelSpecWithoutInputSpec(parameters));
            return;
        }

        /** see {@link ColorGradientDesignerNodeParameters#validate()} for spec independent parameter validation */
        validateParameters(parameters, inTableSpec);

        final var colorModelAndSpecs = computeModelWithInputSpec(parameters, inTableSpec);
        var colorModel = colorModelAndSpecs.colorModel();
        if (colorModel.isPercentageBased()) {
            colorModel = applyPercentageBasedColorModelFromDomain(colorModel, colorModelAndSpecs.specs());
        }
        final var colorHandler = new ColorHandler(colorModel);
        final var outputModelSpec = createOutputModelSpec(colorHandler, "Color gradient");
        out.setOutSpecs(createOutputTableSpec(inTableSpec, colorModelAndSpecs.specs(), colorHandler), outputModelSpec);
    }

    static void execute(final ExecuteInput in, final ExecuteOutput out)
        throws CanceledExecutionException, KNIMEException {
        final var parameters = in.<ColorGradientDesignerNodeParameters> getParameters();
        final var inTables = in.getInTables();
        final var inTable = inTables.length > 0 ? inTables[0] : null;

        if (inTable == null) {
            final var outputModelSpec = computeModelSpecWithoutInputSpec(parameters);
            final var outputModel =
                new ColorHandlerPortObject(outputModelSpec, outputModelSpec.getColumnSpec(0).getName());
            out.setOutData(outputModel);
            return;
        }

        final var inTableSpec = inTable.getDataTableSpec();
        final var exec = in.getExecutionContext();
        final var colorModelAndSpecs = computeModelWithInputSpec(parameters, inTableSpec);
        var colorModel = colorModelAndSpecs.colorModel();
        if (colorModelAndSpecs.colorModel().isPercentageBased()) {
            colorModel =
                applyPercentageBasedColorModelFromDomainAndTable(inTable, colorModel, colorModelAndSpecs.specs(), exec);
        }

        final var outputSpecs = createOutputSpecs(inTableSpec, colorModelAndSpecs.specs(), colorModel, false);
        final var outTableSpec = outputSpecs.dataSpec();
        final var outputTable =
            outTableSpec.equals(inTableSpec) ? inTable : exec.createSpecReplacerTable(inTable, outTableSpec);
        final var outputModel = new ColorHandlerPortObject(outputSpecs.modelSpec(), outputSpecs.portSummary());
        out.setOutData(outputTable, outputModel);
    }

    static ColorModelRange2 applyPercentageBasedColorModelFromDomainAndTable(final BufferedDataTable inTable,
        final ColorModelRange2 colorModel, final List<DataColumnSpec> dependentColumnSpecs, final ExecutionContext exec)
        throws CanceledExecutionException, KNIMEException {
        final var domainMinMax = extractMinMaxFromDomain(dependentColumnSpecs);
        if (inTable.size() == 0 && (domainMinMax.min() == null || domainMinMax.max() == null)) {
            throw new KNIMEException("Cannot compute color gradient for selected columns, because they do not"
                + " have a domain and the input table is empty.");
        }
        final var extractedMinMax =
            extractMinMaxFromTable(dependentColumnSpecs, inTable.getDataTableSpec(), inTable, exec);
        final var combinedMinMax = combineDomainAndExtractedMinMax(extractedMinMax, domainMinMax);
        return colorModel.applyToDomain(combinedMinMax.min(), combinedMinMax.max());
    }

    static ColorModelRange2 applyPercentageBasedColorModelFromDomain(final ColorModelRange2 colorModel,
        final List<DataColumnSpec> dependentColumnSpecs) throws InvalidSettingsException {
        final var minMax = extractMinMaxFromDomain(dependentColumnSpecs);
        final var min = minMax.min();
        final var max = minMax.max();
        if ((min != null && Double.isInfinite(min)) || (max != null && Double.isInfinite(max))) {
            throw new InvalidSettingsException(String.format(
                "Cannot compute color gradient for selected columns, because their domain includes infinity"
                    + " (minimum: %s, maximum: %s). Remove the affected columns or adjust their domains.",
                formatDouble(min), formatDouble(max)));
        }
        // if the previous node is not executed, no domain is available, so we cannot apply
        if (min != null && max != null) {
            return colorModel.applyToDomain(min, max);
        }
        return colorModel;
    }

    private static double extractDoubleFromCell(final DataCell cell) {
        return ((DoubleValue)cell).getDoubleValue();
    }

    private static Map<SpecialColorType, Color>
        computeSpecialColors(final ColorGradientDesignerNodeParameters parameters) {
        return Map.of( //
            SpecialColorType.MISSING, Color.decode(parameters.m_missingValueColor), //
            SpecialColorType.NAN, Color.decode(parameters.m_nanColor), //
            SpecialColorType.NEGATIVE_INFINITY, Color.decode(parameters.m_negativeInfinityColor), //
            SpecialColorType.BELOW_MIN, Color.decode(parameters.m_belowMinColor), //
            SpecialColorType.ABOVE_MAX, Color.decode(parameters.m_aboveMaxColor), //
            SpecialColorType.POSITIVE_INFINITY, Color.decode(parameters.m_positiveInfinityColor));
    }

    private static DataTableSpec
        computeModelSpecWithoutInputSpec(final ColorGradientDesignerNodeParameters parameters) {
        final var specialColors = computeSpecialColors(parameters);
        final ColorModelRange2 colorModel;
        if (parameters.m_gradient == ColorGradientWrapper.CUSTOM) {
            final var stopValues = extractStopValues(parameters);
            final var stopColors = extractStopColors(parameters);
            colorModel = new ColorModelRange2(specialColors, stopValues, stopColors,
                parameters.m_valueScale == ValueScale.PERCENTAGE);
        } else {
            colorModel = new ColorModelRange2(specialColors, parameters.m_gradient.getColorGradient());
        }
        final var colorHandler = new ColorHandler(colorModel);
        return createOutputModelSpec(colorHandler,
            colorModel.isPercentageBased() ? "Percentage based color gradient" : "Color gradient");

    }

    private static ColorModelAndDependentSpecs
        computeModelWithInputSpec(final ColorGradientDesignerNodeParameters parameters, final DataTableSpec spec) {
        final var specialColors = computeSpecialColors(parameters);
        final var selectedColumnSpecs = getSelectedNumericColumns(parameters.m_columnFilter, spec);
        ColorModelRange2 colorModel;
        if (parameters.m_gradient == ColorGradientWrapper.CUSTOM) {
            final var stopValues = extractStopValues(parameters);
            final var stopColors = extractStopColors(parameters);
            colorModel = new ColorModelRange2(specialColors, stopValues, stopColors,
                parameters.m_valueScale == ValueScale.PERCENTAGE);
        } else {
            colorModel = new ColorModelRange2(specialColors, parameters.m_gradient.getColorGradient());
        }
        return new ColorModelAndDependentSpecs(colorModel, selectedColumnSpecs);
    }

    private static MinMax extractMinMaxFromDomain(final List<DataColumnSpec> selectedColumnSpecs) {
        final var columnSpecsWithDomain =
            selectedColumnSpecs.stream().filter(spec -> spec.getDomain().hasBounds()).toList();

        final var domainMinimum = columnSpecsWithDomain.stream() //
            .map(DataColumnSpec::getDomain) //
            .map(domain -> extractDoubleFromCell(domain.getLowerBound())) //
            .min(Double::compare).orElse(null); //

        final var domainMaximum = columnSpecsWithDomain.stream() //
            .map(DataColumnSpec::getDomain) //
            .map(domain -> extractDoubleFromCell(domain.getUpperBound())) //
            .max(Double::compare).orElse(null); //

        return new MinMax(domainMinimum, domainMaximum);
    }

    private static MinMax extractMinMaxFromTable(final List<DataColumnSpec> columnSpecs, final DataTableSpec spec,
        final BufferedDataTable table, final ExecutionContext exec) throws CanceledExecutionException {
        final var columnIndices = columnSpecs.stream() //
            .filter(colSpec -> !colSpec.getDomain().hasBounds()) //
            .map(DataColumnSpec::getName) //
            .mapToInt(spec::findColumnIndex).toArray();
        if (columnIndices.length == 0) {
            return null;
        }
        final var extractor = new CommonMinMaxExtractor(columnIndices);
        TableExtractorUtil.extractData(table, exec, extractor);
        final var extractedMinMax = extractor.getResult();
        return extractedMinMax == null ? null : new MinMax(extractedMinMax[0], extractedMinMax[1]);
    }

    private static MinMax combineDomainAndExtractedMinMax(final MinMax extractedMinMax, final MinMax domainMinMax)
        throws KNIMEException {
        final var domainMin = domainMinMax.min();
        final var domainMax = domainMinMax.max();
        if (extractedMinMax == null) {
            if (domainMin == null || domainMax == null) {
                throw new KNIMEException(
                    "Cannot compute color gradient for selected columns, because they do not have a"
                        + " domain and the column values are either missing or NaN.");
            }
            return domainMinMax;
        }

        final var extractedMin = extractedMinMax.min();
        final var minimum = domainMin == null ? extractedMin : Math.min(domainMin, extractedMin);

        final var extractedMax = extractedMinMax.max();
        final var maximum = domainMax == null ? extractedMax : Math.max(domainMax, extractedMax);

        if (Double.isInfinite(minimum) || Double.isInfinite(maximum)) {
            throw new KNIMEException(String.format(
                "Cannot compute color gradient for selected columns, because at least one column"
                    + " value is infinite (minimum: %s, maximum: %s).",
                formatDouble(extractedMin), formatDouble(extractedMax)));
        }
        return new MinMax(minimum, maximum);
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
