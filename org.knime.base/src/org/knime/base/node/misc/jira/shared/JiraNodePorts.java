package org.knime.base.node.misc.jira.shared;

import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;

/**
 * Small helpers for Jira nodes to work with the optional Jira Connection port.
 */
@SuppressWarnings("restriction")
public final class JiraNodePorts {
    private JiraNodePorts() {}

    /** Returns true if in[0] is a JiraConnectionPortObject. */
    public static boolean hasConnection(final PortObject[] in) {
        return in != null && in.length > 0 && in[0] instanceof JiraConnectionPortObject;
    }

    /** Returns the Jira connection spec from in[0] or null if not present. */
    public static JiraConnectionPortObjectSpec getConnectionSpec(final PortObject[] in) {
        if (!hasConnection(in)) return null;
        final var po = (JiraConnectionPortObject) in[0];
        return (JiraConnectionPortObjectSpec) po.getSpec();
    }

    /** Basic validation for baseUrl in a connected spec. */
    public static void validateConnectionSpec(final JiraConnectionPortObjectSpec spec) throws InvalidSettingsException {
        if (spec == null || spec.m_baseUrl == null || spec.m_baseUrl.isBlank()) {
            throw new InvalidSettingsException(
                "Connected Jira Connection has no Base URL set. Configure the Jira Connector.");
        }
        if (!(spec.m_baseUrl.startsWith("http://") || spec.m_baseUrl.startsWith("https://"))) {
            throw new InvalidSettingsException(
                "Connected Jira Connection Base URL must start with http:// or https://");
        }
    }

    /** Resolves the base URL preferring the connection if available, otherwise the provided fallback. */
    public static String resolveBaseUrl(final PortObject[] in, final String fallback) {
        final var spec = getConnectionSpec(in);
        return spec != null ? spec.m_baseUrl : fallback;
    }
}
