package org.knime.base.node.misc.jira.issue.attach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.knime.base.node.misc.jira.JiraClient;
import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.*;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObject;
import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;
import org.knime.base.node.misc.jira.shared.JiraNodePorts;

@SuppressWarnings({"restriction","deprecation"})
final class JiraAttachFileNodeModel extends WebUINodeModel<JiraAttachFileNodeSettings> {
    JiraAttachFileNodeModel(final WebUINodeConfiguration c, final Class<JiraAttachFileNodeSettings> s){ super(c, s);}    

    JiraAttachFileNodeModel(final org.knime.core.node.port.PortType[] in, final org.knime.core.node.port.PortType[] out,
            final Class<JiraAttachFileNodeSettings> s) { super(in, out, s); }

    @Override
    protected BufferedDataTable[] execute(final PortObject[] in, final ExecutionContext exec, final JiraAttachFileNodeSettings s) throws Exception {
        final var spec = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Size", LongCell.TYPE).createSpec());
        final var con = exec.createDataContainer(spec);
    final boolean hasConn = JiraNodePorts.hasConnection(in);
    if (hasConn) JiraNodePorts.validateConnectionSpec(JiraNodePorts.getConnectionSpec(in));
        final String key = s.m_key==null?"":s.m_key;
    final String filePath = s.m_filePath;
    if (filePath == null || filePath.isBlank()) throw new RuntimeException("No file path provided");
        final Path p = Path.of(filePath);
        final byte[] bytes = Files.readAllBytes(p);
        final var res = hasConn
            ? JiraClient.postAttachment((JiraConnectionPortObjectSpec)((JiraConnectionPortObject)in[0]).getSpec(), "/rest/api/3/issue/" + JiraClient.enc(key) + "/attachments", p.getFileName().toString(), bytes)
            : JiraClient.postAttachment(s.m_baseUrl, s.m_auth.m_email, s.m_auth.m_token.getPassword(), "/rest/api/3/issue/" + JiraClient.enc(key) + "/attachments", p.getFileName().toString(), bytes);
        if (res.statusCode() / 100 != 2) throw new RuntimeException("Jira attach failed: HTTP " + res.statusCode() + " body=" + res.body());
        con.addRowToTable(new DefaultRow(RowKey.createRowKey(0), new DataCell[]{ new StringCell(key), new StringCell(p.getFileName().toString()), new LongCell(bytes.length)}));
        con.close(); return new BufferedDataTable[]{ con.getTable() };
    }

    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs, final JiraAttachFileNodeSettings s) throws InvalidSettingsException {
        final var out = new DataTableSpec(
            new DataColumnSpecCreator("Key", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Size", LongCell.TYPE).createSpec());
        return new DataTableSpec[]{ out };
    }
}
