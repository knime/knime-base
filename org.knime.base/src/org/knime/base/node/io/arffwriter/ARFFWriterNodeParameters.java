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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.io.arffwriter;

import static org.knime.base.node.io.filehandling.webui.OutputFileMessageProvider.LOCAL_URL_PATTERN;
import static org.knime.base.node.io.filehandling.webui.OutputFileMessageProvider.URL_PATTERN;

import org.knime.base.node.io.filehandling.webui.OutputFileMessageProvider;
import org.knime.base.node.io.filehandling.webui.OutputFileMessageProvider.OutputFileRef;
import org.knime.base.node.io.filehandling.webui.OverwritePolicy;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSystemOption;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithFileSystem;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.message.TextMessage;

/**
 * Node parameters for ARFF Writer.
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class ARFFWriterNodeParameters implements NodeParameters {

    @Widget(title = "Output file location", description = "The destination location. Can also be a URL.")
    @Persist(configKey = ARFFWriterNodeModel.CFGKEY_FILENAME)
    @FileWriterWidget(fileExtension = "arff")
    @WithFileSystem(FileSystemOption.LOCAL)
    @ValueReference(OutputFileRef.class)
    @Migrate(loadDefaultIfAbsent = true)
    String m_outputFile = "";

    @TextMessage(value = OutputFileMessageProvider.class)
    Void m_invalidSchemeMessage;

    @Widget(title = "If output file already exists", description = "How to handle local output file already existing.")
    @Persistor(OverwritePolicyPersistor.class)
    @Effect(predicate = HideIfIsRemoteURL.class, type = EffectType.HIDE)
    @ValueSwitchWidget
    OverwritePolicy m_overwrite = OverwritePolicy.PREVENT;

    private static class HideIfIsRemoteURL implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getString(OutputFileRef.class).matchesPattern(URL_PATTERN)
                .and(i.getString(OutputFileRef.class).matchesPattern(LOCAL_URL_PATTERN).negate());
        }
    }

    private static final class OverwritePolicyPersistor extends EnumBooleanPersistor<OverwritePolicy> {
        public OverwritePolicyPersistor() {
            super(ARFFWriterNodeModel.CFGKEY_OVERWRITE_OK, OverwritePolicy.class, OverwritePolicy.OVERWRITE);
        }
    }
}
