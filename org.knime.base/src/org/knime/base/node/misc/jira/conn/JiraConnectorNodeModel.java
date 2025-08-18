package org.knime.base.node.misc.jira.conn;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

@SuppressWarnings({"restriction","deprecation"})
final class JiraConnectorNodeModel extends WebUINodeModel<JiraConnectorNodeSettings> {

    JiraConnectorNodeModel(final WebUINodeConfiguration c, final Class<JiraConnectorNodeSettings> s) {
        super(new PortType[0], new PortType[]{ PortTypeRegistry.getInstance().getPortType(JiraConnectionPortObject.class) }, s);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final JiraConnectorNodeSettings s)
            throws InvalidSettingsException {
        final var spec = new JiraConnectionPortObjectSpec();
        spec.m_baseUrl = s.m_baseUrl == null ? "" : s.m_baseUrl.trim();
        // Basic validation to prevent downstream URI errors
        if (spec.m_baseUrl.isBlank() || !(spec.m_baseUrl.startsWith("http://") || spec.m_baseUrl.startsWith("https://"))) {
            throw new InvalidSettingsException("Base URL must start with http:// or https:// in the Jira Connector.");
        }
        spec.m_email = s.m_email == null ? "" : s.m_email.trim();
        spec.m_token = s.m_token != null ? s.m_token.getPassword() : "";
        return new PortObjectSpec[]{ spec };
    }

    @Override
    protected org.knime.core.node.port.PortObject[] execute(final org.knime.core.node.port.PortObject[] inObjects,
            final ExecutionContext exec, final JiraConnectorNodeSettings s) throws Exception {
        final var spec = (JiraConnectionPortObjectSpec)configure(new PortObjectSpec[0], s)[0];
        return new org.knime.core.node.port.PortObject[]{ new JiraConnectionPortObject(spec) };
    }
}
