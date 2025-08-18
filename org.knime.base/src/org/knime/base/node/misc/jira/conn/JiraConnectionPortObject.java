package org.knime.base.node.misc.jira.conn;

import java.io.IOException;

import javax.swing.JComponent;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.AbstractPortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;

@SuppressWarnings({"restriction","deprecation"})
public final class JiraConnectionPortObject extends AbstractPortObject {

    public static final class Serializer extends AbstractPortObjectSerializer<JiraConnectionPortObject> {}

    private JiraConnectionPortObjectSpec m_spec;

    public JiraConnectionPortObject() {}

    public JiraConnectionPortObject(final JiraConnectionPortObjectSpec spec) {
        m_spec = spec;
    }

    @Override
    public String getSummary() { return "Jira Connection"; }

    @Override
    public PortObjectSpec getSpec() { return m_spec; }

    @Override
    protected void save(final PortObjectZipOutputStream out, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException { /* nothing extra */ }

    @Override
    protected void load(final PortObjectZipInputStream in, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException { m_spec = (JiraConnectionPortObjectSpec)spec; }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        // TODO Auto-generated method stub
        return null;
    }
}
