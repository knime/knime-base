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
 */
package org.knime.filehandling.core.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
     * @param fsLocationColSpec the {@link DataColumnSpec} of the fs location column
     * @param fsSpec the {@link FileSystemPortObjectSpec}, {@code null} if unconnected
     * @return an empty {@link Optional} if everything is fine or an {@link Optional} containing a warning message that
     *         should be set by the node
     * @throws InvalidSettingsException if the incoming {@link DataColumnSpec} is not compatible with
     *             {@link FSLocationValue} or its meta data is not compatible
     */
    public static Optional<String> validateFSLocationColumn(final DataColumnSpec fsLocationColSpec,
        final FileSystemPortObjectSpec fsSpec) throws InvalidSettingsException {
        final String pathColName = fsLocationColSpec.getName();
        // check column type
        validateColumnType(fsLocationColSpec, pathColName);
        // check and get meta data
        final FSLocationValueMetaData metaData = validateAndGetMetaData(fsLocationColSpec);

        final Set<DefaultFSLocationSpec> fsLocationSpecs = metaData.getFSLocationSpecs();
        // fsSpec is null if the node does not have a fs connection
        if (fsSpec != null) {
            return validateConnectedFSMetaData(fsSpec, pathColName, fsLocationSpecs);
        } else {
            return validateUnconnectedFSMetaData(pathColName, fsLocationSpecs);
        }
    }

    private static Optional<String> validateConnectedFSMetaData(final FileSystemPortObjectSpec fsSpec,
        final String pathColName, final Set<DefaultFSLocationSpec> fsLocationSpecs) {
        final Optional<String> fsSpecifier = fsSpec.getFSLocationSpec().getFileSystemSpecifier();
        // return a warning if an fs is connected but the meta data contains a fs location spec with a different fs
        if (fsLocationSpecs.stream().anyMatch(e -> !FSLocationSpec.areEqual(e, fsSpec.getFSLocationSpec()))) {
            return Optional.of(String.format(
                "The selected column '%s' seems to contain a path referencing a "
                    + " different file system than the one at the input port. Such paths will be"
                    + " resolved against the file system at the input port (%s).",
                pathColName, fsSpecifier.orElse("")));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<String> validateUnconnectedFSMetaData(final String pathColName,
        final Set<DefaultFSLocationSpec> fsLocationSpecs) throws InvalidSettingsException {
        final Map<Boolean, List<DefaultFSLocationSpec>> mapOfConnectedAndUnconnectedFS =
            fsLocationSpecs.stream().collect(Collectors.partitioningBy(e -> e.getFSCategory() == FSCategory.CONNECTED));
        // fail if the meta data contains solely connected fs
        checkMetaDataContainingOnlyConnectedFS(pathColName, mapOfConnectedAndUnconnectedFS);
        // return a warning if the meta data contains both unconnected and connected fs
        if (!mapOfConnectedAndUnconnectedFS.get(Boolean.TRUE).isEmpty()) {
            return Optional.of(String.format(
                "The selected column '%s' seems to contain a path referencing a file"
                    + " system that requires to be connected (%s). The node will fail during execution in such case.",
                pathColName,
                mapOfConnectedAndUnconnectedFS.get(Boolean.TRUE).get(0).getFileSystemSpecifier().orElse("")));
        } else {
            return Optional.empty();
        }
    }

    private static void checkMetaDataContainingOnlyConnectedFS(final String pathColName,
        final Map<Boolean, List<DefaultFSLocationSpec>> mapOfConnectedAndUnconnectedFS)
        throws InvalidSettingsException {
        if (mapOfConnectedAndUnconnectedFS.get(Boolean.FALSE).isEmpty()) {
            throw new InvalidSettingsException(String.format(
                "The selected column '%s' references a connected file system (%s). Please add the missing file "
                    + "system connection port.",
                pathColName,
                mapOfConnectedAndUnconnectedFS.get(Boolean.TRUE).get(0).getFileSystemSpecifier().orElse("")));
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

    private static void validateColumnType(final DataColumnSpec fsLocationColSpec, final String pathColName)
        throws InvalidSettingsException {
        if (!fsLocationColSpec.getType().isCompatible(FSLocationValue.class)) {
            throw new InvalidSettingsException(
                String.format("The selected column '%s' has the wrong type.", pathColName));
        }
    }

}
