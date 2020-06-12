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
 *   Jun 12, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.status;

import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.node.NodeModel;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractSettingsModelFileChooser;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * A {@link PriorityStatusConsumer} of {@link StatusMessage StatusMessages} that allows to filter for specific
 * {@link MessageType}s.
 * <p>
 * This consumer should be used in every {@link NodeModel NodeModels} that use {@link AbstractSettingsModelFileChooser}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class NodeModelStatusConsumer implements Consumer<StatusMessage> {

    private final PriorityStatusConsumer m_priorityConsumer;

    private final Set<MessageType> m_msgTypeFilter;

    /**
     * Constructor.
     *
     * @param msgTypeFilter the {@link MessageType} that are being accepted
     */
    public NodeModelStatusConsumer(final Set<MessageType> msgTypeFilter) {
        m_priorityConsumer = new PriorityStatusConsumer();
        m_msgTypeFilter = msgTypeFilter;
    }

    @Override
    public void accept(final StatusMessage t) {
        if (m_msgTypeFilter.contains(t.getType())) {
            m_priorityConsumer.accept(t);
        }
    }

    /**
     * Forwards the status message with the highest priority that matched the filter to the consumer.
     *
     * @param consumer the {@link Consumer}
     */
    public void setWarningsIfRequired(final Consumer<String> consumer) {
        m_priorityConsumer.get().map(StatusMessage::toString).ifPresent(consumer::accept);
        m_priorityConsumer.clear();
    }

}
