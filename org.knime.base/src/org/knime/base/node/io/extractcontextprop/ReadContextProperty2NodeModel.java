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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.io.extractcontextprop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.util.ContextProperties;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class ReadContextProperty2NodeModel extends WebUINodeModel<ReadContextProperty2NodeSettings>  {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ReadContextProperty2NodeModel.class);

    private static final String PROP_COMPATIBILITY_MODE = "org.knime.base.extractcontextproperties.compatibilitymode";

    private static final Set<String> PRE_53_PROPERTIES = Set.of(
        ContextProperties.CONTEXT_PROPERTY_WORKFLOW_NAME,
        ContextProperties.CONTEXT_PROPERTY_WORKFLOW_PATH,
        ContextProperties.CONTEXT_PROPERTY_WORKFLOW_ABSOLUTE_PATH,
        ContextProperties.CONTEXT_PROPERTY_SERVER_USER,
        ContextProperties.CONTEXT_PROPERTY_TEMP_LOCATION,
        ContextProperties.CONTEXT_PROPERTY_AUTHOR,
        ContextProperties.CONTEXT_PROPERTY_EDITOR,
        ContextProperties.CONTEXT_PROPERTY_CREATION_DATE,
        ContextProperties.CONTEXT_PROPERTY_LAST_MODIFIED
    );

    ReadContextProperty2NodeModel(final WebUINodeConfiguration config) {
        super(config, ReadContextProperty2NodeSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs,
            final ReadContextProperty2NodeSettings modelSettings) throws InvalidSettingsException {
        executeInternal(modelSettings);
        return new PortObjectSpec[] { FlowVariablePortObjectSpec.INSTANCE };
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
        final ReadContextProperty2NodeSettings modelSettings) throws Exception {
        executeInternal(modelSettings);
        return new PortObject[] { FlowVariablePortObject.INSTANCE };
    }

    private void executeInternal(final ReadContextProperty2NodeSettings modelSettings) throws InvalidSettingsException {
        final var props = new LinkedHashMap<String, String>();
        // if the compatibility mode is set, the node will not emit "newer" properties, if the checkbox is checked
        final boolean isCompatMode = Boolean.getBoolean(PROP_COMPATIBILITY_MODE);
        if (modelSettings.m_isExtractAllProps) {
            if (isCompatMode) {
                LOGGER.debug("Skipping newer properties due to system property");
            }
            for (String property : ContextProperties.getContextProperties()) {
                if (isCompatMode && !PRE_53_PROPERTIES.contains(property)) {
                    continue;
                }
                props.put(property, StringUtils.defaultString(ContextProperties.extractContextProperty(property)));
            }
        } else {
            final var selectedProps = modelSettings.m_selectedProps;
            if (selectedProps == null || selectedProps.length == 0) {
                throw new InvalidSettingsException("No properties selected");
            }
            final var ignored = new ArrayList<String>();
            for (final var prop : selectedProps) {
                try {
                    props.put(prop, StringUtils.defaultString(ContextProperties.extractContextProperty(prop)));
                } catch (final IllegalArgumentException ex) { // NOSONAR
                    ignored.add(prop);
                }
            }
            if (!ignored.isEmpty()) {
                setWarningMessage("Ignoring selected system properties: " + ignored);
            }
        }
        props.forEach(this::pushFlowVariableString);
    }
}
