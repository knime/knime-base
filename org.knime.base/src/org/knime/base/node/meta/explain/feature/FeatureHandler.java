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
 *   01.04.2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import org.knime.core.data.DataCell;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
interface FeatureHandler {

    /**
     * @param cell
     * @throws MissingValueException if this handler can't deal with missing values and <b>cell</b> is missing
     */
    void setOriginal(final DataCell cell) throws MissingValueException;

    /**
     * @param cell
     * @throws MissingValueException if this handler can't deal with missing values and <b>cell</b> is missing
     */
    void setSampled(final DataCell cell) throws MissingValueException;

    /**
     * Note that <b>idx</b> has to be the local, feature idx i.e. if this handler's current original cell represents 3
     * features and the second should be replaced, idx has to be 1.
     *
     * @param idx feature that should be replaced
     */
    void markForReplacement(final int idx);

    /**
     *
     */
    void reset();

    /**
     * Resets the replacement state but keeps the original and sampled cells
     */
    void resetReplaceState();

    /**
     * Replaces the original cell with the features marked for replacement replaced by their values from the sampled
     * cell.
     *
     * @return the perturbed cell
     */
    DataCell createReplaced();
}