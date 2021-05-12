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
 *   Dec 17, 2020 (Simon Schmid, KNIME GmbH, Konstanz, Germany): created
 *   May 12, 2021 (Lars Schweikardt, KNIME GmbH, Konstanz, Germany): backport to 4.3.3.
 */
package org.knime.filehandling.core.util;

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.connections.DefaultFSLocationSpec;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.data.location.FSLocationValue;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * Utility class for {@link FSLocation} columns.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
public final class FSLocationColumnUtils {

    private FSLocationColumnUtils() {
        // hide constructor for Utils class
    }

    /**
     * Validates the incoming {@link DataColumnSpec} by checking its type to be compatible with {@link FSLocationValue}
     * and its meta data to contain compatible {@link DefaultFSLocationSpec}s. Throws an error if incompatible or
     * returns a warning if the node might fail during execution.
     *
     * @param fsLocationColSpec the {@link DataColumnSpec} of the {@link FSLocation} column
     * @param fsSpec the {@link FileSystemPortObjectSpec}, {@code null} if unconnected
     * @return an empty {@link Optional} if everything is fine or an {@link Optional} containing a warning message that
     *         should be set by the node
     * @throws InvalidSettingsException if the incoming {@link DataColumnSpec} is not compatible with
     *             {@link FSLocationValue} or its meta data is not compatible
     */
    public static Optional<String> validateFSLocationColumn(final DataColumnSpec fsLocationColSpec,
        final FileSystemPortObjectSpec fsSpec) throws InvalidSettingsException {

        final String pathColName = fsLocationColSpec.getName();

        validateColumnType(fsLocationColSpec, pathColName);

        final FSLocationValueMetaData metaData = validateAndGetMetaData(fsLocationColSpec);

        // fsSpec is null if the node does not have a fs connection
        if (fsSpec != null) {
            return validateConnectedFSMetaData(fsSpec, metaData, pathColName);
        } else {
            return validateUnconnectedFSMetaData(metaData, pathColName);
        }
    }

    private static void validateColumnType(final DataColumnSpec fsLocationColSpec, final String pathColName)
        throws InvalidSettingsException {
        if (!fsLocationColSpec.getType().isCompatible(FSLocationValue.class)) {
            throw new InvalidSettingsException(
                String.format("The selected column '%s' has the wrong type.", pathColName));
        }
    }

    /**
     * Validates and returns the meta data.
     *
     * @param fsLocationColSpec the column containing the {@link FSLocationValueMetaData}.
     * @return the columns {@link FSLocationValueMetaData}
     */
    public static FSLocationValueMetaData validateAndGetMetaData(final DataColumnSpec fsLocationColSpec) {
        final String pathColName = fsLocationColSpec.getName();
        return fsLocationColSpec.getMetaDataOfType(FSLocationValueMetaData.class)
            .orElseThrow(() -> new IllegalStateException(
                String.format("Path column '%s' without meta data encountered.", pathColName)));
    }

    private static Optional<String> validateUnconnectedFSMetaData(final FSLocationValueMetaData metaData,
        final String pathColName) throws InvalidSettingsException {
        if (metaData.getFSCategory() == FSCategory.CONNECTED) {
            throw new InvalidSettingsException(String.format(
                "The selected column '%s' references a connected file system (%s). Please add the missing file "
                    + "system connection port.", pathColName,  metaData.getFileSystemSpecifier().orElse("")));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<String> validateConnectedFSMetaData(final FileSystemPortObjectSpec fsSpec,
        final FSLocationValueMetaData metaData, final String pathColName) {
        if (!FSLocationSpec.areEqual(fsSpec.getFSLocationSpec(),
            new DefaultFSLocationSpec(metaData.getFSCategory(), metaData.getFileSystemSpecifier().orElse(null)))) {
            return Optional.of(String.format(
                "The selected column '%s' seems to contain a path referencing a"
                    + " different file system than the one at the input port. Such paths will be"
                    + " resolved against the file system at the input port (%s).",
                pathColName, fsSpec.getFSLocationSpec().getFileSystemSpecifier().orElse("")));
        } else {
            return Optional.empty();
        }
    }
}
