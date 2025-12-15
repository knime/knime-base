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

import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.property.ColorModel;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange2;
import org.knime.core.node.port.viewproperty.ColorHandlerPortObject;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.Message;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;

/**
 * The node parameters for the {@link ColorPaletteDesignerNodeFactory}.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
final class ColorDesignerApplyNodeParameters implements NodeParameters {

    public ColorDesignerApplyNodeParameters() {
        // default constructor
    }

    public ColorDesignerApplyNodeParameters(final NodeParametersInput nodeParametersInput) {
        final var modelSpec = nodeParametersInput.getInPortSpec(0);
        final var tableSpec = nodeParametersInput.getInPortSpec(1);
        if (modelSpec.isEmpty() || tableSpec.isEmpty()) {
            return;
        }
        final var columnChoices = computeColumnChoicesByColorModel(nodeParametersInput);
        final var columnNames = columnChoices.stream().map(DataColumnSpec::getName).toArray(String[]::new);
        m_columnFilter = new ColumnFilter(columnNames);
    }

    @TextMessage(ModelTypeMessageProvider.class)
    Void m_modelTypeMessage;

    @Widget(title = "Columns", description = "Specify one or more columns whose values will be colorized.")
    @ChoicesProvider(DomainColumnChoicesProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Apply to Column Names",
        description = "Whether the categorical color handler should be applied to the column names.")
    @Effect(predicate = InputIsNominalColorModel.class, type = EffectType.SHOW)
    boolean m_applyToColumnNames;

    private static final class InputIsNominalColorModel implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant( //
                npi -> extractColorModel(npi).map(ColorModelNominal.class::isInstance).orElse(false));
        }
    }

    private static final class DomainColumnChoicesProvider implements ColumnChoicesProvider {
        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            return computeColumnChoicesByColorModel(context);
        }
    }

    private static final class ModelTypeMessageProvider implements StateProvider<Optional<Message>> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Optional<Message> computeState(final NodeParametersInput parametersInput) {
            final var inPortSpecs = parametersInput.getInPortSpecs();
            final var inModelSpecIsMissing = inPortSpecs[0] == null;
            final var inTableSpecIsMissing = inPortSpecs[1] == null;
            if (inModelSpecIsMissing || inTableSpecIsMissing) {
                final String title;
                final String description;
                if (inModelSpecIsMissing && inTableSpecIsMissing) {
                    title = "No input table and color model connected.";
                    description = "An input table and a color model are required for configuration.";
                } else if (inModelSpecIsMissing) {
                    title = "No color model connected.";
                    description = "A color model is required for configuration.";
                } else {
                    title = "No input table connected.";
                    description = "An input table is required for configuration.";
                }
                return Optional.of(new Message(title, description, MessageType.WARNING));
            }

            final var colorModelOpt = extractColorModel(parametersInput);
            if (colorModelOpt.isEmpty()) {
                return Optional.of(new Message("Cannot determine color model type.",
                    "The color model type is required for configuration."
                        + " Execute upstream model nodes to determine the type.",
                    MessageType.WARNING));
            }
            final var colorModel = colorModelOpt.get();
            if (colorModel instanceof ColorModelNominal) {
                return Optional.of(new Message("Categorical color model connected.",
                    "The color model can be applied to categorical columns with a domain or column names.",
                    MessageType.INFO));
            }
            if (colorModel instanceof ColorModelRange2) {
                return Optional.of(new Message("Numeric color model connected.",
                    "The color model can be applied to numeric columns.", MessageType.INFO));
            }
            return Optional.of(new Message("Unsupported color model connected.",
                "The connected color model is not supported by this node.", MessageType.ERROR));
        }

    }

    private static List<DataColumnSpec> computeColumnChoicesByColorModel(final NodeParametersInput context) {
        final var colorModelOpt = extractColorModel(context);
        if (colorModelOpt.isEmpty()) {
            return List.of();
        }
        final var colorModel = colorModelOpt.get();
        if (colorModel instanceof ColorModelNominal) {
            final var inputTableSpec = context.getInTableSpec(1);
            if (inputTableSpec.isEmpty()) {
                return List.of();
            }
            return inputTableSpec.get().stream().filter(colSpec -> colSpec.getDomain().hasValues()).toList();
        }
        if (colorModel instanceof ColorModelRange2) {
            return ColumnSelectionUtil.getDoubleColumns(context, 1);
        }
        return List.of();
    }

    private static Optional<ColorModel> extractColorModel(final NodeParametersInput context) {
        final var colorHandlerPortObject = context.getInPortObject(0);
        final var inputTableSpec = context.getInTableSpec(1);
        if (colorHandlerPortObject.isEmpty() || inputTableSpec.isEmpty()) {
            return Optional.empty();
        }
        return ColorDesignerApplyNodeFactory
            .extractColorModel(((ColorHandlerPortObject)colorHandlerPortObject.get()).getSpec());
    }

}
