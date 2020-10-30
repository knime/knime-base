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
 *   Apr 6, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.util;

import java.awt.GridBagConstraints;
import java.awt.Insets;

import org.knime.core.node.util.CheckUtils;

/**
 * Convenience builder for {@link GridBagConstraints} that allows method-chaining.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class GBCBuilder {

    private final GridBagConstraints m_gbc = new GridBagConstraints();

    /**
     * Constructor.
     */
    public GBCBuilder() {

    }

    /**
     * Constructor.
     *
     * @param insets the insets to use
     */
    public GBCBuilder(final Insets insets) {
        m_gbc.insets = insets;
    }

    /**
     * Returns {@link GridBagConstraints} reflecting the current state of this builder.
     *
     * @return {@link GridBagConstraints} reflecting the current state of this builder
     */
    public GridBagConstraints build() {
        return (GridBagConstraints)m_gbc.clone();
    }

    /**
     * Sets the {@link GridBagConstraints#gridx} and {@link GridBagConstraints#gridy} properties to the provided
     * <b>x</b> and <b>y</b> coordinates.
     *
     * @param x coordinate
     * @param y coordinate
     * @return this builder
     */
    public GBCBuilder pos(final int x, final int y) {
        setX(x);
        setY(y);
        return this;
    }

    /**
     * Resets the {@link GridBagConstraints#gridx} and {@link GridBagConstraints#gridy} properties to {@code 0}.
     *
     * @return this builder
     */
    public GBCBuilder resetPos() {
        return resetX().resetY();
    }

    /**
     * Sets the {@link GridBagConstraints#gridx} property to <b>x</b>.
     *
     * @param x the horizontal position in the grid
     * @return this builder
     */
    public GBCBuilder setX(final int x) {
        m_gbc.gridx = x;
        return this;
    }

    /**
     * Increments the {@link GridBagConstraints#gridx} property by one.
     *
     * @return this builder
     */
    public GBCBuilder incX() {
        return incX(1);
    }

    /**
     * Increments the {@link GridBagConstraints#gridx} property by the provided <b>increment</b>.
     *
     * @param increment to add to the x position in the grid
     * @return this builder
     */
    public GBCBuilder incX(final int increment) {
        m_gbc.gridx += increment;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#gridx} property to zero.
     *
     * @return this builder
     */
    public GBCBuilder resetX() {
        m_gbc.gridx = 0;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#gridy} property to <b>y</b>.
     *
     * @param y the vertical position in the grid
     * @return this builder
     */
    public GBCBuilder setY(final int y) {
        m_gbc.gridy = y;
        return this;
    }

    /**
     * Increments the {@link GridBagConstraints#gridy} property by one.
     *
     * @return this builder
     */
    public GBCBuilder incY() {
        m_gbc.gridy++;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#gridy} property by zero.
     *
     * @return this builder
     */
    public GBCBuilder resetY() {
        m_gbc.gridy = 0;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#weightx} property to <b>weight</b>.
     *
     * @param weight the weight for horizontal resizing
     * @return this builder
     */
    public GBCBuilder setWeightX(final double weight) {
        m_gbc.weightx = weight;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#weightx} property to <b>weightX</b> and the {@link GridBagConstraints#weighty} to <b>weightY</b>.
     *
     * @param weightX the horizontal weight
     * @param weightY the vertical weight
     * @return this builder
     */
    public GBCBuilder weight(final double weightX, final double weightY) {
        return setWeightX(weightX).setWeightY(weightY);
    }

    /**
     * Sets the {@link GridBagConstraints#weighty} property to <b>weight</b>.
     *
     * @param weight the weight for vertical resizing
     * @return this builder
     */
    public GBCBuilder setWeightY(final double weight) {
        m_gbc.weighty = weight;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#gridwidth} property to <b>width</b>.
     *
     * @param width of the current cell
     * @return this builder
     */
    public GBCBuilder setWidth(final int width) {
        m_gbc.gridwidth = width;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#gridwidth} property to <b>height</b>.
     *
     * @param height of the current cell
     * @return this builder
     */
    public GBCBuilder setHeight(final int height) {
        m_gbc.gridheight = height;
        return this;
    }


    /**
     * Sets the {@link GridBagConstraints#gridwidth} property to {@link GridBagConstraints#REMAINDER} i.e. a cell will
     * now span all remaining columns of the grid.
     *
     * @return this builder
     */
    public GBCBuilder widthRemainder() {
        return setWidth(GridBagConstraints.REMAINDER);
    }

    /**
     * Sets the {@link GridBagConstraints#fill} property to {@link GridBagConstraints#HORIZONTAL}.
     *
     * @return this builder
     */
    public GBCBuilder fillHorizontal() {
        return fill(GridBagConstraints.HORIZONTAL);
    }

    /**
     * Sets the {@link GridBagConstraints#fill} property to {@link GridBagConstraints#VERTICAL}.
     *
     * @return this builder
     */
    public GBCBuilder fillVertical() {
        return fill(GridBagConstraints.VERTICAL);
    }

    /**
     * Sets the {@link GridBagConstraints#fill} property to {@link GridBagConstraints#BOTH}.
     *
     * @return this builder
     */
    public GBCBuilder fillBoth() {
        return fill(GridBagConstraints.BOTH);
    }

    /**
     * Sets the {@link GridBagConstraints#fill} property to {@link GridBagConstraints#NONE}.
     *
     * @return this builder
     */
    public GBCBuilder fillNone() {
        return fill(GridBagConstraints.NONE);
    }

    private GBCBuilder fill(final int fill) {
        m_gbc.fill = fill;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#anchor} property to {@link GridBagConstraints#LINE_START}.
     *
     * @return this builder
     */
    public GBCBuilder anchorLineStart() {
        m_gbc.anchor = GridBagConstraints.LINE_START;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#anchor} property to {@link GridBagConstraints#PAGE_START}.
     *
     * @return this builder
     */
    public GBCBuilder anchorPageStart() {
        m_gbc.anchor = GridBagConstraints.PAGE_START;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#anchor} property to {@link GridBagConstraints#FIRST_LINE_START}.
     *
     * @return this builder
     */
    public GBCBuilder anchorFirstLineStart() {
        m_gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#anchor} property to {@link GridBagConstraints#CENTER}.
     *
     * @return this builder
     */
    public GBCBuilder anchorCenter() {
        m_gbc.anchor = GridBagConstraints.CENTER;
        return this;
    }

    /**
     * Set the {@link GridBagConstraints#anchor} property to {@link GridBagConstraints#WEST}.
     *
     * @return this builder
     */
    public GBCBuilder anchorWest() {
        m_gbc.anchor = GridBagConstraints.WEST;
        return this;
    }

    /**
     * Set the {@link GridBagConstraints#anchor} property to {@link GridBagConstraints#LINE_END}.
     *
     * @return this builder
     */
    public GBCBuilder anchorLineEnd() {
        m_gbc.anchor = GridBagConstraints.LINE_END;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#insets} property to <b>insets</b>.
     *
     * @param insets the new {@link Insets}
     * @return this builder
     */
    public GBCBuilder setInsets(final Insets insets) {
        m_gbc.insets = CheckUtils.checkArgumentNotNull(insets, "The insets must not be null.");
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#insets} property to the provided values.
     *
     * @param top the inset from the top
     * @param left the inset from the left
     * @param bottom the inset from the bottom
     * @param right the inset from the right
     * @return this builder
     */
    public GBCBuilder insets(final int top, final int left, final int bottom, final int right) {
        return setInsets(new Insets(top, left, bottom, right));
    }

    /**
     * Sets the right padding of the {@link GridBagConstraints#insets} property to the provided value.
     *
     * @param insetRight the external padding on the right side
     * @return this builder
     */
    public GBCBuilder insetRight(final int insetRight) {
        final Insets oldInsets = m_gbc.insets;
        m_gbc.insets = new Insets(oldInsets.top, oldInsets.left, oldInsets.bottom, insetRight);
        return this;
    }

    /**
     * Sets the left external padding of the {@link GridBagConstraints#insets} property to the provided value.
     *
     * @param insetLeft the external padding on the left side
     * @return this builder
     */
    public GBCBuilder insetLeft(final int insetLeft) {
        final Insets oldInsets = m_gbc.insets;
        m_gbc.insets = new Insets(oldInsets.top, insetLeft, oldInsets.bottom, oldInsets.right);
        return this;
    }

    /**
     * Sets the top external padding of the {@link GridBagConstraints#insets} property to the provided value.
     *
     * @param insetTop the external padding on the top
     * @return this builder
     */
    public GBCBuilder insetTop(final int insetTop) {
        final Insets oldInsets = m_gbc.insets;
        m_gbc.insets = new Insets(insetTop, oldInsets.left, oldInsets.bottom, oldInsets.right);
        return this;
    }

    /**
     * Sets the bottom external padding of the {@link GridBagConstraints#insets} property to the provided value.
     *
     * @param insetBottom the external padding on the bottom
     * @return this builder
     */
    public GBCBuilder insetBottom(final int insetBottom) {
        final Insets oldInsets = m_gbc.insets;
        m_gbc.insets = new Insets(oldInsets.top, oldInsets.left, insetBottom, oldInsets.right);
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#ipadx} property to the provided value.
     *
     * @param ipadX the ipadX value
     * @return this builder
     */
    public GBCBuilder ipadX(final int ipadX) {
        m_gbc.ipadx = ipadX;
        return this;
    }

    /**
     * Sets the {@link GridBagConstraints#ipady} property to the provided value.
     *
     * @param ipadY the ipadY value
     * @return this builder
     */
    public GBCBuilder ipadY(final int ipadY) {
        m_gbc.ipady = ipadY;
        return this;
    }

}
