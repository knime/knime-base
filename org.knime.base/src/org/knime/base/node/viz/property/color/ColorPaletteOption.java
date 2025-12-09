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
 *   1 Dec 2025 (Robin Gerling): created
 */
package org.knime.base.node.viz.property.color;

import java.util.Arrays;

import org.knime.core.data.property.ColorAttr;
import org.knime.node.parameters.widget.choices.Label;

/**
 * The possible color palettes to choose from in the {@link ColorPaletteDesignerNodeFactory}.
 *
 * @author Robin Gerling
 */
enum ColorPaletteOption {

        @Label(value = "Custom", description = "Define a personalized color palette.")
        CUSTOM(null),

        /** See ColorBrewer, https://colorbrewer2.org/ */
        @Label("Brewer Set 1 (9 colors)")
        BREWER_SET1_COLORS9(new String[]{"#e41a1c", "#377eb8", "#4daf4a", "#984ea3", "#ff7f00", "#ffff33", "#a65628",
            "#f781bf", "#999999"}),

        @Label("Brewer Set 2 (8 colors)")
        BREWER_SET2_COLORS8(
            new String[]{"#66c2a5", "#fc8d62", "#8da0cb", "#e78ac3", "#a6d854", "#ffd92f", "#e5c494", "#b3b3b3"}),

        @Label("Brewer Set 3 (12 colors)")
        BREWER_SET3_COLORS12(new String[]{"#8dd3c7", "#ffffb3", "#bebada", "#fb8072", "#80b1d3", "#fdb462", "#b3de69",
            "#fccde5", "#d9d9d9", "#bc80bd", "#ccebc5", "#ffed6f"}),

        @Label("Brewer Pastel1 (9 colors)")
        BREWER_PASTEL1_COLORS9(new String[]{"#fbb4ae", "#b3cde3", "#ccebc5", "#decbe4", "#fed9a6", "#ffffcc", "#e5d8bd",
            "#fddaec", "#f2f2f2"}),

        @Label("Brewer Pastel 2 (8 colors)")
        BREWER_PASTEL2_COLORS8(
            new String[]{"#b3e2cd", "#fdcdac", "#cbd5e8", "#f4cae4", "#e6f5c9", "#fff2ae", "#f1e2cc", "#cccccc"}),

        @Label("Brewer Paired (12 colors)")
        BREWER_PAIRED_COLORS12(new String[]{"#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", "#e31a1c", "#fdbf6f",
            "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99", "#b15928"}),

        @Label("Brewer Accent (8 colors)")
        BREWER_ACCENT_COLORS8(
            new String[]{"#7fc97f", "#beaed4", "#fdc086", "#ffff99", "#386cb0", "#f0027f", "#bf5b17", "#666666"}),

        @Label("Brewer Dark 2 (8 colors)")
        BREWER_DARK2_COLORS_8(
            new String[]{"#1b9e77", "#d95f02", "#7570b3", "#e7298a", "#66a61e", "#e6ab02", "#a6761d", "#666666"}),

        /** See Color Universal Design, https://jfly.uni-koeln.de/color/. */
        @Label(value = "Color Universal Design (7 colors)", description = "(colorblind safe)")
        COLOR_UNIVERSAL_DESIGN_COLORS7(
            new String[]{"#E69F00", "#56B4E9", "#009E73", "#F0E442", "#0072B2", "#D55E00", "#CC79A7"});

    private final ColorAttr[] m_paletteAsColorAttr;

    ColorPaletteOption(final String[] palette) {
        m_paletteAsColorAttr = palette == null ? null
            : Arrays.stream(palette).map(ColorPaletteDesignerNodeFactory::hexToColorAttr).toArray(ColorAttr[]::new);
    }

    /**
     * @return the paletteAsColorAttr (or <code>null</code> for {@link ColorPaletteOption#CUSTOM}).
     */
    ColorAttr[] getPaletteAsColorAttr() {
        return m_paletteAsColorAttr;
    }
}
