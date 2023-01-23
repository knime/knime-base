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
 *   Jan 13, 2023 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.utility.nodes.pathtouri.variable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocationSpec;
import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.utility.nodes.pathtouri.exporter.AbstractURIExporterModelHelper;

/**
 * Concrete implementation of the {@link AbstractURIExporterModelHelper} to assist in the node model.
 *
 * @author Zkriya Rakhimberdiyev
 */
final class URIExporterModelHelper extends AbstractURIExporterModelHelper {

    private Supplier<Map<String, FlowVariable>> m_filteredFlowVariablesSupplier;

    URIExporterModelHelper(final Supplier<Map<String, FlowVariable>> filteredFlowVariablesSupplier, //
        final SettingsModelString selectedUriExporterModel, //
        final int fileSystemPortIndex) {

        super(selectedUriExporterModel, fileSystemPortIndex);
        m_filteredFlowVariablesSupplier = filteredFlowVariablesSupplier;
    }

    @Override
    public void validate(final Consumer<StatusMessage> warningMessageConsumer, final boolean overwriteInvalidSettings)
        throws InvalidSettingsException {
        validateFileSystemPort();
        validateFlowVariables(warningMessageConsumer, overwriteInvalidSettings);
        validateURIExporter(warningMessageConsumer, overwriteInvalidSettings);
    }

    private void validateFlowVariables(final Consumer<StatusMessage> warningMessageConsumer, final boolean overwriteInvalidSettings) throws InvalidSettingsException {
        Optional<String> errorMessage;

        if (m_filteredFlowVariablesSupplier.get().isEmpty()) {
            errorMessage = Optional.of("No flow variables selected.");
        } else if (getFileSystemPortObjectSpec() != null) {
            errorMessage = validateConnectedFSPortObjectSpec();
        } else {
            errorMessage = checkForConnectedFlowVariables();
        }
        if (errorMessage.isPresent()) {
            if (overwriteInvalidSettings) {
                warningMessageConsumer.accept(DefaultStatusMessage.mkWarning(errorMessage.get()));
            } else {
                throw new InvalidSettingsException(errorMessage.get());
            }
        }
    }

    private Optional<String> validateConnectedFSPortObjectSpec() {
        final var fsPortSpec = getFileSystemPortObjectSpec();

        if (getFlowVariablesFSLocationSpecs().stream().anyMatch(e -> !FSLocationSpec.areEqual(e, fsPortSpec.getFSLocationSpec()))) {
            return Optional.of(String.format(
                "The selected flow variables seem to contain a variable referencing a "
                    + " different file system than the one at the input port. Such paths will be"
                    + " resolved against the file system at the input port (%s).",
                    fsPortSpec.getFSLocationSpec().getFileSystemSpecifier().orElse("")));
        }
        return Optional.empty();
    }

    private Optional<String> checkForConnectedFlowVariables() {
        final FSLocationSpec connectedSpec = getFlowVariablesFSLocationSpecs() //
                .stream() //
                .filter(spec -> spec.getFSCategory() == FSCategory.CONNECTED) //
                .findAny() //
                .orElse(null);

        if (connectedSpec != null) {
            return Optional.of(String.format(
                "The selected flow variables seem to contain a path referencing a file system that requires to be connected (%s).", //
                connectedSpec.getFileSystemSpecifier().orElse("")));
        }
        return Optional.empty();
    }

    private Set<FSLocationSpec> getFlowVariablesFSLocationSpecs() {
        return m_filteredFlowVariablesSupplier.get().entrySet().stream() //
                .map(entry -> entry.getValue().getValue(FSLocationVariableType.INSTANCE)).collect(Collectors.toSet());
    }

    @Override
    protected String getUnsupportedExporterMessage(final URIExporterID exporterId) {
        return "The chosen URL format '%s' is not supported by the file system(s) of flow variables";
    }

    @Override
    protected Set<FSLocationSpec> getFSLocationSpecs() {
        if (getFileSystemPortObjectSpec() != null) {
            return Set.of(getFileSystemPortObjectSpec().getFSLocationSpec());
        }
        return getFlowVariablesFSLocationSpecs(); //
    }
}

