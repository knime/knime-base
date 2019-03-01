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

package org.knime.base.data.filter.row.dialog.component.config;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.knime.base.data.filter.row.dialog.component.RowFilterComponent;
import org.knime.base.data.filter.row.dialog.component.RowFilterElementFactory;
import org.knime.base.data.filter.row.dialog.component.handler.TreeConditionHandler;
import org.knime.base.data.filter.row.dialog.component.handler.TreeGroupHandler;
import org.knime.base.data.filter.row.dialog.component.tree.TreePanel;

/**
 * Configuration object for the {@link TreePanel}.
 *
 * @author Viktor Buria
 */
public class TreePanelConfig extends AbstractPanelConfig {

    private final RowFilterElementFactory m_elementFactory;

    private Runnable m_treeChangedListener;

    private Runnable m_noSelectionListener;

    private Consumer<TreeGroupHandler> m_selectGroupConsumer;

    private Consumer<TreeConditionHandler> m_selectConditionConsumer;

    /**
     * Constructs an {@link TreePanelConfig}.
     *
     * @param elementFactory the {@link RowFilterElementFactory} to create groups and conditions for the
     *            {@link RowFilterComponent}.
     */
    public TreePanelConfig(final RowFilterElementFactory elementFactory) {
        m_elementFactory = Objects.requireNonNull(elementFactory, "elementFactory");
    }

    /**
     * Gets the factory to create groups and conditions for the {@link RowFilterComponent}.
     *
     * @return the {@link RowFilterElementFactory}
     */
    public RowFilterElementFactory getElementFactory() {
        return m_elementFactory;
    }

    /**
     * Gest the tree structure has changed event listener.
     *
     * @return the {@link Runnable} object
     */
    public Optional<Runnable> getTreeChangedListener() {
        return Optional.ofNullable(m_treeChangedListener);
    }

    /**
     * Sets the tree structure has changed event listener.
     *
     * @param listener the {@link Runnable} object
     */
    public void setTreeChangedListener(final Runnable listener) {
        m_treeChangedListener = listener;
    }

    /**
     * Gets the "no selected element" event listener.
     *
     * @return the {@link Runnable}
     */
    public Optional<Runnable> getNoSelectionListener() {
        return Optional.ofNullable(m_noSelectionListener);
    }

    /**
     * Sets the "no selected element" event listener.
     *
     * @param listener the {@link Runnable} object
     */
    public void setNoSelectionListener(final Runnable listener) {
        m_noSelectionListener = listener;
    }

    /**
     * Gets the "group was selected" event listener.
     *
     * @return the group {@link Consumer}
     */
    public Optional<Consumer<TreeGroupHandler>> getSelectGroupListener() {
        return Optional.ofNullable(m_selectGroupConsumer);
    }

    /**
     * Sets the "group was selected" event listener.
     *
     * @param consumer the group {@link Consumer}
     */
    public void setSelectGroupListener(final Consumer<TreeGroupHandler> consumer) {
        m_selectGroupConsumer = consumer;
    }

    /**
     * Gets the "condition was selected" event listener.
     *
     * @return the condition {@link Consumer}
     */
    public Optional<Consumer<TreeConditionHandler>> getSelectConditionListener() {
        return Optional.ofNullable(m_selectConditionConsumer);
    }

    /**
     * Sets the "condition was selected" event listener.
     *
     * @param consumer the condition {@link Consumer}
     */
    public void setSelectConditionListener(final Consumer<TreeConditionHandler> consumer) {
        m_selectConditionConsumer = consumer;
    }

}
