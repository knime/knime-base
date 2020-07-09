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
 * -------------------------------------------------------------------
 *
 * History
 *   Dec 17, 2005 (wiswedel): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.FlowVariableModelButton;
import org.knime.core.node.util.ConvenientComboBoxRenderer;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.core.node.util.FileSystemBrowser.FileSelectionMode;
import org.knime.core.node.util.FileSystemBrowserExtension;
import org.knime.core.node.util.LocalFileSystemBrowser;
import org.knime.core.node.util.StringHistory;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeContext;

/**
 * Panel that contains an editable Combo Box showing the file to write to and a button to trigger a file chooser. The
 * elements in the combo are files that have been recently used.
 *
 * @see org.knime.core.node.util.StringHistory
 * @author Bernd Wiswedel, University of Konstanz
 * @noreference non-public API
 * @noinstantiate non-public API
 */
@SuppressWarnings("serial")
public final class FilesHistoryPanel extends JPanel {

    private final List<ChangeListener> m_changeListener = new ArrayList<ChangeListener>();

    private final JComboBox<String> m_textBox;

    private final JButton m_chooseButton;

    private String[] m_suffixes;

    private final String m_historyID;

    private final FlowVariableModelButton m_flowVariableButton;

    private final FileSelectionMode m_selectMode;

    private final DialogType m_dialogType;

    private String m_forcedFileExtensionOnSave = null;

    /** See {@link #setAllowSystemPropertySubstitution(boolean)}. */
    private boolean m_allowSystemPropertySubstitution = false;

    private FileSystemBrowser m_fileSystemBrowser;

    /**
     * Creates new instance, sets properties, for instance renderer, accordingly.
     *
     * @param historyID identifier for the string history, see {@link StringHistory}
     * @param suffixes the set of suffixes for the file chooser
     * @param fvm model to allow to use a variable instead of the text field.
     * @param fileSystemBrowser The FileSystemBrowser used to browser the specified FileSystem.
     * @param selectMode
     * @param dialogType
     */
    @SuppressWarnings("unchecked")
    public FilesHistoryPanel(final FlowVariableModel fvm, final String historyID,
        final FileSystemBrowser fileSystemBrowser, final FileSelectionMode selectMode, final DialogType dialogType,
        final String... suffixes) {

        if (historyID == null || suffixes == null) {
            throw new IllegalArgumentException("Argument must not be null.");
        }
        if (Arrays.asList(suffixes).contains(null)) {
            throw new IllegalArgumentException("Array must not contain null.");
        }

        m_historyID = historyID;
        m_suffixes = suffixes;
        m_fileSystemBrowser = fileSystemBrowser;
        m_selectMode = selectMode;
        m_dialogType = dialogType;

        m_textBox = new JComboBox<String>(new DefaultComboBoxModel<String>());
        m_textBox.setEditable(true);
        m_textBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        m_textBox.setPreferredSize(new Dimension(300, 25));
        m_textBox.setRenderer(new ConvenientComboBoxRenderer());

        m_chooseButton = new JButton("Browse...");

        m_flowVariableButton = new FlowVariableModelButton(fvm);

        initEventListeners(fvm);
        initLayout();
        fileLocationChanged();
        updateHistory();
    }

    private void initLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(m_textBox, c);

