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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.base.node.viz.property.color.ColorDesignerUtil.OutputSpecification;
import org.knime.base.node.viz.property.color.ColorPaletteDesignerNodeParameters.ApplyColorTo;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModel;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
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
 * The node factory of the Color Palette Designer node.
 *
 * @author Robin Gerling
 * @since 5.10
 */
public final class ColorPaletteDesignerNodeFactory extends DefaultNodeFactory {

    private static final DefaultNode NODE = DefaultNode.create() //
        .name("Color Palette Designer") //
        .icon("./color-palette-designer-icon.png") //
        .shortDescription("Creates a color palette for categorical values or column names.") //
        .fullDescription("""
                Creates a customizable color palette for categorical values or column names. Colors can be \
                assigned automatically using a base palette or explicitly defined for specific values or names.\
                <br/>
                Assignment behavior:
                <ul><li>
                For categorical values, all unique values across the selected columns are combined, sorted in \
                natural order, and assigned colors accordingly.
                </li><li>
                For column names, colors are assigned based on their order in the input table.
                </li></ul>
                Note: Explicit assignments take precedence. Unmatched values or names are assigned colors from the \
                base palette.""") //
        .sinceVersion(5, 10, 0) //
        .dynamicPorts(p -> p //
            .addInputAndOutputPortGroup("Table", ColorPaletteDesignerNodeFactory::createInputOutputTablePortGroup)
            .addOutputPortGroup("model", ColorPaletteDesignerNodeFactory::createOutputModelPort) //
        ) //
        .model(m -> m //
            .parametersClass(ColorPaletteDesignerNodeParameters.class) //
            .configure(ColorPaletteDesignerNodeFactory::configure) //
            .execute(ColorPaletteDesignerNodeFactory::execute) //
        ) //
        .nodeType(NodeType.Visualizer);

    /**
     * Default constructor for the node factory.
     */
    public ColorPaletteDesignerNodeFactory() {
        super(NODE);
    }

    private static ConfigurablePort createInputOutputTablePortGroup(final RequireInputPortName requireInputPortName) {
        return requireInputPortName.inputName("Table") //
            .inputDescription("Input table containing category columns to which colors will be applied."
                + " At least one column with a categorical domain is required for value-based coloring.") //
            .outputName("Table with color information") //
            .outputDescription("""
                    <ul><li>
                    For value-based coloring: Returns the input table with color handlers added to the selected columns.
                    </li><li>
                    For column name coloring: The table itself remains unchanged, but color information for column names
                     is included in the output.
                    </li></ul>
                        """) //
            .optional() //
            .supportedTypes(BufferedDataTable.TYPE);

    }

    private static ConfigurablePort createOutputModelPort(final RequirePortName requirePortName) {
        return requirePortName.name("Color Palette") //
            .description("A color palette object containing all color mappings.") //
            .fixed(ColorHandlerPortObject.TYPE);
    }

    static void configure(final ConfigureInput in, final ConfigureOutput out) throws InvalidSettingsException {
        final var parameters = in.<ColorPaletteDesignerNodeParameters> getParameters();
        final var specs = in.getInTableSpecs();
        final var hasInputTable = specs.length > 0;
        final var inTableSpec = hasInputTable ? specs[0] : null;

        if (inTableSpec != null) {
            /** see {@link ColorPaletteDesignerNodeParameters#validate()} for spec independent parameter validation */
            validateParameters(parameters, inTableSpec);
        }

        final var computedOutputSpecs = computeOutputSpecs(parameters, inTableSpec);

        final DataTableSpec[] outSpecs = hasInputTable //
            ? new DataTableSpec[]{computedOutputSpecs.dataSpec(), computedOutputSpecs.modelSpec()}
            : new DataTableSpec[]{computedOutputSpecs.modelSpec()};

        out.setOutSpecs(outSpecs);
    }

