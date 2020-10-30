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
 *   Aug 14, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.selector;

import javax.swing.event.ChangeListener;

import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * A {@link TableTransformation} that allows to register {@link ChangeListener ChangeListeners} that are notified
 * whenever the model changes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> The type used to identify external data types
 */
public interface ObservableTransformationModelProvider<T> {

    /**
     * Returns the current {@link TableTransformation}
     *
     * @return the current {@link TableTransformation}
     */
    TableTransformation<T> getTransformationModel();

    /**
     * Adds the provided {@link ChangeListener}.
     *
     * @param listener to add
     */
    void addChangeListener(final ChangeListener listener);

    /**
     * Removes the provided {@link ChangeListener}.
     *
     * @param listener to remove
     */
    void removeChangeListener(final ChangeListener listener);

    /**
     * Updates the raw {@link TypedReaderTableSpec} i.e. before type mapping, renaming, filtering or reordering.
     *
     * @param rawSpec the raw {@link TypedReaderTableSpec}
     */
    void updateRawSpec(final RawSpec<T> rawSpec);

    /**
     * Adapts this instance so that behaves exactly the same as the provided {@link TableTransformation}.
     *
     * @param transformationModel to imitate
     */
    void load(final TableTransformation<T> transformationModel);

    /**
     * Enables or disables this instance, depending on the value of {@code enabled}.
     *
     * @param enabled {@code true} if the model should be enabled, {@code false} otherwise
     */
    void setEnabled(boolean enabled);

}
