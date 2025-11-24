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
 *   Mar 26, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.csv.reader2.AutoDetectCSVFormatParameters.AutoDetectButtonRef;
import org.knime.base.node.io.filehandling.csv.reader2.AutoDetectCSVFormatParameters.BufferSizeRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVFormatParameters.CommentStartRef;
import org.knime.base.node.io.filehandling.csv.reader2.FileEncodingParameters.CustomEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.FileEncodingParameters.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.FileEncodingParameters.FileEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.SkipFirstLinesOfFileParameters.SkipFirstLinesRef;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileSelectionParameters.FileSelectionRef;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.DefaultFileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;

import com.univocity.parsers.csv.CsvFormat;

/**
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
final class CSVFormatProvider extends CSVFormatAutoDetector implements StateProvider<CsvFormat> {

    /**
     * Used in {@link CSVTableReaderNodeSettings} to set settings values depending on a newly auto-guesses CSV format
     */
    abstract static class ProviderFromCSVFormat<T> implements StateProvider<T> {

        /**
         * Package scoped for tests
         */
        Supplier<CsvFormat> m_csvFormatSupplier;

        /**
         * {@inheritDoc}
         */
        @Override
        public void init(final StateProviderInitializer initializer) {
            m_csvFormatSupplier = initializer.computeFromProvidedState(CSVFormatProvider.class);
        }

        protected final CsvFormat getCsvFormat() {
            return m_csvFormatSupplier.get();
        }
    }

    // Supplier of values of settings on which the auto detection depends

    Supplier<Long> m_skipFirstLinesSupplier;

    Supplier<FileEncodingOption> m_fileEncodingSupplier;

    Supplier<String> m_customEncodingSupplier;

    Supplier<Integer> m_bufferSizeSupplier;

    Supplier<String> m_commentStartSupplier;

    Supplier<MultiFileSelection<DefaultFileChooserFilters>> m_fileSelectionSupplier;

    @Override
    protected long getNumLinesToSkip() {
        return m_skipFirstLinesSupplier.get();
    }

    @Override
    protected boolean isSkipLines() {
        return m_skipFirstLinesSupplier.get() > 0;
    }

    @Override
    protected Charset getSelectedCharset() throws InvalidSettingsException {
        try {
            final var charsetName = FileEncodingParameters.fileEncodingToCharsetName(m_fileEncodingSupplier.get(),
                m_customEncodingSupplier.get());
            return charsetName == null ? Charset.defaultCharset() : Charset.forName(charsetName);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new InvalidSettingsException(e);
        }
    }

    @Override
    protected int getBufferSize() {
        return m_bufferSizeSupplier.get();
    }

    @Override
    protected String getCommentStart() {
        return m_commentStartSupplier.get();
    }

    @Override
    protected MultiFileSelection<DefaultFileChooserFilters> getMultiFileSelection() {
        return m_fileSelectionSupplier.get();
    }

    @Override
    public void init(final StateProviderInitializer initializer) {
        // Trigger
        initializer.computeOnButtonClick(AutoDetectButtonRef.class);

        // Dependencies
        m_fileSelectionSupplier = initializer.getValueSupplier(FileSelectionRef.class);
        m_skipFirstLinesSupplier = initializer.getValueSupplier(SkipFirstLinesRef.class);
        m_fileEncodingSupplier = initializer.getValueSupplier(FileEncodingRef.class);
        m_customEncodingSupplier = initializer.getValueSupplier(CustomEncodingRef.class);
        m_bufferSizeSupplier = initializer.getValueSupplier(BufferSizeRef.class);
        m_commentStartSupplier = initializer.getValueSupplier(CommentStartRef.class);
    }

    @Override
    public CsvFormat computeState(final NodeParametersInput context) {
        try {
            return detectFormat(FileSystemPortConnectionUtil.getFileSystemConnection(context));
        } catch (IOException | InvalidSettingsException e) { // NOSONAR
            throw new WidgetHandlerException(e.getMessage());
        }
    }

}
