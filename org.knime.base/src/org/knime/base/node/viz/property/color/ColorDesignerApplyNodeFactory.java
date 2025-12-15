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

import java.util.Optional;
import java.util.stream.Stream;

import org.knime.base.node.viz.property.color.ColorDesignerUtil.OutputSpecification;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModel;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange2;
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
import org.knime.node.RequirePorts.PortsAdder;

/**
 * The node factory of the Color Palette Designer node.
 *
 * @author Robin Gerling
 * @since 5.10
 */
public final class ColorDesignerApplyNodeFactory extends DefaultNodeFactory {

    private static final DefaultNode NODE = DefaultNode.create() //
        .name("Color Designer (Apply)") //
        .icon("./color-designer-apply.png") //
        .shortDescription("Applies a color model to a table's values or column names.") //
        .fullDescription("""
                Applies an existing color model to the selected columns or column names of an input table. The \
                input color model may be categorical (color palette) or numerical (color gradient), and is applied \
                according to its configuration.<br/><br/>
                Application behavior:
                <ul>
                <li><b>Categorical models</b>: Assign colors to values or column names. New values found in the \
                input table are added to the model and assigned colors based on its configuration.</li>
                <li><b>Numerical models</b>: Color values using a gradient with percentage-based or absolute \
                stop values.
                    <ul>
                        <li>For <b>percentage-based</b> models: The joint domain of the selected columns is used \
                        to interpret the percentage stops as absolute values.</li>
                        <li>For <b>absolute models</b>: The model is applied directly without modification.</li>
                    </ul>
                </li>
                </ul>
                The output table includes color handlers for the affected columns, and the output model reflects \
                any updates made during application.
                """) //
        .sinceVersion(5, 10, 0) //
        .ports(ColorDesignerApplyNodeFactory::ports) //
        .model(m -> m //
            .parametersClass(ColorDesignerApplyNodeParameters.class) //
            .configure(ColorDesignerApplyNodeFactory::configure) //
            .execute(ColorDesignerApplyNodeFactory::execute) //
        ) //
        .nodeType(NodeType.Visualizer);

    /**
     * Default constructor for the node factory.
     */
    public ColorDesignerApplyNodeFactory() {
        super(NODE);
    }

    private static void ports(final PortsAdder portsAdder) {
        portsAdder
            .addInputPort("Color Model",
                "A color model (palette or gradient) that defines"
                    + " how colors are assigned to the input table's values or column names.",
                ColorHandlerPortObject.TYPE)
            .addInputTable("Table",
                "Input table containing the columns or column names to which the color model is applied.")
            .addOutputTable("Colored Table",
                "Table with color handlers added to the selected columns based on the applied model.")
            .addOutputPort("Applied Color Model", """
                    <ul>
                    <li><b>Categorical model</b>: Returns the input model updated with new values found in the table.
                    </li>
                    <li><b>Percentage-based numerical model</b>: Returns the model adjusted to the domain of the \
                    selected columns.</li>
                    <li><b>Absolute numerical model</b>: Returns the input model unchanged.</li>
                    </ul>
                    """, ColorHandlerPortObject.TYPE);
    }

    static void configure(final ConfigureInput in, final ConfigureOutput out) throws InvalidSettingsException {
        final var inModelSpec = (DataTableSpec)in.getInPortSpec(0);
        final var inTableSpec = in.getInTableSpec(1);

        if (inModelSpec == null || inTableSpec == null) {
            out.setOutSpecs(inTableSpec, inModelSpec);
            return;
        }

        final var colorModel = extractColorModel(inModelSpec).orElseThrow(() -> new InvalidSettingsException(
            "The input color model is invalid. Ensure the input model has exactly one column with a color handler."));

        final var parameters = in.<ColorDesignerApplyNodeParameters> getParameters();
        validateParameters(parameters, inTableSpec, colorModel);

        final DataTableSpec outTableSpec;
        var outModelSpec = inModelSpec;
        if (colorModel instanceof ColorModelNominal nominalModel) {
            final var outputSpecs = computeOutputSpecsNominalModel(parameters, inTableSpec, nominalModel);
            outTableSpec = outputSpecs.dataSpec();
            outModelSpec = outputSpecs.modelSpec();
        } else if (colorModel instanceof ColorModelRange2 colorModelRange) {
            final var selectedColumnSpecs =
                ColorGradientDesignerNodeFactory.getSelectedNumericColumns(parameters.m_columnFilter, inTableSpec);
            if (colorModelRange.isPercentageBased()) {
                final var updatedRangeModel = ColorGradientDesignerNodeFactory
                    .applyPercentageBasedColorModelFromDomain(colorModelRange, selectedColumnSpecs);
                final var colorHandler = new ColorHandler(updatedRangeModel);
                outModelSpec = ColorDesignerUtil.createOutputModelSpec(colorHandler, "Color gradient");
                outTableSpec = ColorDesignerUtil.createOutputTableSpec(inTableSpec, selectedColumnSpecs, colorHandler);
            } else {
                outTableSpec = ColorDesignerUtil.createOutputTableSpec(inTableSpec, selectedColumnSpecs,
                    extractColorHandler(inModelSpec));
            }
        } else {
            throw new InvalidSettingsException(
                "The input color model type is not supported: " + colorModel.getClass().getSimpleName());
        }
        out.setOutSpecs(outTableSpec, outModelSpec);
    }

