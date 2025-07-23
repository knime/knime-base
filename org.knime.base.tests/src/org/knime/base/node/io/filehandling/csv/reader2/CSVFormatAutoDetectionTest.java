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
 *   Mar 27, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.CharsetRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.SkipFirstLinesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.BufferSizeRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.CommentStartRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption;
import org.knime.base.node.io.filehandling.webui.LocalWorkflowContextTest;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.BaseSettings.FileSelectionRef;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.handler.WidgetHandlerException;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;

import com.univocity.parsers.csv.CsvFormat;

/**
 * Tests the behavior on autodetect button click.
 *
 * @author Paul Bärnreuther
 */
class CSVFormatAutoDetectionTest extends LocalWorkflowContextTest {

    @TempDir
    Path m_tempFolder;

    static final class TestFormatDependencies {
        final String m_commentLineCharacter;

        final long m_skipFirstLines;

        final String m_filePath;

        TestFormatDependencies(final String filePath) {
            m_commentLineCharacter = "#";
            m_skipFirstLines = 3;
            m_filePath = filePath;
        }
    }

    static final class TestFormat {

        final String m_columnDelimiter;

        final String m_customRowDelimiter;

        final String m_escapedCustomRowDelimiter;

        final String m_quoteCharacter;

        final String m_quoteEscapeCharacter;

        TestFormat() {
            m_columnDelimiter = ";";
            m_customRowDelimiter = "\r\n";
            m_escapedCustomRowDelimiter = "\\r\\n";
            m_quoteCharacter = "'";
            m_quoteEscapeCharacter = "'";
        }
    }

    @Test
    void testDetectsFormat() throws IOException, StateComputationFailureException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var testFormat = new TestFormat();
        final var testFormatDependencies = new TestFormatDependencies(file);
        writeCsvFile(testFormat, testFormatDependencies);
        final var settings = new CSVTableReaderNodeSettings();
        setFormatDependencies(settings, testFormatDependencies);

        final var detectedFormat = guessCSVFormat(settings);

