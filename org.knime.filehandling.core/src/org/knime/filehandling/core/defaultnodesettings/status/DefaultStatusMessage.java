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
 *   May 7, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.status;

import org.knime.core.node.util.CheckUtils;

/**
 * Default implementation of a {@link StatusMessage}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DefaultStatusMessage implements StatusMessage {

    private final MessageType m_type;

    private final String m_msg;

    /**
     * Constructor.
     *
     * @param type of message
     * @param msg the actual message
     */
    public DefaultStatusMessage(final MessageType type, final String msg) {
        m_type = CheckUtils.checkArgumentNotNull(type, "The type must not be null.");
        m_msg = CheckUtils.checkArgumentNotNull(msg, "The msg must not be null.");
    }

    /**
     * Convenience constructor that allows to use formatting.
     *
     * @param type of message
     * @param format defines the format of the message
     * @param args arguments that are injected into <b>format</b>
     * @see String#format(String, Object...)
     */
    public DefaultStatusMessage(final MessageType type, final String format, final Object... args) {
        this(type, String.format(format, args));
    }

    @Override
    public MessageType getType() {
        return m_type;
    }

    @Override
    public String getMessage() {
        return m_msg;
    }

    @Override
    public String toString() {
        return String.format("[Type: %s, Message: %s]", m_type, m_msg);
    }

    /**
     * Creates an error {@link StatusMessage}.
     *
     * @param format defines the format of the message
     * @param args arguments that are injected into <b>format</b>
     * @return the error {@link StatusMessage}
     */
    public static StatusMessage mkError(final String format, final Object... args) {
        return new DefaultStatusMessage(MessageType.ERROR, format, args);
    }

    /**
     * Creates a warning {@link StatusMessage}.
     *
     * @param format defines the format of the message
     * @param args arguments that are injected into <b>format</b>
     * @return the warning {@link StatusMessage}
     */
    public static StatusMessage mkWarning(final String format, final Object... args) {
        return new DefaultStatusMessage(MessageType.WARNING, format, args);
    }

    /**
     * Creates a info {@link StatusMessage}.
     *
     * @param format defines the format of the message
     * @param args arguments that are injected into <b>format</b>
     * @return the info {@link StatusMessage}
     */
    public static StatusMessage mkInfo(final String format, final Object... args) {
        return new DefaultStatusMessage(MessageType.INFO, format, args);
    }

}
