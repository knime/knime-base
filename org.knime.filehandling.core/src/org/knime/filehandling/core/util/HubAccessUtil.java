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

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.util.auth.Authenticator;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFileSystem;
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

    private static List<Space> listSpaces(final FSFileSystem<?> fs) throws IOException {
        final var fileSystemProvider = fs.provider();

        if (fileSystemProvider instanceof SpaceAware) {
            return ((SpaceAware)fileSystemProvider).getSpaces();
        } else {
            throw new IllegalStateException("Chosen file system does not provide access to Hub Spaces");
        }
    }

    private static Space fetchSpace(final FSFileSystem<?> fs, final String spaceId) throws IOException {
        final var fileSystemProvider = fs.provider();

        if (fileSystemProvider instanceof SpaceAware) {
            return ((SpaceAware)fileSystemProvider).getSpace(spaceId);
        } else {
            throw new IllegalStateException("Chosen file system does not provide access to Hub Spaces");
        }
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
     * @param repositoryAddress Hub repository address
     * @param authenticator Hub authenticator
     * @return The list of spaces stored in the Hub instance described by the given repository address.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public static List<Space> getSpacesByRepositoryAddress(final URI repositoryAddress,
        final Authenticator authenticator) throws IOException {
        try (var connection = DefaultFSConnectionFactory.createHubConnection(repositoryAddress, authenticator)) {
            return listSpaces(connection.getFileSystem());
        }
    }

    /**
     * @param repositoryAddress Hub repository address
     * @param authenticator Hub authenticator
     * @param spaceId The space id
     * @return The space info about a space described by the given repository address and space id.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public static Space getSpaceByRepositoryAddress(final URI repositoryAddress,
        final Authenticator authenticator, final String spaceId) throws IOException {
        try (var connection = DefaultFSConnectionFactory.createHubConnection(repositoryAddress, authenticator)) {
            return fetchSpace(connection.getFileSystem(), spaceId);
        }
    }

    /**
     * @return The list of spaces stored in the current Hub instance.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public static List<Space> getSpacesByWorkflowContext() throws IOException {
        try (var connection = getFSConnectionByWorkflowContext()) {
            return listSpaces(connection.getFileSystem());
        }
    }

    /**
     * @param spaceId The space id.
     * @return The space info about a space with the given id and located on the current Hub instance.
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public static Space getSpaceByWorkflowContext(final String spaceId) throws IOException {
        try (var connection = getFSConnectionByWorkflowContext()) {
            return fetchSpace(connection.getFileSystem(), spaceId);
        }
    }
}
