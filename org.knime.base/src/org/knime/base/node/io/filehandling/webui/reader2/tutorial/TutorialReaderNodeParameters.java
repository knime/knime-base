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
 */
package org.knime.base.node.io.filehandling.webui.reader2.tutorial;

import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.reader2.FileSelectionPath;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;

/**
 *
 * @author KNIME AG, Zurich, Switzerland
 */
@Modification(TutorialReaderNodeParameters.SetTutorialExtensions.class)
@SuppressWarnings("restriction")
class TutorialReaderNodeParameters implements NodeParameters {

    TutorialReaderNodeParameters(final NodeParametersInput input) {
        this(input.getURLConfiguration());
    }

    TutorialReaderNodeParameters(final NodeCreationConfiguration nodeCreationConfig) {
        this(nodeCreationConfig.getURLConfig());
    }

    private TutorialReaderNodeParameters(final Optional<? extends URLConfiguration> urlConfig) {
        if (urlConfig.isPresent()) {
            final var url = urlConfig.get().getUrl();
            m_readerParameters = new ReaderParameters(url);
            m_tutorialReaderParameters = new TutorialReaderParameters(url);
        }
    }

    TutorialReaderNodeParameters() {
        // default constructor
    }

    // TODO (#6): Set the file extensions for your reader
    static final class SetTutorialExtensions extends ReaderParameters.SetFileReaderWidgetExtensions {
        @Override
        protected String[] getExtensions() {
            return new String[]{"TODO (#6)"};
        }
    }

    public static final class ReaderParametersRef implements ParameterReference<ReaderParameters> {
    }

    @ValueReference(ReaderParametersRef.class)
    ReaderParameters m_readerParameters = new ReaderParameters();

    public static final class TutorialReaderParametersRef implements ParameterReference<TutorialReaderParameters> {
    }

    @ValueReference(TutorialReaderParametersRef.class)
    TutorialReaderParameters m_tutorialReaderParameters = new TutorialReaderParameters();

    TutorialReaderTransformationParameters m_transformationParameters = new TutorialReaderTransformationParameters();

    void saveToSource(final FileSelectionPath sourceSettings) {
        m_readerParameters.saveToSource(sourceSettings);
    }

    void saveToConfig(final DummyMultiTableReadConfig config) {
        m_readerParameters.saveToConfig(config);
        m_tutorialReaderParameters.saveToConfig(config);
        final var configID = config.getConfigID();
        m_transformationParameters.saveToConfig(//
            config, m_readerParameters.m_source.m_path.getPath(), //
            configID, //
            m_readerParameters.m_howToCombineColumns, //
            m_readerParameters.m_appendPathColumn //
        );
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_readerParameters.validate();
        m_tutorialReaderParameters.validate();
        m_transformationParameters.validate();
    }

}
