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
 *   28.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.s3;

import java.net.URI;
import java.net.URISyntaxException;

import org.knime.cloud.core.util.port.CloudConnectionInformation;
import org.knime.core.node.NodeLogger;

/**
 * Connection information for the {@link S3FileSystem}.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class S3CloudConnectionInformation extends CloudConnectionInformation {

    private static final long serialVersionUID = 1L;

    private boolean m_useAnonymous;

    /**
     * @return the useAnonymous
     */
    public boolean isUseAnonymous() {
        return m_useAnonymous;
    }

    /**
     * @param useAnonymous the useAnonymous to set
     */
    public void setUseAnonymous(final boolean useAnonymous) {
        m_useAnonymous = useAnonymous;
    }

    /**
     * @return a URI including switchroleInformation if necessary for identification of the fileSystem
     */
    public URI getFileSystemURI() {
        URI uri = toURI();
        final StringBuilder sb = new StringBuilder();
        if (switchRole()) {
            sb.append(getSwitchRoleAccount());
            sb.append(":");
            sb.append(getSwitchRoleName());
            sb.append(":");

            sb.append(getUser());
            try {
                uri = new URI(getProtocol(), sb.toString(), getHost(), getPort(), null, null, null);
            } catch (final URISyntaxException e) {
                // Should not happen
                NodeLogger.getLogger(getClass()).coding(e.getMessage(), e);
            }
        }
        return uri;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof S3CloudConnectionInformation && super.equals(obj)) {
            return m_useAnonymous == ((S3CloudConnectionInformation)obj).m_useAnonymous;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hsc = super.hashCode();
        hsc += m_useAnonymous ? 1 : 0;
        return hsc;
    }

}
