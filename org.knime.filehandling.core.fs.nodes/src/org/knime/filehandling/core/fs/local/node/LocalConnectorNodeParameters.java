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

package org.knime.filehandling.core.fs.local.node;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FSConnectionProvider;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.SingleFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.WithCustomFileSystem;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidation;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.filehandling.core.connections.DefaultFSConnectionFactory;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;

/**
 * Node parameters for Local File System Connector.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class LocalConnectorNodeParameters implements NodeParameters {

    @Widget(title = "Use custom working directory",
        description = """
                Whether to set a custom working directory or not. Unless this option is set, the working
                directory will be the home directory of the current operating system user.""")
    @ValueReference(UseCustomWorkingDirectoryRef.class)
    boolean m_useCustomWorkingDirectory;

    static class UseCustomWorkingDirectoryRef implements ParameterReference<Boolean> {
    }

    @Widget(title = "Working directory",
        description = """
            Specify the working directory of the resulting file system connection. The working
            directory must be specified as an absolute path, for example C:\\Users\\joe. A working
            directory allows downstream nodes to access files/folders using relative paths.""")
    @FileSelectionWidget(SingleFileSelectionMode.FOLDER)
    @Effect(predicate = ShowCustomWorkingDirectory.class, type = EffectType.SHOW)
    @ValueReference(WorkingDirectoryRef.class)
    @WithCustomFileSystem(connectionProvider = FileSystemConnectionProvider.class)
    @CustomValidation(WorkingDirectoryValidation.class)
    String m_workingDirectory = "";

    static class WorkingDirectoryRef implements ParameterReference<String> {
    }

    static final class WorkingDirectoryValidation implements CustomValidationProvider<String> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
        }

        @Override
        public ValidationCallback<String> computeValidationCallback(final NodeParametersInput parametersInput) {
            return WorkingDirectoryValidation::validateWorkingDirectory;
        }

        static void validateWorkingDirectory(final String workingDirectory) throws InvalidSettingsException {
            try {
                CheckUtils.checkSetting( //
                    !StringUtils.isBlank(workingDirectory) && Paths.get(workingDirectory).isAbsolute(), //
                    "Working directory must be set to an absolute path.");
            } catch (InvalidPathException e) {
                throw new InvalidSettingsException("Invalid working directory: " + e.getMessage(), e);
            }
        }
    }

    static final class FileSystemConnectionProvider implements StateProvider<FSConnectionProvider> {

        private Supplier<String> m_workingDirectorySupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_workingDirectorySupplier = initializer.computeFromValueSupplier(WorkingDirectoryRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public FSConnectionProvider computeState(final NodeParametersInput parametersInput) {
            return () -> { // NOSONAR: Longer lambda acceptable, as it improves readability
                try {
                    WorkingDirectoryValidation.validateWorkingDirectory(m_workingDirectorySupplier.get());
                    return DefaultFSConnectionFactory.createLocalFSConnection(m_workingDirectorySupplier.get());
                } catch (InvalidSettingsException e) { // NOSONAR
                    return DefaultFSConnectionFactory.createLocalFSConnection(true);
                }
            };
        }
    }

    static final class ShowCustomWorkingDirectory implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseCustomWorkingDirectoryRef.class).isTrue();
        }
    }

    void validateOnConfigure() throws InvalidSettingsException {
        if (m_useCustomWorkingDirectory) {
            WorkingDirectoryValidation.validateWorkingDirectory(m_workingDirectory);
        }
    }
}