        assertFormat(testFormat, detectedFormat);
    }

    @Test
    void testThrowsWidgetHandlerExceptionOnInvalidCharset() throws IOException {
        final var file = m_tempFolder.resolve("file2.csv").toAbsolutePath().toString();
        final var testFormat = new TestFormat();
        final var testFormatDependencies = new TestFormatDependencies(file);
        writeCsvFile(testFormat, testFormatDependencies);
        final var settings = new CSVTableReaderNodeSettings();
        settings.m_encoding.m_charset = new Charset(FileEncodingOption.OTHER, "Invalid custom encoding");

        assertThrows(WidgetHandlerException.class, () -> guessCSVFormat(settings));

    }

    private static void setFormatDependencies(final CSVTableReaderNodeSettings settings,
        final TestFormatDependencies testFormatDependencies) {
        settings.m_settings.m_source.m_path = new FSLocation(FSCategory.LOCAL, testFormatDependencies.m_filePath);
        settings.m_settings.m_commentLineCharacter = testFormatDependencies.m_commentLineCharacter;
        settings.m_limitRows.m_skipFirstLines = testFormatDependencies.m_skipFirstLines;
    }

    private static void writeCsvFile(final TestFormat format, final TestFormatDependencies formatDependencies)
        throws IOException {
        try (final var writer = new BufferedWriter(new FileWriter(formatDependencies.m_filePath))) {
            for (int i = 0; i < formatDependencies.m_skipFirstLines; i++) {
                writer.write("Ignored line\n");
            }
            writer.write(String.format("%sThis,is,a,comment%s", formatDependencies.m_commentLineCharacter,
                format.m_customRowDelimiter));
            writer.write(String.format("colName%scolName2%s", format.m_columnDelimiter, format.m_customRowDelimiter));
            writer.write(String.format("1%s2%s", format.m_columnDelimiter, format.m_customRowDelimiter));
            writer.write(String.format("%sWith space%s%s%s%sWithQuotes%s%s%s", //
                format.m_quoteCharacter, format.m_quoteCharacter, // Around With space
                format.m_columnDelimiter, //
                format.m_quoteEscapeCharacter, format.m_quoteCharacter, // Before WithQuotes
                format.m_quoteEscapeCharacter, format.m_quoteCharacter, // After WithQuotes
                format.m_customRowDelimiter));
        }
    }

    private static CsvFormat guessCSVFormat(final CSVTableReaderNodeSettings settings) {
        final var formatProvider = new CSVFormatProvider();
        formatProvider.init(getStateProviderInitializer(settings));
        return formatProvider.computeState(null);
    }

    private static void assertFormat(final TestFormat testFormat, final CsvFormat detectedFormat)
        throws StateComputationFailureException {
        assertThat(getProvidedState(detectedFormat, CSVTableReaderNodeSettings.Settings.ColumnDelimiterProvider::new))
            .isEqualTo(testFormat.m_columnDelimiter);

        assertThat(
            getProvidedState(detectedFormat, CSVTableReaderNodeSettings.Settings.CustomRowDelimiterProvider::new))
                .isEqualTo(testFormat.m_escapedCustomRowDelimiter);

        assertThat(getProvidedState(detectedFormat, CSVTableReaderNodeSettings.Settings.QuoteCharacterProvider::new))
            .isEqualTo(testFormat.m_quoteCharacter);

        assertThat(
            getProvidedState(detectedFormat, CSVTableReaderNodeSettings.Settings.QuoteEscapeCharacterProvider::new))
                .isEqualTo(testFormat.m_quoteEscapeCharacter);

        assertThat(
            getProvidedState(detectedFormat, CSVTableReaderNodeSettings.Settings.RowDelimiterOptionProvider::new))
                .isEqualTo(RowDelimiterOption.LINE_BREAK);

    }

    private static <T> T getProvidedState(final CsvFormat detectedFormat,
        final Supplier<CSVFormatProvider.ProviderFromCSVFormat<T>> constructor)
        throws StateComputationFailureException {
        final var providerFromCSVFormat = constructor.get();
        providerFromCSVFormat.m_csvFormatSupplier = () -> detectedFormat;
        return providerFromCSVFormat.computeState(null);
    }

    private static final StateProvider.StateProviderInitializer
        getStateProviderInitializer(final CSVTableReaderNodeSettings settings) {
        return new StateProvider.StateProviderInitializer() {

            @Override
            @SuppressWarnings("unchecked")
            public <T> Supplier<T> getValueSupplier(final Class<? extends ParameterReference<T>> ref) {
                if (ref.equals(SkipFirstLinesRef.class)) {
                    return () -> (T)(Object)settings.m_limitRows.m_skipFirstLines;
                }
                if (ref.equals(CharsetRef.class)) {
                    return () -> (T)settings.m_encoding.m_charset;
                }
                if (ref.equals(BufferSizeRef.class)) {
                    return () -> (T)(Object)settings.m_settings.m_numberOfCharactersForAutodetection;
                }
                if (ref.equals(CommentStartRef.class)) {
                    return () -> (T)settings.m_settings.m_commentLineCharacter;
                }
                if (ref.equals(FileSelectionRef.class)) {
                    return () -> (T)settings.m_settings.m_source;
                }
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
            }

            @Override
            public <T> void computeOnValueChange(final Class<? extends ParameterReference<T>> id) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public void computeOnButtonClick(final Class<? extends ButtonReference> ref) {
                // Do nothing
            }

            @Override
            public <T> Supplier<T> computeFromValueSupplier(final Class<? extends ParameterReference<T>> ref) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public void computeBeforeOpenDialog() {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public void computeAfterOpenDialog() {
                throw new IllegalAccessError("Should not be called within this test");

            }

            @Override
            public NodeParametersInput getNodeParametersInput() {
                throw new IllegalAccessError("Should not be called within this test");
            }
        };
    }

}
