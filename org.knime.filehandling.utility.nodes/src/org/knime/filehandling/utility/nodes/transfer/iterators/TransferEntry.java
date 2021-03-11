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
 *   Feb 24, 2021 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.utility.nodes.transfer.iterators;

import java.io.IOException;
import java.util.List;

import org.knime.filehandling.core.connections.FSPath;

/**
 * A transfer entry encodes a {@link TransferPair} that must be copied or moved. It consists of a source destination
 * pair and in case this pair refers to a folder all files and folders that have to be additionally copied are
 * accessible via the {@link #getPathsToCopy()} method.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public interface TransferEntry {

    /**
     * Returns the source {@link FSPath}.
     *
     * @return the source path
     */
    FSPath getSource();

    /**
     * The source destination {@link TransferPair}.
     *
     * @return the source destination pair
     * @throws IOException - If something goes wrong while constructing this pair
     */
    TransferPair getSrcDestPair() throws IOException;

    /**
     * Returns the list of {@link TransferPair}s. This list must not contain the {@link #getSrcDestPair()}. It must be
     * empty if the {@link #getSrcDestPair()} refers to files and otherwise contain all files/folder
     * {@link TransferPair}s that need to be additionally transfered.
     *
     * @return the {@link TransferPair}s that have to be additionally copied/moved
     * @throws IOException - If something went wrong while compiling this list
     */
    List<TransferPair> getPathsToCopy() throws IOException;

}
