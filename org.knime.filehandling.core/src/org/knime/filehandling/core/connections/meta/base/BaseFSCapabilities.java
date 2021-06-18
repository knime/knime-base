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
 *   Apr 28, 2021 (bjoern): created
 */
package org.knime.filehandling.core.connections.meta.base;

import org.knime.filehandling.core.connections.meta.FSCapabilities;

/**
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class BaseFSCapabilities implements FSCapabilities {

    private final boolean m_canBrowse;

    private final boolean m_canListDirectories;

    private final boolean m_canCreateDirectories;

    private final boolean m_canDeleteDirectories;

    private final boolean m_canGetPosixAttributes;

    private final boolean m_canSetPosixAttributes;

    private final boolean m_canCheckAccessReadOnFiles;

    private final boolean m_canCheckAccessReadOnDirectories;

    private final boolean m_canCheckAccessWriteOnFiles;

    private final boolean m_canCheckAccessWriteOnDirectories;

    private final boolean m_canCheckAccessExecuteOnFiles;

    private final boolean m_canCheckAccessExecuteOnDirectories;

    private final boolean m_canWriteFiles;

    private final boolean m_canDeleteFiles;

    private final boolean m_isWorkflowAware;

    private BaseFSCapabilities(final boolean canBrowse, //
        final boolean canListDirectories, //
        final boolean canCreateDirectories, //
        final boolean canDeleteDirectories, //
        final boolean canGetPosixAttributes, //
        final boolean canSetPosixAttributes, //
        final boolean canCheckAccessReadOnFiles, //
        final boolean canCheckAccessReadOnDirectories, //
        final boolean canCheckAccessWriteOnFiles, //
        final boolean canCheckAccessWriteOnDirectories, //
        final boolean canCheckAccessExecuteOnFiles, //
        final boolean canCheckAccessExecuteOnDirectories, //
        final boolean canWriteFiles, //
        final boolean canDeleteFiles, //
        final boolean isWorkflowAware) {

        m_canBrowse = canBrowse;
        m_canListDirectories = canListDirectories;
        m_canCreateDirectories = canCreateDirectories;
        m_canDeleteDirectories = canDeleteDirectories;
        m_canGetPosixAttributes = canGetPosixAttributes;
        m_canSetPosixAttributes = canSetPosixAttributes;
        m_canCheckAccessReadOnFiles = canCheckAccessReadOnFiles;
        m_canCheckAccessReadOnDirectories = canCheckAccessReadOnDirectories;
        m_canCheckAccessWriteOnFiles = canCheckAccessWriteOnFiles;
        m_canCheckAccessWriteOnDirectories = canCheckAccessWriteOnDirectories;
        m_canCheckAccessExecuteOnFiles = canCheckAccessExecuteOnFiles;
        m_canCheckAccessExecuteOnDirectories = canCheckAccessExecuteOnDirectories;
        m_canWriteFiles = canWriteFiles;
        m_canDeleteFiles = canDeleteFiles;
        m_isWorkflowAware = isWorkflowAware;
    }

    @Override
    public boolean canBrowse() {
        return m_canBrowse;
    }

    @Override
    public boolean canListDirectories() {
        return m_canListDirectories;
    }

    @Override
    public boolean canCreateDirectories() {
        return m_canCreateDirectories;
    }

    @Override
    public boolean canDeleteDirectories() {
        return m_canDeleteDirectories;
    }

    @Override
    public boolean canGetPosixAttributes() {
        return m_canGetPosixAttributes;
    }

    @Override
    public boolean canSetPosixAttributes() {
        return m_canSetPosixAttributes;
    }

    @Override
    public boolean canCheckAccessReadOnFiles() {
        return m_canCheckAccessReadOnFiles;
    }

    @Override
    public boolean canCheckAccessReadOnDirectories() {
        return m_canCheckAccessReadOnDirectories;
    }

    @Override
    public boolean canCheckAccessWriteOnFiles() {
        return m_canCheckAccessWriteOnFiles;
    }

    @Override
    public boolean canCheckAccessWriteOnDirectories() {
        return m_canCheckAccessWriteOnDirectories;
    }

    @Override
    public boolean canCheckAccessExecuteOnFiles() {
        return m_canCheckAccessExecuteOnFiles;
    }

    @Override
    public boolean canCheckAccessExecuteOnDirectories() {
        return m_canCheckAccessExecuteOnDirectories;
    }

    @Override
    public boolean canWriteFiles() {
        return m_canWriteFiles;
    }

    @Override
    public boolean canDeleteFiles() {
        return m_canDeleteFiles;
    }

    @Override
    public boolean isWorkflowAware() {
        return m_isWorkflowAware;
    }

    static class Builder {
        private boolean m_canBrowse = true;

        private boolean m_canListDirectories = true;

        private boolean m_canCreateDirectories = true;

        private boolean m_canDeleteDirectories = true;

        private boolean m_canGetPosixAttributes = false;

        private boolean m_canSetPosixAttributes = false;

        private boolean m_canCheckAccessReadOnFiles = false;

        private boolean m_canCheckAccessReadOnDirectories = false;

        private boolean m_canCheckAccessWriteOnFiles = false;

        private boolean m_canCheckAccessWriteOnDirectories = false;

        private boolean m_canCheckAccessExecuteOnFiles = false;

        private boolean m_canCheckAccessExecuteOnDirectories = false;

        private boolean m_canWriteFiles = true;

        private boolean m_canDeleteFiles = true;

        private boolean m_isWorkflowAware = false;

        Builder withCanBrowse(final boolean canBrowse) {
            m_canBrowse = canBrowse;
            return this;
        }

        Builder withCanListDirectories(final boolean canListDirectories) {
            m_canListDirectories = canListDirectories;
            return this;
        }

        Builder withCanCreateDirectories(final boolean canCreateDirectories) {
            m_canCreateDirectories = canCreateDirectories;
            return this;
        }

        Builder withCanDeleteDirectories(final boolean canDeleteDirectories) {
            m_canDeleteDirectories = canDeleteDirectories;
            return this;
        }

        Builder withCanGetPosixAttributes(final boolean canGetPosixAttributes) {
            m_canGetPosixAttributes = canGetPosixAttributes;
            return this;
        }

        Builder withCanSetPosixAttributes(final boolean canSetPosixAttributes) {
            m_canSetPosixAttributes = canSetPosixAttributes;
            return this;
        }

        Builder withCanCheckAccessReadOnFiles(final boolean canCheckAccessReadOnFiles) {
            m_canCheckAccessReadOnFiles = canCheckAccessReadOnFiles;
            return this;
        }

        Builder withCanCheckAccessReadOnDirectories(final boolean canCheckAccessReadOnDirectories) {
            m_canCheckAccessReadOnDirectories = canCheckAccessReadOnDirectories;
            return this;
        }

        Builder withCanCheckAccessWriteOnFiles(final boolean canCheckAccessWriteOnFiles) {
            m_canCheckAccessWriteOnFiles = canCheckAccessWriteOnFiles;
            return this;
        }

        Builder withCanCheckAccessWriteOnDirectories(final boolean canCheckAccessWriteOnDirectories) {
            m_canCheckAccessWriteOnDirectories = canCheckAccessWriteOnDirectories;
            return this;
        }

        Builder withCanCheckAccessExecuteOnFiles(final boolean canCheckAccessExecuteOnFiles) {
            m_canCheckAccessExecuteOnFiles = canCheckAccessExecuteOnFiles;
            return this;
        }

        Builder withCanCheckAccessExecuteOnDirectories(final boolean canCheckAccessExecuteOnDirectories) {
            m_canCheckAccessExecuteOnDirectories = canCheckAccessExecuteOnDirectories;
            return this;
        }

        Builder withCanWriteFiles(final boolean canWriteFiles) {
            m_canWriteFiles = canWriteFiles;
            return this;
        }

        Builder withCanDeleteFiles(final boolean canDeleteFiles) {
            m_canDeleteFiles = canDeleteFiles;
            return this;
        }

        Builder withIsWorkflowAware(final boolean isWorkflowAware) {
            m_isWorkflowAware = isWorkflowAware;
            return this;
        }

        BaseFSCapabilities build() {
            return new BaseFSCapabilities(m_canBrowse, //
                m_canListDirectories, //
                m_canCreateDirectories, //
                m_canDeleteDirectories, //
                m_canGetPosixAttributes, //
                m_canSetPosixAttributes, //
                m_canCheckAccessReadOnFiles, //
                m_canCheckAccessReadOnDirectories, //
                m_canCheckAccessWriteOnFiles, //
                m_canCheckAccessWriteOnDirectories, //
                m_canCheckAccessExecuteOnFiles, //
                m_canCheckAccessExecuteOnDirectories, //
                m_canWriteFiles, //
                m_canDeleteFiles, //
                m_isWorkflowAware);
        }
    }
}