    static void execute(final ExecuteInput in, final ExecuteOutput out)
        throws CanceledExecutionException, KNIMEException {
        final var parameters = in.<ColorDesignerApplyNodeParameters> getParameters();
        final var inModel = in.getInPortObject(0);
        final var inTable = in.getInTable(1);

        final var inModelSpec = ((ColorHandlerPortObject)inModel).getSpec();
        // Illegal state since we checked for the presence of the color model in configure()
        final var colorModel = extractColorModel(inModelSpec).orElseThrow(IllegalStateException::new);
        final var inTableSpec = inTable.getDataTableSpec();

        final var exec = in.getExecutionContext();

        final OutputSpecification outSpecs;
        if (colorModel instanceof ColorModelNominal nominalModel) {
            outSpecs = computeOutputSpecsNominalModel(parameters, inTableSpec, nominalModel);
        } else {
            // We checked for the type of the color model in configure(), so this cast is safe.
            final var colorModelRange = (ColorModelRange2)colorModel;
            outSpecs = computeOutputSpecsRangeModelExecute(parameters, inTable, inModelSpec, exec, colorModelRange);
        }

        final var outTableSpec = outSpecs.dataSpec();
        final var outputTable =
            outTableSpec.equals(inTableSpec) ? inTable : exec.createSpecReplacerTable(inTable, outTableSpec);
        final var outModel = new ColorHandlerPortObject(outSpecs.modelSpec(), outSpecs.portSummary());
        out.setOutData(outputTable, outModel);
    }

    private static OutputSpecification computeOutputSpecsNominalModel(final ColorDesignerApplyNodeParameters parameters,
        final DataTableSpec tableSpec, final ColorModelNominal nominalModel) {
        final var selectedColumnSpecsWithDomain =
            ColorPaletteDesignerNodeFactory.getSelectedColumnsWithDomain(parameters.m_columnFilter, tableSpec);

        final var domainValues = ColorPaletteDesignerNodeFactory.computeDomainValues(selectedColumnSpecsWithDomain);
        if (!parameters.m_applyToColumnNames) {
            final var updatedColorModel = nominalModel.applyToNewValues(domainValues);
            return ColorDesignerUtil.createOutputSpecs(tableSpec, selectedColumnSpecsWithDomain, updatedColorModel,
                false);
        }
        final var columnNames = ColorPaletteDesignerNodeFactory.computeColumnNames(tableSpec);
        final var domainValuesAndColumnNames = Stream.concat(domainValues.stream(), columnNames.stream()).toList();
        final var updatedColorModel = nominalModel.applyToNewValues(domainValuesAndColumnNames);
        final var colorHandler = new ColorHandler(updatedColorModel);
        final var updatedTableSpec =
            ColorPaletteDesignerNodeFactory.createSpecWithColumnsNamesColorHandler(tableSpec, colorHandler);
        return ColorDesignerUtil.createOutputSpecs(updatedTableSpec, selectedColumnSpecsWithDomain, updatedColorModel,
            true);
    }

    private static OutputSpecification computeOutputSpecsRangeModelExecute(
        final ColorDesignerApplyNodeParameters parameters, final BufferedDataTable inTable,
        final DataTableSpec inModelSpec, final ExecutionContext exec, final ColorModelRange2 rangeModel)
        throws CanceledExecutionException, KNIMEException {
        final var inTableSpec = inTable.getDataTableSpec();
        final var selectedColumnSpecs =
            ColorGradientDesignerNodeFactory.getSelectedNumericColumns(parameters.m_columnFilter, inTableSpec);
        if (rangeModel.isPercentageBased()) {
            final var updatedRangeModel = ColorGradientDesignerNodeFactory
                .applyPercentageBasedColorModelFromDomainAndTable(inTable, rangeModel, selectedColumnSpecs, exec);
            return ColorDesignerUtil.createOutputSpecs(inTableSpec, selectedColumnSpecs, updatedRangeModel, false);
        }
        final var outTableSpec =
            ColorDesignerUtil.createOutputTableSpec(inTableSpec, selectedColumnSpecs, extractColorHandler(inModelSpec));
        final var portSummary = ColorDesignerUtil.createPortSummary(selectedColumnSpecs, false);
        return new OutputSpecification(outTableSpec, inModelSpec, portSummary);
    }

    private static void validateParameters(final ColorDesignerApplyNodeParameters parameters, final DataTableSpec spec,
        final ColorModel colorModel) throws InvalidSettingsException {
        if (colorModel instanceof ColorModelNominal) {
            final var selectedColumnSpecs =
                ColorPaletteDesignerNodeFactory.getSelectedColumnsWithDomain(parameters.m_columnFilter, spec);
            if (selectedColumnSpecs.isEmpty() && !parameters.m_applyToColumnNames) {
                throw new InvalidSettingsException(
                    "Neither columns with a domain nor column names are selected for applying colors.");
            }
        } else if (colorModel instanceof ColorModelRange2) {
            final var selectedColumnSpecs =
                ColorGradientDesignerNodeFactory.getSelectedNumericColumns(parameters.m_columnFilter, spec);
            if (selectedColumnSpecs.isEmpty()) {
                throw new InvalidSettingsException("No numeric columns are selected for applying colors.");
            }
        }
    }

    static Optional<ColorModel> extractColorModel(final DataTableSpec modelTableSpec) {
        if (modelTableSpec.getNumColumns() != 1) {
            return Optional.empty();
        }
        final var colorHandler = extractColorHandler(modelTableSpec);
        if (colorHandler == null) {
            return Optional.empty();
        }
        return Optional.of(colorHandler.getColorModel());
    }

    private static ColorHandler extractColorHandler(final DataTableSpec modelTableSpec) {
        return modelTableSpec.getColumnSpec(0).getColorHandler();

    }

}
