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
 *   11 Mar 2026 (Robin Gerling, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.BaseGradientReference;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ColorGradientWrapper;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.CustomGradientReference;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.StopValueColor;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ValueScale;
import org.knime.base.node.viz.property.color.ColorGradientDesignerNodeParameters.ValueScaleReference;
import org.knime.core.data.property.ColorGradient;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ColorPreview;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ColorPreview.Gradient;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;

/**
 * State provider for the color gradient preview in the color gradient designer dialog. Computes the gradient to be
 * displayed based on the selected base gradient, the custom gradient stops and colors, and the value scale. Invalid or
 * non-ascending custom gradient stops are filtered out, and the remaining stops are normalized to a 0-100 range for
 * display purposes. If the base gradient is not custom, the corresponding predefined gradient colors are used directly.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class ColorGradientPreviewProvider implements StateProvider<ColorPreview.Gradient> {

    private Supplier<ColorGradientWrapper> m_baseGradient;

    private Supplier<StopValueColor[]> m_customGradient;

    private Supplier<ValueScale> m_valueScale;

    @Override
    public void init(final StateProviderInitializer initializer) {
        initializer.computeBeforeOpenDialog();
        m_valueScale = initializer.computeFromValueSupplier(ValueScaleReference.class);
        m_baseGradient = initializer.computeFromValueSupplier(BaseGradientReference.class);
        m_customGradient = initializer.computeFromValueSupplier(CustomGradientReference.class);
    }

    @Override
    public Gradient computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
        final var baseGradient = m_baseGradient.get().getColorGradient();
        if (baseGradient != ColorGradient.CUSTOM) {
            return new Gradient(baseGradient.getGradientColors(), null);
        }

        final var customGradient = m_customGradient.get();
        if (customGradient.length == 0) {
            return new Gradient(new Color[0], new double[0]);
        }

        final var filteredGradient = filterNonAscendingStops(customGradient);
        final var visibleGradient = m_valueScale.get() == ValueScale.PERCENTAGE
            ? filterInvalidPercentageStops(filteredGradient) : filteredGradient;
        if (visibleGradient.isEmpty()) {
            return new Gradient(new Color[0], new double[0]);
        }

        final var customStops = visibleGradient.stream().mapToDouble(svc -> svc.m_stopValue).toArray();
        final var colors = visibleGradient.stream().map(svc -> svc.m_color).toArray(Color[]::new);
        final var stops = normalizeStops(customStops);
        return new Gradient(colors, stops);
    }

    private static List<StopValueColor> filterNonAscendingStops(final StopValueColor[] gradient) {
        final List<StopValueColor> filtered = new ArrayList<>(gradient.length);
        Double lastStopValue = null;

        for (final var stopValueColor : gradient) {
            final double stopValue = stopValueColor.m_stopValue;
            if (lastStopValue == null || stopValue > lastStopValue) {
                filtered.add(stopValueColor);
                lastStopValue = stopValue;
            }
        }

        return filtered;
    }

    private static List<StopValueColor> filterInvalidPercentageStops(final List<StopValueColor> gradient) {
        return gradient.stream().filter(svc -> svc.m_stopValue >= 0 && svc.m_stopValue <= 100).toList();
    }

    private static double[] normalizeStops(final double[] customStops) {
        final double minStop = customStops[0];
        final double maxStop = customStops[customStops.length - 1];
        final double range = maxStop - minStop;

        if (range == 0) {
            return new double[customStops.length];
        }

        return Arrays.stream(customStops).map(stop -> (stop - minStop) / range * 100).toArray();
    }
}
