package org.knime.base.node.preproc.pmml.missingval.compute;

import java.io.IOException;
import java.util.Map;

import org.apache.xmlbeans.XmlException;
import org.knime.base.node.preproc.pmml.missingval.utils.MissingValueNodeDescriptionHelper;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.core.webui.node.dialog.NodeDialog;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultKaiNodeInterface;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterface;
import org.knime.core.webui.node.dialog.kai.KaiNodeInterfaceFactory;
import org.xml.sax.SAXException;

/**
 * <code>NodeFactory</code> for the "CompiledModelReader" Node.
 *
 * @author Alexander Fillbrunn
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 * @noreference This class is not intended to be referenced by clients.
 */
@SuppressWarnings("restriction")
public class MissingValueHandlerNodeFactory extends NodeFactory<MissingValueHandlerNodeModel>
    implements NodeDialogFactory, KaiNodeInterfaceFactory {

    private static NodeDescription m_description;

    /**
     * {@inheritDoc}
     */
    @Override
    public MissingValueHandlerNodeModel createNodeModel() {
        return new MissingValueHandlerNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<MissingValueHandlerNodeModel> createNodeView(final int viewIndex,
        final MissingValueHandlerNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return NodeDialogManager.createLegacyFlowVariableNodeDialog(createNodeDialog());
    }

    @Override
    public NodeDialog createNodeDialog() {
        return new DefaultNodeDialog(SettingsType.MODEL, MissingValueHandlerNodeParameters.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        // TODO remove once we have a more general description cache implemented with AP-15784
        synchronized (MissingValueHandlerNodeFactory.class) {
            if (m_description == null) {
                m_description = MissingValueNodeDescriptionHelper.createNodeDescription(super.createNodeDescription());
            }
        }
        return m_description;
    }

    @Override
    public KaiNodeInterface createKaiNodeInterface() {
        return new DefaultKaiNodeInterface(Map.of(SettingsType.MODEL, MissingValueHandlerNodeParameters.class));
    }
}
