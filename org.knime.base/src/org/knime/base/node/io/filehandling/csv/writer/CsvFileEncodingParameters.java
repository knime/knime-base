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
 *   17 Dec 2025 (Thomas Reifenberger): created
 */
package org.knime.base.node.io.filehandling.csv.writer;

import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.SuggestionsProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.message.TextMessage.SimpleTextMessageProvider;

/**
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 */
class CsvFileEncodingParameters implements NodeParameters {

    private static final String DEFAULT_CHARSET = Charset.defaultCharset().name();

    private static final String DEFAULT_CHARSET_LABEL = "OS default (" + DEFAULT_CHARSET + ")";

    private static final List<String> PREDEFINED_CHARSETS =
        List.of("ISO-8859-1", "US-ASCII", "UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE");

    @Persistor(CharsetPersistor.class)
    @SuggestionsProvider(CharsetChoicesProvider.class)
    @Widget(title = "File encoding", description = """
            You can select a predefined character set (UTF-8, UTF-16, etc.), or specify any other encoding supported by
            your Java VM. The OS default uses the default encoding of the Java VM, which may depend on the locale or the
            Java property "file.encoding". """)
    @ValueReference(FileEncodingRef.class)
    String m_fileEncoding = "UTF-8";

    @TextMessage(CharacterSetValidationMessage.class)
    Void m_characterSetValidationMessage;

    private static class FileEncodingRef implements ParameterReference<String> {
    }

    static class CharsetChoicesProvider implements StringChoicesProvider {

        @Override
        public List<String> choices(final NodeParametersInput context) {
            return Stream.concat(List.of(DEFAULT_CHARSET_LABEL).stream(), PREDEFINED_CHARSETS.stream())
                .collect(Collectors.toList());
        }
    }

    static class CharsetPersistor implements NodeParametersPersistor<String> {
        private static String getConfigKey() {
            return CSVWriter2Config.CFG_CHAR_ENCODING;
        }

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var value = settings.getString(getConfigKey());

            if (value == null) {
                return DEFAULT_CHARSET_LABEL;
            }
            return value;
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            if (DEFAULT_CHARSET_LABEL.equals(param) || param == null || param.isEmpty()) {
                settings.addString(getConfigKey(), null);
            } else {
                settings.addString(getConfigKey(), param);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{getConfigKey()}};
        }

    }

    private static class CharacterSetValidationMessage implements SimpleTextMessageProvider {

        Supplier<String> m_fileEncodingSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_fileEncodingSupplier = initializer.computeFromValueSupplier(FileEncodingRef.class);
        }

        @Override
        public boolean showMessage(final NodeParametersInput context) {
            return !isCharsetValid(m_fileEncodingSupplier.get());
        }

        @Override
        public String title() {
            return "Invalid character set";
        }

        @Override
        public String description() {
            return "The specified character set '" + m_fileEncodingSupplier.get() + "' is not supported.";
        }

        @Override
        public MessageType type() {
            return MessageType.INFO;
        }
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (!isCharsetValid(m_fileEncoding)) {
            throw new InvalidSettingsException(
                "The specified character set '" + m_fileEncoding + "' is not supported.");
        }
    }

    private static boolean isCharsetValid(final String label) {
        try {
            Charset.forName(getCharsetFromLabel(label));
            return true;
        } catch (IllegalArgumentException e) { // NOSONAR
            return false;
        }
    }

    private static String getCharsetFromLabel(final String label) {
        if (label == null || DEFAULT_CHARSET_LABEL.equals(label)) {
            return DEFAULT_CHARSET;
        }
        return label;
    }

}
