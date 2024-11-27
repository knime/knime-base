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
 *   Aug 12, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.webui;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;

/**
 * Utility around accessing the file system port of a reader node
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public final class FileSystemPortConnectionUtil {

    private FileSystemPortConnectionUtil() {
        // Utility
    }

    /**
     * Utility method for retrieving the {@link FSConnection File System Connection} from the first file system port of
     * the specs of a given {@link DefaultNodeSettingsContext}.
     *
     * @param context the context from which to obtain the file system connection
     * @return the file system connection of the first file system port
     */
    public static Optional<FSConnection> getFileSystemConnection(final DefaultNodeSettingsContext context) {
        if (context == null) {
            return Optional.empty();
        }
        return getFirstFileSystemPort(context.getPortObjectSpecs())
            .flatMap(FileSystemPortObjectSpec::getFileSystemConnection);
    }

    /**
     * Utility method for checking whether the first file system port of the specs of a given
     * {@link DefaultNodeSettingsContext} provides a file system but there exists no connection yet.
     *
     * @param context the context from which to obtain the file system connection
     * @return whether the file system input port provides a file system but no connection
     */
    public static boolean hasEmptyFileSystemPort(final DefaultNodeSettingsContext context) {
        return hasFileSystemPort(context) && getFileSystemConnection(context).isEmpty();
    }

    /**
     * Utility method for checking whether the first file system port of the specs of a given
     * {@link DefaultNodeSettingsContext} provides a file system and a {@link FSConnection}.
     *
     * @param context the context from which to obtain the file system connection
     * @return whether the file system input port provides a file system and a connection
     */
    public static boolean hasFileSystemPortWithConnection(final DefaultNodeSettingsContext context) {
        return hasFileSystemPort(context) && getFileSystemConnection(context).isPresent();
    }

    private static Optional<FileSystemPortObjectSpec> getFirstFileSystemPort(final PortObjectSpec[] specs) {
        return Arrays.asList(specs).stream().filter(FileSystemPortObjectSpec.class::isInstance)
            .map(FileSystemPortObjectSpec.class::cast).findFirst();
    }

    /**
     * Utility method for checking whether the first file system port of the specs of a given
     * {@link DefaultNodeSettingsContext} provides a file system regardless of the state of the
     * connection.
     *
     * @param context the context from which to obtain the file system connection
     * @return whether the file system input port provides a file system
     */
    public static boolean hasFileSystemPort(final DefaultNodeSettingsContext context) {
        final var inPortTypes = context.getInPortTypes();
        return IntStream.range(0, inPortTypes.length)
            .anyMatch(i -> FileSystemPortObjectSpec.class.equals(inPortTypes[i].getPortObjectSpecClass()));

    }

    /**
     * Satisfied whenever a file system port exists in the input but no connection to the file system could be
     * established, i.e. when there is nothing connected, when the previous nodes are not executed or when the
     * connection is closed.
     *
     * This is not to be used on the file chooser widget directly, since it is disabled automatically by the framework.
     *
     * @author Paul Bärnreuther
     */
    public static final class ConnectedWithoutFileSystemSpec implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getConstant(ConnectedWithoutFileSystemSpec::applies);
        }

        static boolean applies(final DefaultNodeSettingsContext context) {
            return hasEmptyFileSystemPort(context);
        }
    }

}
