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
 *   Apr 14, 2021 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.utility.nodes.pathtouri;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
import java.util.Optional;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingValue;
import org.knime.core.data.MissingValueException;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.uri.UriCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.location.FSPathProvider;
import org.knime.filehandling.core.connections.location.FSPathProviderFactory;
import org.knime.filehandling.core.connections.uriexport.URIExporter;
import org.knime.filehandling.core.data.location.FSLocationValue;

/**
 * {@link CellFactory} to convert {@link FSLocation}s into {@link URI}s.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
final class PathToUriCellFactory extends SingleCellFactory implements Closeable {

    private static final NodeLogger LOG = NodeLogger.getLogger(PathToUriCellFactory.class);

    private final int m_pathColumnIdx;

    private final URIExporterModelHelper m_exporterModelHelper;

    private final FSPathProviderFactory m_pathProviderFactory;

    private final boolean m_failIfPathNotExists;

    private final boolean m_failOnMissingValues;

    PathToUriCellFactory(final int pathColumnIdx, final DataColumnSpec newColSpec, final Optional<FSConnection> portObjectConnection,
        final URIExporterModelHelper uriModelHelper, final boolean failIfPathNotExists, final boolean failOnMissingValues) {
        super(newColSpec);

        m_pathColumnIdx = pathColumnIdx;
        m_exporterModelHelper = uriModelHelper;
        m_pathProviderFactory =  FSPathProviderFactory.newFactory(portObjectConnection, uriModelHelper.getPathColumnFSLocationSpec());
        m_failIfPathNotExists = failIfPathNotExists;
        m_failOnMissingValues = failOnMissingValues;
    }

    @Override
    public void close() {
        try {
            m_pathProviderFactory.close();
        } catch (IOException e) {
            LOG.error("Error when closing path provider factory: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("resource")
    @Override
    public DataCell getCell(final DataRow row) {
        //Check if the column is missing and return a missing DataCell object
        if (row.getCell(m_pathColumnIdx).isMissing()) {
            if (m_failOnMissingValues) {
                throw new MissingValueException((MissingValue)row.getCell(m_pathColumnIdx),
                    "Input column contains missing cell(s).");
            } else {
                return DataType.getMissingCell();
            }
        }

        final FSLocation fsLocation = ((FSLocationValue)row.getCell(m_pathColumnIdx)).getFSLocation();

        try (final FSPathProvider pathProvider = m_pathProviderFactory.create(fsLocation)) {

            final FSPath fsPath = pathProvider.getPath();

            if (m_failIfPathNotExists && !FSFiles.exists(fsPath)) {
                throw new UncheckedIOException(String.format("The path '%s' does not exist.", fsPath),
                    new NoSuchFileException(fsPath.toString()));
            }

            return convertPathCellToURI(pathProvider.getFSConnection(), fsPath);
        } catch (InvalidSettingsException e) {
            throw new UrlConversionException(String.format("Failed to load URL format settings when converting %s: %s",
                fsLocation.toString(), e.getMessage()), e);
        } catch (URISyntaxException e) {
            throw new UrlConversionException(
                String.format("Failed convert %s to a URL: %s", fsLocation.toString(), e.getMessage()), e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DataCell convertPathCellToURI(final FSConnection fsConnection, final FSPath path)
        throws URISyntaxException, InvalidSettingsException {

        final URIExporter exporter = m_exporterModelHelper.createExporter(fsConnection);
        return UriCellFactory.create(exporter.toUri(path).toString());
    }

    @SuppressWarnings("serial")
    static class UrlConversionException extends RuntimeException {
        UrlConversionException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}