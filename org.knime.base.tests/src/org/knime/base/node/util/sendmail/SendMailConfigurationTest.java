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
 *   20 Oct 2021 (Dionysios Stolis): created
 */
package org.knime.base.node.util.sendmail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;

/**
 * Contains unit tests for {@link SendMailConfiguration}}
 *
 * @author Dionysios Stolis, KNIME Gmbh, Berlin, Germany
 */
class SendMailConfigurationTest {

    private final SendMailConfiguration m_mailConfiguration = new SendMailConfiguration();
    private final SendMailNodeModel m_mailModel = new SendMailNodeModel();

    private static final String BODY = "This is my email body for the test case";

    private static final String SUBJECT = "This is my subject";

    private final GreenMail m_mailServer = new GreenMail(ServerSetupTest.SMTP);


    @BeforeEach
    void setup() {
        m_mailServer.setUser("knime", "test123");
        setBasicMailConfiguration();
        m_mailServer.start();
    }

    @AfterEach
    void stopMailServer() {
        m_mailServer.stop();
    }

    @Test
    void testSendSimpleEmail() throws MessagingException, IOException, InvalidSettingsException {
        m_mailConfiguration.send(m_mailModel, null);

        MimeMessage received = m_mailServer.getReceivedMessages()[0];
        basicMailChecks(received);
        assertTrue(m_mailServer.waitForIncomingEmail(5000, 1));
    }

    @Test
    void testEmailWithMultipleRecipients() throws MessagingException, IOException, InvalidSettingsException {
        m_mailConfiguration.setCc("cc_testing@knime.eu");
        m_mailConfiguration.setBcc("bcc_testing@knime.eu");
        m_mailConfiguration.send(m_mailModel, null);

        MimeMessage received = m_mailServer.getReceivedMessages()[0];
        basicMailChecks(received);
        assertEquals("cc_testing@knime.eu", received.getRecipients(RecipientType.CC)[0].toString());

        // BCC address is not contained in the message since other receivers are not allowed to know
        assertNull(received.getRecipients(RecipientType.BCC));
        // expected mails = 3 = TO + CC + BCC
        assertTrue(m_mailServer.waitForIncomingEmail(5000, 3));
    }

    @Test
    void testEmailWithMultipleRecipients_replyTo() throws MessagingException, IOException, InvalidSettingsException {
        m_mailConfiguration.setTo("testing@knime.eu, to2_testing@knime.eu");
        m_mailConfiguration.setCc("cc_testing@knime.eu, cc2_testing@knime.eu");
        m_mailConfiguration.setBcc("bcc_testing@knime.eu, bcc2_testing@knime.eu");
        m_mailConfiguration.setReplyTo("replyTo_testing@knime.eu");
        m_mailConfiguration.send(m_mailModel, null);

        MimeMessage received = m_mailServer.getReceivedMessages()[0];
        basicMailChecks(received);
        assertEquals("to2_testing@knime.eu", received.getRecipients(RecipientType.TO)[1].toString());
        assertEquals("cc_testing@knime.eu", received.getRecipients(RecipientType.CC)[0].toString());
        assertEquals("cc2_testing@knime.eu", received.getRecipients(RecipientType.CC)[1].toString());
        assertEquals("replyTo_testing@knime.eu", received.getReplyTo()[0].toString());

        // BCC address is not contained in the message since other receivers are not allowed to know
        assertNull(received.getRecipients(RecipientType.BCC));
        // expected mails = 6 = (2 * TO) + (2 * CC) + (2 * BCC)
        assertTrue(m_mailServer.waitForIncomingEmail(5000, 6));
    }

    private static void basicMailChecks(final MimeMessage mail) throws MessagingException {
        String actualBody = GreenMailUtil.getBody(mail);
        String actualSubject = mail.getSubject();

        assertEquals(SUBJECT, actualSubject);
        assertTrue(actualBody.contains(BODY));
        assertEquals("testing@knime.eu", mail.getRecipients(RecipientType.TO)[0].toString());
    }

    private void setBasicMailConfiguration() {
        m_mailConfiguration.setFrom("test@knime.testing");
        m_mailConfiguration.setText(BODY);
        m_mailConfiguration.setSubject(SUBJECT);
        m_mailConfiguration.setSmtpHost(ServerSetupTest.SMTP.getBindAddress());
        m_mailConfiguration.setSmtpPort(ServerSetupTest.SMTP.getPort());
        m_mailConfiguration.setTo("testing@knime.eu");
        m_mailConfiguration.setUseAuthentication(true);
        m_mailConfiguration.setSmtpUser("knime");
        m_mailConfiguration.setSmtpPassword("test123");
    }

}
