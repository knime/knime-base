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
 *   Nov 27, 2020 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.core.fs.knime.relativeto.export;

import org.knime.filehandling.core.connections.RelativeTo;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.connections.uriexport.URIExporterFactory;
import org.knime.filehandling.core.connections.uriexport.base.BaseURIExporterMetaInfo;
import org.knime.filehandling.core.connections.uriexport.base.LegacyKNIMEUriExporterHelper;
import org.knime.filehandling.core.connections.uriexport.noconfig.NoConfigURIExporterFactory;

/**
 * {@link URIExporter} that provides legacy knime:// URLs.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class LegacyKNIMEUrlExporterFactory extends NoConfigURIExporterFactory {

    private static final BaseURIExporterMetaInfo META_INFO =
        new BaseURIExporterMetaInfo("knime:// URL", "Generates a knime:// URL");

    private static final LegacyKNIMEUrlExporterFactory WORKFLOW_RELATIVE_INSTANCE =
        new LegacyKNIMEUrlExporterFactory(RelativeTo.WORKFLOW);

    private static final LegacyKNIMEUrlExporterFactory WORKFLOW_DATA_RELATIVE_INSTANCE =
            new LegacyKNIMEUrlExporterFactory(RelativeTo.WORKFLOW_DATA);

    private static final LegacyKNIMEUrlExporterFactory MOUNTPOINT_RELATIVE_INSTANCE =
        new LegacyKNIMEUrlExporterFactory(RelativeTo.MOUNTPOINT);

    private static final LegacyKNIMEUrlExporterFactory SPACE_RELATIVE_INSTANCE =
        new LegacyKNIMEUrlExporterFactory(RelativeTo.SPACE);

    private LegacyKNIMEUrlExporterFactory(final RelativeTo type) {
        super(META_INFO, p -> LegacyKNIMEUriExporterHelper.createRelativeKNIMEProtocolURI(type, p));
    }

    /**
     * @return the singleton instance for the {@link URIExporterFactory} that generates workflow-relative knime:// URLs
     */
    public static NoConfigURIExporterFactory getWorkflowRelativeInstance() {
        return WORKFLOW_RELATIVE_INSTANCE;
    }

    /**
     * @return the singleton instance for the {@link URIExporterFactory} that generates mountpoint-relative knime://
     *         URLs
     */
    public static NoConfigURIExporterFactory getMountpointRelativeInstance() {
        return MOUNTPOINT_RELATIVE_INSTANCE;
    }

    /**
     * @return the singleton instance for the {@link URIExporterFactory} that generates workflow-data-area-relative
     *         knime:// URLs
     */
    public static NoConfigURIExporterFactory getWorkflowDataRelativeInstance() {
        return WORKFLOW_DATA_RELATIVE_INSTANCE;
    }

    /**
     * @return the singleton instance for the {@link URIExporterFactory} that generates space-relative
     *         knime:// URLs
     */
    public static LegacyKNIMEUrlExporterFactory getSpaceRelativeInstance() {
        return SPACE_RELATIVE_INSTANCE;
    }
}