    static void execute(final ExecuteInput in, final ExecuteOutput out) {
        final var parameters = in.<ColorPaletteDesignerNodeParameters> getParameters();
        final var inTables = in.getInTables();
        final var hasInputTable = inTables.length > 0;

        final var inTable = hasInputTable ? inTables[0] : null;
        final var inTableSpec = inTable == null ? null : inTable.getDataTableSpec();

        final var computedOutputSpecs = computeOutputSpecs(parameters, inTableSpec);

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

    static List<DataColumnSpec> getSelectedColumnsWithDomain(final ColumnFilter columnFilter,
        final DataTableSpec spec) {
        final var selectedColumns = columnFilter.filterFromFullSpec(spec);
        return Arrays.stream(selectedColumns) //
            .map(spec::getColumnSpec) //
            .filter(colSpec -> colSpec.getDomain().hasValues()) //
            .toList();
    }

    static List<DataCell> computeDomainValues(final List<DataColumnSpec> selectedColumnSpecsWithDomain) {
        return selectedColumnSpecsWithDomain.stream() //
            .map(DataColumnSpec::getDomain) //
            .map(DataColumnDomain::getValues) //
            .flatMap(Set::stream) //
            .distinct() //
            .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString())) //
            .toList();
    }

    static List<DataCell> computeColumnNames(final DataTableSpec spec) {
        return Arrays.stream(spec.getColumnNames()).map(s -> (DataCell)new StringCell(s)).toList();
    }

    static DataTableSpec createSpecWithColumnsNamesColorHandler(final DataTableSpec spec,
        final ColorHandler colorHandler) {
        final var tableSpecCreator = new DataTableSpecCreator(spec);
        tableSpecCreator.setColumnNamesColorHandler(colorHandler);
        return tableSpecCreator.createSpec();
    }

    private static OutputSpecification computeOutputSpecs(final ColorPaletteDesignerNodeParameters parameters,
        final DataTableSpec spec) {

        final var colorPalette = parameters.m_basePalette == ColorPaletteOption.CUSTOM //
            ? Arrays.stream(parameters.m_customPalette) //
                .map(customColor -> customColor.m_color) //
                .map(ColorPaletteDesignerNodeFactory::hexToColorAttr) //
                .toArray(ColorAttr[]::new)
            : parameters.m_basePalette.getPaletteAsColorAttr();

        final var assignedColors = Arrays.stream(parameters.m_assignedColors) //
            .collect(Collectors.toMap( //
                colorRule -> new StringCell(colorRule.m_matchingValue), //
                colorRule -> hexToColorAttr(colorRule.m_color), //
                (oldV, newV) -> oldV, //
                HashMap<DataCell, ColorAttr>::new));
        assignedColors.put(new MissingCell(null), hexToColorAttr(parameters.m_missingValueColor));

        if (spec == null) {
            final var colorModel = createColorModelForValues(assignedColors, colorPalette, List.of());
            final var modelSpec = createOutputModelSpec(new ColorHandler(colorModel), "Color handler");
            return new OutputSpecification(null, modelSpec, null);
        }

        return parameters.m_applyTo == ApplyColorTo.VALUES
            ? computeOutputSpecsForColumnValueColoring(parameters, spec, assignedColors, colorPalette)
            : computeOutputSpecsForColumnNameColoring(spec, assignedColors, colorPalette);
    }

    private static OutputSpecification computeOutputSpecsForColumnValueColoring(
        final ColorPaletteDesignerNodeParameters parameters, final DataTableSpec spec,
        final Map<DataCell, ColorAttr> assignedColors, final ColorAttr[] colorPalette) {
        final var selectedColumnSpecsWithDomain = getSelectedColumnsWithDomain(parameters.m_columnFilter, spec);
        final var domainValues = computeDomainValues(selectedColumnSpecsWithDomain);
        final var colorModel = createColorModelForValues(assignedColors, colorPalette, domainValues);
        return createOutputSpecs(spec, selectedColumnSpecsWithDomain, colorModel, false);
    }

    private static OutputSpecification computeOutputSpecsForColumnNameColoring(final DataTableSpec spec,
        final Map<DataCell, ColorAttr> assignedColors, final ColorAttr[] colorPalette) {
        final var columnNames = computeColumnNames(spec);
        final var colorModel = createColorModelForValues(assignedColors, colorPalette, columnNames);
        final var colorHandler = new ColorHandler(colorModel);

        final var outputTableSpec = createSpecWithColumnsNamesColorHandler(spec, colorHandler);
        final var colorColSpec = new DataColumnSpecCreator("Column names color handler", StringCell.TYPE);
        colorColSpec.setDomain(new DataColumnDomainCreator(columnNamesToDataCellSet(spec)).createDomain());
        colorColSpec.setColorHandler(colorHandler);
        final var outputColumnSpec = colorColSpec.createSpec();
        return new OutputSpecification(outputTableSpec, new DataTableSpec(outputColumnSpec),
            "Coloring on column names");
    }

    private static ColorModel createColorModelForValues(final Map<DataCell, ColorAttr> assignedColors,
        final ColorAttr[] colorPalette, final List<DataCell> valuesToColor) {
        final var assignedColorsModel = new ColorModelNominal(assignedColors, colorPalette, assignedColors.keySet());
        return assignedColorsModel.applyToNewValues(valuesToColor);
    }

    private static Set<DataCell> columnNamesToDataCellSet(final DataTableSpec dts) {
        return Stream.of(dts.getColumnNames()) //
            .map(StringCell::new) //
            .collect(Collectors.toCollection(LinkedHashSet<DataCell>::new));
    }

    static ColorAttr hexToColorAttr(final String hex) {
        return ColorAttr.getInstance(Color.decode(hex));
    }

    private static void validateParameters(final ColorPaletteDesignerNodeParameters parameters,
        final DataTableSpec spec) throws InvalidSettingsException {
        if (parameters.m_applyTo == ApplyColorTo.VALUES) {
            final var selectedColumnSpecs = getSelectedColumnsWithDomain(parameters.m_columnFilter, spec);
            if (selectedColumnSpecs.isEmpty()) {
                throw new InvalidSettingsException("No columns selected for value color assignment.");
            }
        } else {
            final var columnNames = spec.getColumnNames();
            if (columnNames.length == 0) {
                throw new InvalidSettingsException("The input table has no columns for color assignment.");
            }
        }
    }

}
