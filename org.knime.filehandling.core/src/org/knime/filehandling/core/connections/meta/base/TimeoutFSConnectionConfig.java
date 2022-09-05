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
 *   Jun 23, 2022 (bjoern): created
 */
package org.knime.filehandling.core.connections.meta.base;

import java.time.Duration;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.meta.FSConnectionConfig;

/**
 * Abstract base class for {@link FSConnectionConfig}s that require timeout parameters. This class provides a
 * <i>connection timeout</i> and and <i>read timeout</i>. Typically, the first is the timeout to use when opening a
 * TCP/IP connection to a target host, and the second is a per-request timeout that defines how long to wait for an
 * answer after having sent a request/command to the target host.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public abstract class TimeoutFSConnectionConfig extends BaseFSConnectionConfig {

    /**
     * Default connection and read timeout in seconds.
     */
    public static final int DEFAULT_TIMEOUT_SECONDS = 60;

    /**
     * Default duration timeout.
     */
    public static final Duration DEFAULT_TIMEOUT_DURATION = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);

    private Duration m_connectionTimeout = DEFAULT_TIMEOUT_DURATION;

    private Duration m_readTimeout = DEFAULT_TIMEOUT_DURATION;

    /**
     * Creates a config for a {@link FSCategory#CONNECTED} file system with the given working directory.
     *
     * @param workingDirectory The working directory to set.
     */
    protected TimeoutFSConnectionConfig(final String workingDirectory) {
        super(workingDirectory);
    }

    /**
     * Creates a connection config with the given working directory and connectedness.
     *
     * @param workingDirectory The working directory to set.
     * @param isConnected Set to true when creating a config for a {@link FSCategory#CONNECTED} file system, and set to
     *            false otherwise.
     */
    protected TimeoutFSConnectionConfig(final String workingDirectory, final boolean isConnected) {
        super(workingDirectory, isConnected);
    }

    /**
     * Creates a connection config with the given settings.
     *
     * @param workingDirectory The working directory to set.
     * @param isConnected Set to true when creating a config for a {@link FSCategory#CONNECTED} file system, and set to
     *            false otherwise.
     * @param connectionTimeout Timeout to use when opening a TCP/IP connection to a target host.
     * @param readTimeout A per-request timeout that defines how long to wait for an answer after having sent a
     *            request/command to the target host
     */
    protected TimeoutFSConnectionConfig(final String workingDirectory, //
        final boolean isConnected, //
        final Duration connectionTimeout, //
        final Duration readTimeout) {

        super(workingDirectory, isConnected);
        m_connectionTimeout = connectionTimeout;
        m_readTimeout = readTimeout;
    }

    /**
     * @return timeout to use when opening a TCP/IP connection to a target host.
     */
    public final Duration getConnectionTimeout() {
        return m_connectionTimeout;
    }

    /**
     * @return a per-request timeout that defines how long to wait for an answer after having sent a request/command to
     *         the target host.
     */
    public final Duration getReadTimeout() {
        return m_readTimeout;
    }

    /**
     * Sets the timeout to use when opening a TCP/IP connection to a target host.
     *
     * @param connectionTimeout The timeout to set in the form of a {@link Duration}.
     */
    public final void setConnectionTimeout(final Duration connectionTimeout) {
        CheckUtils.checkArgument(!connectionTimeout.isNegative(), "Negative connection timeouts are not allowed.");
        m_connectionTimeout = connectionTimeout;
    }

    /**
     * Sets the per-request timeout that defines how long to wait for an answer after having sent a request/command to
     * the target host.
     *
     * @param readTimeout The timeout to set in the form of a {@link Duration}.
     */
    public final void setReadTimeout(final Duration readTimeout) {
        CheckUtils.checkArgument(!readTimeout.isNegative(), "Negative read timeouts are not allowed.");
        m_readTimeout = readTimeout;
    }
}
