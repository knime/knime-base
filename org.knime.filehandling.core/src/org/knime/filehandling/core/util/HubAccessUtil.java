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
 *   Sep 22, 2022 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.util.auth.Authenticator;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.SpaceAware;
import org.knime.filehandling.core.connections.SpaceAware.Space;

/**
 * Utility class providing different method for fetching data about Hub Spaces.
 *
 * @author Alexander Bondaletov
 */
public final class HubAccessUtil {
    private HubAccessUtil() {
    }

    private static HubSpaceLocationInfo getLocationInfoByWorkflowContext() {
        if (WorkflowContextUtil.isCurrentWorkflowOnHub()) {
            return (HubSpaceLocationInfo)WorkflowContextUtil.getWorkflowContextV2().getLocationInfo();
        } else {
            throw new IllegalStateException("The workflow is not located on the Hub");
        }
    }

    private static FSConnection getFSConnectionByWorkflowContext() {
        var locInfo = getLocationInfoByWorkflowContext();
        return DefaultFSConnectionFactory.createHubConnection(locInfo.getRepositoryAddress(),
            locInfo.getAuthenticator());
    }

    /**
     * @return a new {@link HubAccess} instance that connects to a KNIME Hub by using information from the current
     *         workflow context.
     */
    public static HubAccess createHubAccessViaWorkflowContext() {
        return new HubAccess() {
            @Override
            protected FSConnection createFSConnection() {
                return getFSConnectionByWorkflowContext();
            }
        };
    }

    /**
     * @param repositoryAddress URI of the Hub repository/catalog.
     * @param authenticator An {@link Authenticator} to use to connect.
     * @return a new {@link HubAccess} instance that connects to the KNIME Hub at the given URI.
     */
    public static HubAccess createHubAccessViaRepositoryAddress(final URI repositoryAddress,
        final Authenticator authenticator) {
        return new HubAccess() {
            @Override
            protected FSConnection createFSConnection() {
                return DefaultFSConnectionFactory.createHubConnection(repositoryAddress, authenticator);
            }
        };
    }

    /**
     * Stateless class that encapsulates access to a KNIME Hub instance. Instances of this class do not keep any
     * resources (connections etc) open, therefore this class does not have to implement {@link Closeable}.
     *
     * @author Alexander Bondaletov
     */
    public abstract static class HubAccess {

        /**
         * This method is called by the {@link #listSpaces()} and the other methods in this class to obtain an
         * {@link FSConnection}. The connection returned here will be closed immediately by the calling method, so it is
         * important to always create a new {@link FSConnection}.
         *
         * @return a newly created {@link FSConnection} instance.
         */
        protected abstract FSConnection createFSConnection();

        /**
         * @return The list of spaces for the current user.
         * @throws IOException
         */
        @SuppressWarnings("resource")
        public List<Space> listSpaces() throws IOException {
            try (var connection = createFSConnection()) {
                final var fileSystemProvider = connection.getFileSystem().provider();

                if (fileSystemProvider instanceof SpaceAware) {
                    var provider = (SpaceAware)fileSystemProvider;
                    return provider.getSpaces();
                } else {
                    throw new IllegalStateException("Chosen file system does not provide access to Hub Spaces");
                }
            }
        }

        /**
         * Lists all Hub Spaces owned by the given account, which are accessible by the current user.
         *
         * @param accountNameOrID The account name ("joe.blank") or ID ("user:eda5b6ca-a8b8-46a7-86b2-9b27a24cc972").
         * @return the list of {@link Space}s.
         * @throws IOException
         */
        @SuppressWarnings("resource")
        public List<Space> listSpacesForAccount(final String accountNameOrID) throws IOException {
            try (var connection = createFSConnection()) {
                final var fileSystemProvider = connection.getFileSystem().provider();

                if (fileSystemProvider instanceof SpaceAware) {
                    var provider = (SpaceAware)fileSystemProvider;
                    return provider.getSpacesOwnedByAccount(accountNameOrID);
                } else {
                    throw new IllegalStateException("Chosen file system does not provide access to Hub Spaces");
                }
            }
        }

        /**
         * @param spaceId The space id.
         * @return The {@link Space} object.
         * @throws IOException
         */
        @SuppressWarnings("resource")
        public Space fetchSpace(final String spaceId) throws IOException {
            try (var connection = createFSConnection()) {
                final var fileSystemProvider = connection.getFileSystem().provider();

                if (fileSystemProvider instanceof SpaceAware) {
                    return ((SpaceAware)fileSystemProvider).getSpace(spaceId);
                } else {
                    throw new IllegalStateException("Chosen file system does not provide access to Hub Spaces");
                }
            }
        }
    }
}
