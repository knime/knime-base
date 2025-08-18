package org.knime.base.node.misc.jira.shared;

import org.knime.base.node.misc.jira.conn.JiraConnectionPortObjectSpec;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;

/**
 * Shared UI helpers for Jira nodes: predicate to detect a connected Jira connection and an info banner provider.
 */
@SuppressWarnings("restriction")
public final class JiraUI {
    private JiraUI() {}

    /** UI predicate that is true when a Jira connection port is connected. */
    public static final class JiraConnectionPresent implements EffectPredicateProvider {
        @Override
        public org.knime.node.parameters.updates.EffectPredicate init(
                final org.knime.node.parameters.updates.EffectPredicateProvider.PredicateInitializer i) {
            return i.getConstant(inp -> {
                try {
                    var specs = inp.getInPortSpecs();
                    return specs != null && specs.length > 0 && specs[0] instanceof JiraConnectionPortObjectSpec;
                } catch (Exception e) {
                    return false;
                }
            });
        }
    }

    /** Info banner shown when a Jira Connection is connected. */
    public static final class JiraConnectionInfo implements TextMessage.SimpleTextMessageProvider {
        @Override
        public boolean showMessage(final NodeParametersInput context) {
            return context != null && context.getInPortSpecs() != null && context.getInPortSpecs().length > 0
                    && context.getInPortSpecs()[0] instanceof JiraConnectionPortObjectSpec;
        }

        @Override
        public String title() {
            return "Jira connection managed by input port";
        }

        @Override
        public String description() {
            return "A Jira Connection is connected. Base URL and Credentials are taken from the connection and are used to fetch projects and fields.";
        }

        @Override
        public MessageType type() { return MessageType.INFO; }
    }
}
