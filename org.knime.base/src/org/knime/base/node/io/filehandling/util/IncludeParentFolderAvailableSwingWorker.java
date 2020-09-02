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
 *   Aug 27, 2020 (lars.schweikardt): created
 */
package org.knime.base.node.io.filehandling.util;

import java.util.EnumSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * Swingworker to check whether a path ends with "." or ".." and return true or false to the passed {@link Consumer}.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
public final class IncludeParentFolderAvailableSwingWorker extends SwingWorkerWithContext<Boolean, Void> {

    private final Supplier<ReadPathAccessor> m_readPathAccessorSupplier;

    private final FilterMode m_filterMode;

    private final NodeModelStatusConsumer m_statusConsumer;

    private final Consumer<Boolean> m_booleanConsumer;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(IncludeParentFolderAvailableSwingWorker.class);

    /**
     * Constructor.
     *
     * @param readPathAccessorSupplier the path accessor of a {@link SettingsModelReaderFileChooser}
     * @param filterMode the {@link FilterMode}
     * @param booleanConsumer a {@link Consumer}
     */
    public IncludeParentFolderAvailableSwingWorker(final Supplier<ReadPathAccessor> readPathAccessorSupplier,
        final FilterMode filterMode, final Consumer<Boolean> booleanConsumer) {
        m_booleanConsumer = booleanConsumer;
        m_readPathAccessorSupplier = readPathAccessorSupplier;
        m_filterMode = filterMode;
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.INFO));
    }

    @Override
    protected Boolean doInBackgroundWithContext() throws Exception {
        try (final ReadPathAccessor readPathAccessor = m_readPathAccessorSupplier.get()) {
            final FSPath rootPath = readPathAccessor.getRootPath(m_statusConsumer);

            return PathHandlingUtils.isIncludeParentFolderAvailable(rootPath, m_filterMode);
        }
    }

    @Override
    protected void doneWithContext() {
        try {
            m_booleanConsumer.accept(get());
        } catch (ExecutionException e) {
            LOGGER.debug("Error during swingworker execution", e);
            m_booleanConsumer.accept(false);
        } catch (CancellationException e) {
            LOGGER.debug("Swingworker got canceled", e);
        } catch (InterruptedException e) { //NOSONAR
            /* the InterruptedException will be never thrown and in case it will be,
             * we cannot interrupt the UI Thread by using Thread.currentThread.interrupt()*/
            LOGGER.error("Swingworker got interrupted", e);
        }
    }
}