        c.gridx++;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 5, 0, 0);
        add(m_chooseButton, c);

        c.gridx++;
        add(m_flowVariableButton, c);
    }

    private void initEventListeners(final FlowVariableModel fvm) {
        m_textBox.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                fileLocationChanged();
            }
        });

        m_textBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                fileLocationChanged();
            }
        });

        final Component editor = m_textBox.getEditor().getEditorComponent();
        if (editor instanceof JTextComponent) {
            Document d = ((JTextComponent)editor).getDocument();
            d.addDocumentListener(new DocumentListener() {
                @Override
                public void changedUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                @Override
                public void insertUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }

                @Override
                public void removeUpdate(final DocumentEvent e) {
                    fileLocationChanged();
                }
            });
        }

        m_chooseButton.addActionListener((e) -> {
            final String newFile = getOutputFileName();
            if (newFile != null) {
                m_textBox.setSelectedItem(newFile);
                StringHistory.getInstance(m_historyID).add(newFile);
                fileLocationChanged();
            }
        });

        fvm.addChangeListener((e) -> {
            FlowVariableModel wvm = (FlowVariableModel)(e.getSource());
            boolean variableReplacementEnabled = wvm.isVariableReplacementEnabled();
            m_textBox.setEnabled(!variableReplacementEnabled);
            m_chooseButton.setEnabled(!variableReplacementEnabled);
            if (variableReplacementEnabled) {
                // if the location is overwritten by a variable show its value
                wvm.getVariableValue().ifPresent(fv -> setSelectedFile(fv.getStringValue()));
            }
            fileLocationChanged();
        });

        this.addAncestorListener(new AncestorListener() {

            @Override
            public void ancestorRemoved(final AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(final AncestorEvent event) {
            }

            @Override
            public void ancestorAdded(final AncestorEvent event) {
                if (fvm.isVariableReplacementEnabled() && fvm.getVariableValue().isPresent()) {
                    String newPath = fvm.getVariableValue().get().getStringValue();
                    String oldPath = getSelectedFile();
                    if ((newPath != null) && !newPath.equals(oldPath)) {
                        ViewUtils.invokeLaterInEDT(() -> {
                            setSelectedFile(newPath);
                            fileLocationChanged();
                        });
                    }
                }
            }
        });

    }

    private String getOutputFileName() {
        // file chooser triggered by choose button
        FileSystemBrowser browser;
        if (getFileSystemBrowser() != null) {
            browser = getFileSystemBrowser(); // Instantiate file system browser based on given FileSystem/FileSystemConnection.
        } else if (NodeContext.getContext().getNodeContainer() != null) {
            //if an ordinary node context is available, use default file system browser
            browser = new LocalFileSystemBrowser();
        } else {
            browser = FileSystemBrowserExtension.collectFileSystemBrowsers().stream().filter(b -> b.isCompatible())
                .sorted((b1, b2) -> b1.getPriority() - b2.getPriority()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No applicable file system browser available"));
        }
        String selectedFile;
        if (m_allowSystemPropertySubstitution) {
            selectedFile = getSelectedFileWithPropertiesReplaced();
        } else {
            selectedFile = getSelectedFile();
        }
        return browser.openDialogAndGetSelectedFileName(m_selectMode, m_dialogType, this, m_forcedFileExtensionOnSave,
            selectedFile, m_suffixes);
    }

    /**
     * Set file file as part of the suffix.
     *
     * @param suffixes The new list of valid suffixes.
     */
    public void setSuffixes(final String... suffixes) {
        m_suffixes = suffixes;
    }

    /** @return the currently set list of file filter suffixes. */
    public String[] getSuffixes() {
        return m_suffixes;
    }

    /**
     * Get currently selected file.
     *
     * @return the current file url
     * @see javax.swing.JComboBox#getSelectedItem()
     */
    public String getSelectedFile() {
        return ((JTextField)m_textBox.getEditor().getEditorComponent()).getText();
    }

    /**
     * Internal getter to resolve possible system properties as per
     * {@link #setAllowSystemPropertySubstitution(boolean)}.
     */
    private String getSelectedFileWithPropertiesReplaced() {
        String selectedFile = getSelectedFile();
        if (m_allowSystemPropertySubstitution) {
            selectedFile = StrSubstitutor.replaceSystemProperties(selectedFile);
        }
        return selectedFile;
    }

    /**
     * Set the file url as default.
     *
     * @param url the file to choose
     * @see javax.swing.JComboBox#setSelectedItem(java.lang.Object)
     */
    public void setSelectedFile(final String url) {
        if (SwingUtilities.isEventDispatchThread()) {
            m_textBox.setSelectedItem(url);
        } else {
            ViewUtils.invokeAndWaitInEDT(new Runnable() {
                @Override
                public void run() {
                    m_textBox.setSelectedItem(url);
                }
            });
        }
    }

    /** Updates the elements in the combo box, reads the file history. */
    public void updateHistory() {
        StringHistory history = StringHistory.getInstance(m_historyID);
        String[] allVals = history.getHistory();
        LinkedHashSet<String> list = new LinkedHashSet<String>();
        for (int i = 0; i < allVals.length; i++) {
            String cur = allVals[i];
            if (!cur.isEmpty()) {
                try {
                    URL url = new URL(cur);
                    list.add(url.toString());
                } catch (MalformedURLException mue) {
                    // ignore, it's probably not a URL
                    list.add(new File(cur).getAbsolutePath());
                }
            }
        }
        m_textBox.setModel(new DefaultComboBoxModel<String>(list.toArray(new String[list.size()])));
        // changing the model will also change the minimum size to be
        // quite big. We have tooltips, we don't need that
        Dimension newMin = new Dimension(0, getPreferredSize().height);
        setMinimumSize(newMin);
    }

    /**
     * Adds a change listener that gets notified if a new file name is entered into the text field.
     *
     * @param cl a change listener
     */
    public void addChangeListener(final ChangeListener cl) {
        m_changeListener.add(cl);
    }

    /**
     * Removes the given change listener from the listener list.
     *
     * @param cl a change listener
     */
    public void removeChangeListener(final ChangeListener cl) {
        m_changeListener.remove(cl);
    }

    /**
     * Forces the given file extension when the user enters a path in the text field that does not end with either the
     * argument extension or any extension specified in {@link #setSuffixes(String...)} (ignoring case).
     *
     * @param forcedExtension optional parameter to force a file extension to be appended to the selected file name,
     *            e.g. ".txt" (null and blanks not force any extension).
     */
    public void setForceExtensionOnSave(final String forcedExtension) {
        if (m_dialogType != DialogType.SAVE_DIALOG) {
            throw new IllegalArgumentException("Cannot force extension on save for OPEN dialog");
        }
        m_forcedFileExtensionOnSave = StringUtils.defaultIfBlank(forcedExtension, null);
    }

    private void fileLocationChanged() {
        final ChangeEvent changeEvent = new ChangeEvent(this);
        m_changeListener.stream().forEach(c -> c.stateChanged(changeEvent));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        if (m_flowVariableButton != null) {
            boolean replacedByVariable = m_flowVariableButton.getFlowVariableModel().isVariableReplacementEnabled();

            m_chooseButton.setEnabled(enabled && !replacedByVariable);
            m_textBox.setEnabled(enabled && !replacedByVariable);
            m_flowVariableButton.setEnabled(enabled);
        } else {
            m_chooseButton.setEnabled(enabled);
            m_textBox.setEnabled(enabled);
        }
    }

    public void setBrowseable(final boolean enabled) {
        m_chooseButton.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        super.setToolTipText(text);
        m_textBox.setToolTipText(text);
        m_chooseButton.setToolTipText(text);
    }

    /**
     * If set to <code>true</code>, the path may contain system properties such as "${user.home}/file.txt". The
     * {@link #getSelectedFile()} method will return the path as entered by the user (incl. placeholders) so the caller
     * needs to resolve them also (see for instance {@link org.apache.commons.lang3.text.StrSubstitutor}).
     *
     * <p>
     * This method should be called right after construction.
     *
     * @param b Whether to allow system properties in the string (default is <code>false</code>)
     */
    public void setAllowSystemPropertySubstitution(final boolean b) {
        m_allowSystemPropertySubstitution = b;
        fileLocationChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        m_textBox.getEditor().getEditorComponent().requestFocus();
    }

    /**
     * Adds the current location to the history.
     */
    public void addToHistory() {
        StringHistory.getInstance(m_historyID).add(getSelectedFile());
    }

    /**
     * @return true if the value is replaced by a variable.
     */
    public boolean isVariableReplacementEnabled() {
        if (m_flowVariableButton == null) {
            return false;
        }
        return m_flowVariableButton.getFlowVariableModel().isVariableReplacementEnabled();
    }

    /**
     * @return the fileSystemBrowser
     */
    public FileSystemBrowser getFileSystemBrowser() {
        return m_fileSystemBrowser;
    }

    /**
     * @param fileSystemBrowser the fileSystemBrowser to set
     */
    public void setFileSystemBrowser(final FileSystemBrowser fileSystemBrowser) {
        m_fileSystemBrowser = fileSystemBrowser;
    }

}
