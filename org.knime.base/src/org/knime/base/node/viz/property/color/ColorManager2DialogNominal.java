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
 * -------------------------------------------------------------------
 *
 * History
 *   09.02.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.knime.base.node.viz.property.color.ColorManager2NodeModel.PaletteOption;
import org.knime.core.data.DataCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A dialog panel used to set color for nominal values.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class ColorManager2DialogNominal extends JPanel {

    /** Used for 'new' values when dialog opens -- all black. */
    private static final String[] UNKNOWN_VALUE_PALETTE = new String[] {"#000000"};

    private static final long serialVersionUID = 1L;

    /** Keeps mapping from column to a 'map mapping data cell name to color'. */
    private final Map<String, Map<DataCell, ColorAttr>> m_map;

    /** Keeps the all possible column values. */
    private final JList<ColorManager2Icon> m_columnValues;

    /** list model for column values. */
    private final DefaultListModel<ColorManager2Icon> m_columnModel;

    private int m_alpha = 255;

    /**
     * Creates an empty nominal dialog.
     */
    ColorManager2DialogNominal() {
        super(new GridLayout());

        // map for key to color mapping
        m_map = new LinkedHashMap<String, Map<DataCell, ColorAttr>>();

        // create list for possible column values
        m_columnModel = new DefaultListModel<ColorManager2Icon>();
        m_columnValues = new JList<ColorManager2Icon>(m_columnModel);
        m_columnValues.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_columnValues.setCellRenderer(new ColorManager2IconRenderer());
        super.add(new JScrollPane(m_columnValues));
    }

    /**
     * Called if a new column is selected.
     *
     * @param column the new selected column
     * @return <code>true</code>, if the call caused any changes
     */
    boolean select(final String column) {
        m_columnModel.removeAllElements();
        Map<DataCell, ColorAttr> map = m_map.get(column);
        boolean flag;
        if (map == null) {
            m_columnModel.removeAllElements();
            m_columnValues.setEnabled(false);
            flag = false;
        } else {
            m_columnValues.setEnabled(true);
            for (DataCell cell : map.keySet()) {
                assert cell != null;
                ColorAttr color = map.get(cell);
                assert color != null;
                m_columnModel.addElement(new ColorManager2Icon(cell, color.getColor()));
            }
            flag = true;
        }
        super.validate();
        super.repaint();
        return flag;
    }

    /**
     * Select new color for the selected attribute value (selection = selected element in the JList)
     *
     * @param column the selected column
     * @param color the new color
     * @return true if a change is made
     */
    boolean update(final String column, final ColorAttr color) {
        ColorManager2Icon icon = m_columnValues.getSelectedValue();
        if (icon != null) {
            Map<DataCell, ColorAttr> map = m_map.get(column);
            Color oldColor = icon.getColor();
            if (!Objects.equals(oldColor, color.getColor())) {
                map.put(icon.getCell(), color);
                icon.setColor(color.getColor());
                super.validate();
                super.repaint();
                return true;
            }
        }
        return false;
    }

    /**
     * Apply new colors of a given palette of the selected column.
     *
     * @param column the selected column
     * @param palette the new palette
     */
    void updateWithPalette(final String column, final String[] palette) {
        Map<DataCell, ColorAttr> map = m_map.get(column);
        int i = 0;
        ColorAttr color;
        for (Enumeration<ColorManager2Icon> enu = m_columnModel.elements(); enu.hasMoreElements();) {
            if (i >= palette.length) {
                i = 0;
            }
            ColorManager2Icon icon = enu.nextElement();
            color = ColorAttr.getInstance(Color.decode(palette[i]));
            icon.setColor(color.getColor());
            map.put(icon.getCell(), color);
            i++;
        }

        super.validate();
        super.repaint();

    }

    /**
     * Adds the given set of possible values to the internal structure by the given column name.
     *
     * @param column the column name
     * @param set the set of possible values for this column
     */
    void add(final String column, final Set<DataCell> set, final PaletteOption po) {
        if (set != null && !set.isEmpty()) {
            Map<DataCell, ColorAttr> map = createColorMapping(set, po);
            m_map.put(column, map);
        }
    }

    /**
     * Create default color mapping for the given set of possible <code>DataCell</code> values.
     *
     * @param set possible values
     * @return a map of possible value to color
     */
    static final Map<DataCell, ColorAttr> createColorMapping(final Set<DataCell> set, final PaletteOption po) {
        if (set == null) {
            return Collections.emptyMap();
        }
        Map<DataCell, ColorAttr> map = new LinkedHashMap<DataCell, ColorAttr>();
        int idx = 0;
        String[] palette = null;
        palette = switch (po) {
            case SET1 -> PaletteOption.SET1.getPalette();
            case SET2 -> PaletteOption.SET2.getPalette();
            case SET3 -> PaletteOption.SET3.getPalette();
            default -> UNKNOWN_VALUE_PALETTE;
        };
        List<DataCell> cellsSorted = new ArrayList<>(set);
        Collections.sort(cellsSorted, (a, b) -> {
            return String.CASE_INSENSITIVE_ORDER.compare(a.toString(), b.toString());
        });
        for (DataCell cell : cellsSorted) {
            if (idx >= palette.length) {
                idx = 0;
            }
            Color color = Color.decode(palette[idx]);
            map.put(cell, ColorAttr.getInstance(color));
            idx++;
        }
        return map;
    }

    /**
     * Removes all elements for the internal map.
     */
    void removeAllElements() {
        m_map.clear();
        m_columnModel.removeAllElements();
        this.setEnabled(false);
    }

    /**
     * Save settings that are the current color settings.
     *
     * @param settings to write to
     * @throws InvalidSettingsException if no nominal value are defined on the selected column
     */
    void saveSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        int len = m_columnModel.getSize();
        if (len > 0) {
            DataCell[] vals = new DataCell[len];
            for (int i = 0; i < m_columnModel.getSize(); i++) {
                ColorManager2Icon icon = m_columnModel.getElementAt(i);
                vals[i] = icon.getCell();
                Color c = icon.getColor();
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), m_alpha);
                settings.addInt(vals[i].toString(), c.getRGB());
            }
            settings.addDataCellArray(ColorManager2NodeModel.VALUES, vals);
        } else {
            throw new InvalidSettingsException(
                "Make sure the selected column has nominal values defined within the domain.");
        }
    }

    /**
     * Reads the color settings for the given column.
     *
     * @param settings to read from
     * @param column the selected column
     * @throws InvalidSettingsException
     */
    void loadSettings(final NodeSettingsRO settings, final String column) {
        if (column == null) {
            return;
        }
        m_alpha = 255; // 255 by default -- unless it's a custom palette and then the alpha of any assignment will work
        DataCell[] vals = settings.getDataCellArray(ColorManager2NodeModel.VALUES, (DataCell[])null);
        if (vals == null) {
            return;
        }
        Map<DataCell, ColorAttr> map = m_map.get(column);
        if (map == null) {
            return;
        }
        for (int i = 0; i < vals.length; i++) {
            if (map.containsKey(vals[i])) {
                Color dftColor = map.get(vals[i]).getColor();
                int c = settings.getInt(vals[i].toString(), dftColor.getRGB());
                Color color = new Color(c, true);
                m_alpha = color.getAlpha();
                color = new Color(color.getRGB(), false);
                map.put(vals[i], ColorAttr.getInstance(color));
            }
        }
    }

    /**
     * @return intermediate alpha value as read from the current settings
     */
    final int getAlpha() {
        return m_alpha;
    }

    /**
     * @param alpha the new alpha value as set by the alpha color panel
     */
    final void setAlpha(final int alpha) {
        m_alpha = alpha;
    }
}
