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
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 19, 2023 (carlwitt): created
 */
package org.knime.base.node.flowvariable.credentialspropertiesextractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.CredentialsStore.CredentialsProperties;
import org.knime.core.node.workflow.VariableType.CredentialsType;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public class CredentialsPropertiesExtractorNodeModel extends WebUINodeModel<CredentialsPropertyExtractorSettings> {

    /** Credentials identifier as per {@link CredentialsProperties#name()} */
    static final DataColumnSpec NAME_COLUMN = new DataColumnSpecCreator("Credentials", StringCell.TYPE).createSpec();

    /** User identifier as per {@link CredentialsProperties#login()} */
    static final DataColumnSpec LOGIN_COLUMN = new DataColumnSpecCreator("Username", StringCell.TYPE).createSpec();

    /** Whether the variable has a non-empty password set as per {@link CredentialsProperties#isPasswordSet()}. */
    static final DataColumnSpec PASSWORD_COLUMN =
        new DataColumnSpecCreator("Includes Password", BooleanCell.TYPE).createSpec();

    /** Whether the variable has a non-empty factor set as per {@link CredentialsProperties#isSecondFactorSet()}. */
    static final DataColumnSpec FACTOR_COLUMN =
        new DataColumnSpecCreator("Includes Second Authentication Factor", BooleanCell.TYPE).createSpec();

    private static final DataTableSpec OUTPUT_SPEC =
        new DataTableSpecCreator().addColumns(NAME_COLUMN, LOGIN_COLUMN, PASSWORD_COLUMN, FACTOR_COLUMN).createSpec();

    /**
     * @param configuration
     * @param modelSettingsClass
     */
    protected CredentialsPropertiesExtractorNodeModel(final WebUINodeConfiguration configuration,
        final Class<CredentialsPropertyExtractorSettings> modelSettingsClass) {
        super(configuration, modelSettingsClass);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
        final CredentialsPropertyExtractorSettings modelSettings) throws InvalidSettingsException {
        return new PortObjectSpec[]{OUTPUT_SPEC};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inPorts, final ExecutionContext exec,
        final CredentialsPropertyExtractorSettings modelSettings) throws Exception {

        // given the flow variables
        final var flowVars = getAvailableFlowVariables(CredentialsType.INSTANCE);

        // extract all credential properties and filter output
        final var credentialsProperties = flowVars.values().stream() //
            .flatMap(fv -> CredentialsStore.CredentialsProperties.of(fv).stream()).toList();

        // CredentialsPropertyExtractorSettings.m_selectedCredentials will never have filter mode "TYPE", because we
        // only provide a ChoicesProvider and not a ColumnChoicesProvider. So, passing null for a spec is okay.
        final var selectedNames = Arrays
            .stream(modelSettings.m_selectedCredentials.filter(flowVars.keySet().toArray(String[]::new)))
            .collect(Collectors.toSet());

        // fail on missing properties if necessary
        final var exception = credentialsProperties.stream() //
            .filter(cp -> selectedNames.contains(cp.name())) //
            .flatMap(cp -> incompleteCredentialsException(modelSettings, cp).stream()) //
            .findAny();
        if (exception.isPresent()) {
            throw exception.get();
        }

        final var outputData = credentialsProperties.stream() //
            .filter(cp -> selectedNames.contains(cp.name())) //
            .toArray(CredentialsProperties[]::new);

        // create data table
        final var container = exec.createDataContainer(OUTPUT_SPEC);
        for (var i = 0; i < outputData.length; i++) {
            final var key = RowKey.createRowKey((long)i);
            container.addRowToTable(new DefaultRow(key, //
                new StringCell(outputData[i].name()), //
                new StringCell(outputData[i].login()), //
                BooleanCellFactory.create(outputData[i].isPasswordSet()), //
                BooleanCellFactory.create(outputData[i].isSecondAuthenticationFactorSet()) //
            ));
        }
        container.close();
        return new PortObject[]{container.getTable()};
    }

    /**
     * @param settings that define what is required
     * @param credentialsProperties that are tested
     * @return Empty if nothing is missing. Otherwise, an exception explaining that the credentials with name {name} are
     *         incomplete and a request to provide the missing information.
     */
    private static Optional<IllegalArgumentException> incompleteCredentialsException(
        final CredentialsPropertyExtractorSettings settings, final CredentialsProperties credentialsProperties) {

        // determine what's missing
        final var missing = new ArrayList<String>();
        if (settings.m_failOnEmptyUser && credentialsProperties.login().isEmpty()) {
            missing.add("a user name");
        }
        if (settings.m_failOnEmptyPassword && !credentialsProperties.isPasswordSet()) {
            missing.add("a password");
        }
        if (settings.m_failOnEmptySecondAuthenticationFactor
            && !credentialsProperties.isSecondAuthenticationFactorSet()) {
            missing.add("a second authentication factor");
        }

        // all good
        if (missing.isEmpty()) {
            return Optional.empty();
        }

        // construct message
        if (missing.size() == 3) {
            missing.add(1, ", ");
            missing.add(missing.size() - 1, ", and ");
        } else if (missing.size() == 2) {
            missing.add(1, " and ");
        }
        final var message = "The credentials with name '%s' are incomplete. Please provide %s."
            .formatted(credentialsProperties.name(), missing.stream().collect(Collectors.joining("")));

        return Optional.of(new IllegalArgumentException(message));
    }

}
