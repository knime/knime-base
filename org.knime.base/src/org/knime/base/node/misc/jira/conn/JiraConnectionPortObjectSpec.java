package org.knime.base.node.misc.jira.conn;

import java.io.IOException;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;

@SuppressWarnings("restriction")
public final class JiraConnectionPortObjectSpec implements PortObjectSpec {

    public static final class Serializer extends PortObjectSpecSerializer<JiraConnectionPortObjectSpec> {
        private static final String KEY = "jira-connection";
        private static final String K_BASE = "baseUrl";
        private static final String K_EMAIL = "email";
        private static final String K_TOKEN = "token";

        @Override
        public void savePortObjectSpec(final JiraConnectionPortObjectSpec spec,
                final PortObjectSpecZipOutputStream out) throws IOException {
            out.putNextEntry(new ZipEntry(KEY));
            final var mc = new ModelContent(KEY);
            mc.addString(K_BASE, spec.m_baseUrl == null ? "" : spec.m_baseUrl);
            mc.addString(K_EMAIL, spec.m_email == null ? "" : spec.m_email);
            mc.addString(K_TOKEN, spec.m_token == null ? "" : spec.m_token);
            mc.saveToXML(new NonClosableOutputStream.Zip(out));
        }

        @Override
        public JiraConnectionPortObjectSpec loadPortObjectSpec(final PortObjectSpecZipInputStream in)
                throws IOException {
            final var ze = in.getNextEntry();
            if (ze == null || !KEY.equals(ze.getName())) {
                throw new IOException("Unexpected or missing zip entry for Jira connection spec: "
                        + (ze != null ? ze.getName() : "<none>"));
            }
            try {
                final ModelContentRO mc = ModelContent.loadFromXML(new NonClosableInputStream.Zip(in));
                final var s = new JiraConnectionPortObjectSpec();
                s.m_baseUrl = mc.containsKey(K_BASE) ? mc.getString(K_BASE) : "";
                s.m_email = mc.containsKey(K_EMAIL) ? mc.getString(K_EMAIL) : "";
                s.m_token = mc.containsKey(K_TOKEN) ? mc.getString(K_TOKEN) : "";
                return s;
            } catch (InvalidSettingsException e) {
                throw new IOException("Failed to read Jira connection spec settings", e);
            }
        }
    }

    public String m_baseUrl = "";
    public String m_email = "";
    public String m_token = "";
    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent[] getViews() {
        // TODO Auto-generated method stub
        return null;
    }
}
