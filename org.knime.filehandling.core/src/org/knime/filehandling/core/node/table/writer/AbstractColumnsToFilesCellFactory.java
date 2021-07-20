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
 *   20 Jul 2021 (Laurin): created
 */
package org.knime.filehandling.core.node.table.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.filehandling.core.connections.FSFiles;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.cell.MultiSimpleFSLocationCellFactory;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCell;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.node.table.writer.AbstractMultiTableWriterNodeModel.FileNameGenerator;

/**
 * Factory that extends {@link AbstractCellFactory}, which creates one new column {@link SimpleFSLocationCell} holding
 * the output path of written images. It also handles the writing of images to the file system.
 *
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractColumnsToFilesCellFactory<T extends DataValue> extends AbstractCellFactory {

    private final MultiSimpleFSLocationCellFactory m_multiFSLocationCellFactory;

    private final FSPath m_outputPath;

    private final FileOverwritePolicy m_overwritePolicy;

    private final int m_colIdx;

    private int m_rowIdx = 0;

    private int m_missingCellCount = 0;

    private static final MissingCell MISSING_VALUE_CELL = new MissingCell("Missing cell value");

    private final FileNameGenerator m_fileNameGenerator;

    AbstractColumnsToFilesCellFactory(final DataColumnSpec[] columnSpec, final int imgColIdx,
        final FileOverwritePolicy overwritePolicy, final FSPath outputPath, final FileNameGenerator fileNameGenerator) {
        super(columnSpec);
        m_colIdx = imgColIdx;
        m_outputPath = outputPath;
        m_overwritePolicy = overwritePolicy;
        m_multiFSLocationCellFactory = new MultiSimpleFSLocationCellFactory();
        m_fileNameGenerator = fileNameGenerator;
    }

    @Override
    public DataCell[] getCells(final DataRow row) {
        final DataCell valueCell = row.getCell(m_colIdx);

        if (valueCell.isMissing()) {
            m_missingCellCount++;
            m_rowIdx++;
            return new DataCell[]{MISSING_VALUE_CELL, MISSING_VALUE_CELL};
        }

        final T value = (T)valueCell;
        final FSPath outputPath = createOutputPath(value);
        m_rowIdx++;

        final String fileStatus = write(value, outputPath);
        return new DataCell[]{m_multiFSLocationCellFactory.createCell(outputPath.toFSLocation()),
            StringCellFactory.create(fileStatus)};
    }

    /**
     * TODO: Write javadoc with example for filenameGenerator
     *
     * return (FSPath)m_outputPath
     *          .resolve(m_fileNameGenerator.getOutputFilename(row, m_rowIdx) + "." + value.getImageExtension());
     *
     * @param value
     * @return
     */
    protected abstract FSPath createOutputPath(final T value);

    private String write(final T value, final FSPath outputPath) {
        String status;

        try {
            status = FSFiles.exists(outputPath) ? "overwritten" : "created";
        } catch (AccessDeniedException accessDeniedException) {
            throw new IllegalStateException(
                String.format("An AccessDeniedException occured while writing '%s'", outputPath.toString()),
                accessDeniedException);
        }

        try (final OutputStream outputStream = FSFiles.newOutputStream(outputPath, m_overwritePolicy.getOpenOptions())) {
            writeFile(outputStream, value);
        } catch (FileAlreadyExistsException fileAlreadyExistsException) {
            if (m_overwritePolicy == FileOverwritePolicy.FAIL) {
                throw new IllegalStateException(
                    String.format("The file '%s' already exists and must not be overwritten", outputPath.toString()),
                    fileAlreadyExistsException);
            } else if (m_overwritePolicy == FileOverwritePolicy.IGNORE) {
                status = "unmodified";
            }
        } catch (IOException writeImageException) {
            throw new IllegalStateException(
                String.format("An IOException occured while writing '%s'", outputPath.toString()), writeImageException);
        }
        return status;
    }

    /**
     * TODO: Write javadoc with example for write operation
     *
     * imgValue.getImageContent().save(outputStream);
     * @param outputStream
     * @param value
     */
    protected abstract void writeFile(final OutputStream outputStream, final T value) throws IOException;

    //TODO: Check visibility -> depends where abstraction class is stored
    protected int getMissingCellCount() {
        return m_missingCellCount;
    }

}
