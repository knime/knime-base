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
 *   20 Jul 2021 (Laurin Siefermann, KNIME GmbH, Konstanz, Germany): created
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

/**
 * Factory that extends {@link AbstractCellFactory}, which creates one new column {@link SimpleFSLocationCell} holding
 * the output path of written files. It also handles the writing of files of a specific type <C> to the file system.
 *
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 * @param <T> the type of {@link DataValue}
 */
public abstract class AbstractMultiTableWriterCellFactory<T extends DataValue> extends AbstractCellFactory {

    private static final MissingCell MISSING_VALUE_CELL = new MissingCell("Missing cell value");

    private final MultiSimpleFSLocationCellFactory m_multiFSLocationCellFactory;

    private final int m_sourceColumnIndex;

    private FileOverwritePolicy m_overwritePolicy;

    private int m_rowIndex = 0;

    private int m_missingCellCount = 0;

    private FileNameGenerator m_fileNameGenerator;

    private FSPath m_outputPath;

    /**
     * Constructor for a concrete subclass instantiation of {@link AbstractMultiTableWriterCellFactory}.
     *
     * @param outputColumnsSpecs the spec's of the created columns
     * @param sourceColumnIndex index of source column
     * @param overwritePolicy policy how to proceed when output file exists according to {@link FileOverwritePolicy}
     */
    protected AbstractMultiTableWriterCellFactory(final DataColumnSpec[] outputColumnsSpecs, final int sourceColumnIndex,
        final FileOverwritePolicy overwritePolicy) {
        super(outputColumnsSpecs);
        m_sourceColumnIndex = sourceColumnIndex;
        m_multiFSLocationCellFactory = new MultiSimpleFSLocationCellFactory();
    }

    @Override
    public DataCell[] getCells(final DataRow row) {
        final DataCell valueCell = row.getCell(m_sourceColumnIndex);

        if (valueCell.isMissing()) {
            m_missingCellCount++;
            m_rowIndex++;
            return new DataCell[]{MISSING_VALUE_CELL, MISSING_VALUE_CELL};
        }

        @SuppressWarnings("unchecked")
        final T value = (T)valueCell;
        final String fileExtension = getOutputFileExtension(value);
        final FSPath outputFilePath = createOutputPath(row, fileExtension);
        m_rowIndex++;

        final String fileStatus = write(value, outputFilePath);
        return new DataCell[]{m_multiFSLocationCellFactory.createCell(outputFilePath.toFSLocation()),
            StringCellFactory.create(fileStatus)};
    }

    private String write(final T value, final FSPath outputPath) {
        String status;

        try {
            status = FSFiles.exists(outputPath) ? "overwritten" : "created";
        } catch (AccessDeniedException accessDeniedException) {
            throw new IllegalStateException(accessDeniedException.getMessage(), accessDeniedException);
        }

        try (final OutputStream outputStream =
            FSFiles.newOutputStream(outputPath, m_overwritePolicy.getOpenOptions())) {
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

    int getMissingCellCount() {
        return m_missingCellCount;
    }

    void setOutputPath(final FSPath outputPath) {
        m_outputPath = outputPath;
    }

    void setFileNameGenerator(final FileNameGenerator fileNameGenerator) {
        m_fileNameGenerator = fileNameGenerator;
    }

    void setOverwritePolicy(final FileOverwritePolicy overwritePolicy) {
        m_overwritePolicy = overwritePolicy;
    }

    private FSPath createOutputPath(final DataRow row, final String fileExtension) {
        return (FSPath)m_outputPath
            .resolve(String.format("%s.%s", m_fileNameGenerator.getOutputFilename(row, m_rowIndex), fileExtension));
    }

    /**
     * Writes a file to a given outputStream.
     *
     * <pre>
     * // example for images
     * imgValue.getImageContent().save(outputStream);
     * </pre>
     *
     * @param outputStream
     * @param value concrete subclass instantiation of {@link DataValue}
     * @throws IOException
     */
    protected abstract void writeFile(final OutputStream outputStream, final T value) throws IOException;

    /**
     * Resolves the file extension against a given {@link DataValue}
     *
     * <pre>
     * // example for images
     * return value.getImageExtension();
     * </pre>
     *
     * @param value concrete subclass instantiation of {@link DataValue}
     * @return extension of the written file
     */
    protected abstract String getOutputFileExtension(final T value);
}
