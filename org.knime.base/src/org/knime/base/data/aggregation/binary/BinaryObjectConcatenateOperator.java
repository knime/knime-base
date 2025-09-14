/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */

package org.knime.base.data.aggregation.binary;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataCell;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;

/**
 * Concatenates all binary objects in a column, interpreting missing values as empty (zero-byte) objects. The
 * {@link #inclMissingCells()} setting determines whether the result is missing or empty if all input cells are missing.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
public final class BinaryObjectConcatenateOperator extends AggregationOperator {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(BinaryObjectConcatenateOperator.class);

    private final BinaryObjectCellFactory m_factory;

    private DeferredFileOutputStream m_outputStream;

    /**
     * Constructor with default operator data.
     *
     * @param globalSettings global settings
     * @param opColSettings operator column settings
     */
    public BinaryObjectConcatenateOperator(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        this(new OperatorData("Binary_Concat", "Concatenate binary objects", "Concatenated binary objects", false,
            true, BinaryObjectDataValue.class, true), globalSettings, opColSettings);
    }

    private BinaryObjectConcatenateOperator(final OperatorData operatorData, final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        super(operatorData, globalSettings, opColSettings);
        m_factory = new BinaryObjectCellFactory();
        Optional.ofNullable(globalSettings.getFileStoreFactory()).ifPresent(m_factory::initFactory);
    }

    @Override
    public AggregationOperator createInstance(final GlobalSettings globalSettings,
        final OperatorColumnSettings opColSettings) {
        return new BinaryObjectConcatenateOperator(getOperatorData(), globalSettings, opColSettings);
    }

    @Override
    public String getDescription() {
        return "Concatenates all non-missing binary objects in a column. The result for an all-missing group "
            + "(empty vs. missing) is configurable via the \"include missing\" setting.";
    }

    @Override
    protected DataType getDataType(final DataType origType) {
        return BinaryObjectDataCell.TYPE;
    }

    @Override
    protected boolean computeInternal(final DataCell cell) {
        if (cell.isMissing()) {
            // `inclMissingCells()` only determines the result cell if all cells are missing
            return false;
        }

        if (!(cell instanceof BinaryObjectDataValue binaryData)) {
            setSkipMessage("Expected %s data cell, found %s" //
                .formatted(BinaryObjectDataCell.TYPE.getName(), cell.getType().getName()));
            return true;
        }

        if (m_outputStream == null) {
            m_outputStream = DeferredFileOutputStream.builder() //
                .setDirectory(FileUtil.getWorkflowTempDir()) //
                .setPrefix("concatenated_binary_") //
                .setSuffix(".bin") //
                .setBufferSize((int)(128 * FileUtils.ONE_KB)) //
                .setThreshold((int)FileUtils.ONE_MB) //
                .get();
        }

        try (final var inStream = binaryData.openInputStream()) {
            inStream.transferTo(m_outputStream);
            return false;
        } catch (final IOException e) {
            LOGGER.error("Failed to write binary cell to output stream", e);
            resetInternal();
            setSkipMessage("Could not write to temporary file: " + e.getMessage());
            return true;
        }
    }

    @Override
    protected DataCell getResultInternal() {
        final var outputStream = m_outputStream;
        if (outputStream == null) {
            if (isSkipped() || inclMissingCells()) {
                // if the user chose to include missing cells, we return a missing cell
                return DataType.getMissingCell();
            }

            try {
                // all cells were missing, return empty binary object
                return m_factory.create(new byte[0]);
            } catch (IOException e) {
                // cannot happen, short arrays always result in in-memory binary objects
                throw new IllegalStateException(e);
            }
        }

        try {
            outputStream.close();
            try (final var concatenatedStream = outputStream.toInputStream()) {
                return m_factory.create(concatenatedStream);
            }
        } catch (final IOException e) {
            LOGGER.error("Failed to create binary object cell", e);
            return new MissingCell("Could not create result cell: " + e.getMessage());
        } finally {
            resetInternal();
        }
    }

    @Override
    protected void resetInternal() {
        final var outputStream = m_outputStream;
        if (outputStream == null) {
            return;
        }

        m_outputStream = null;
        try {
            outputStream.close();
        } catch (IOException e) {
            LOGGER.error("Error while closing output stream", e);
        } finally {
            // delete the temporary file if it was created
            FileUtils.deleteQuietly(outputStream.getFile());
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj); // to make Sonar happy
    }

    @Override
    public int hashCode() {
        return super.hashCode(); // to make Sonar happy
    }
}
