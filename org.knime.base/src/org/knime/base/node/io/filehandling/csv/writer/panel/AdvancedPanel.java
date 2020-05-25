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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 9, 2007 (ohl): created
 */
package org.knime.base.node.io.filehandling.csv.writer.panel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.base.node.io.csvwriter.FileWriterSettings.quoteMode;
import org.knime.base.node.io.filehandling.csv.writer.config.AdvancedConfig;

/**
 * A dialog panel for advanced settings of CSV writer node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public final class AdvancedPanel extends JPanel {

    /** Auto generated serialVersionUID */
    private static final long serialVersionUID = -6233070493423721644L;

    private static final int TEXT_FIELD_WIDTH = 7;

    private JTextField m_missingValuePatternField;

    private JCheckBox m_compressWithGzipChecker;

    private final JRadioButton m_quoteIfNeededButton;

    private final JRadioButton m_quoteStringsButton;

    private final JRadioButton m_quoteAlwaysButton;

    private final JRadioButton m_quoteNeverButton;

    private JTextField m_separatorReplacementField;

    private JTextField m_decimalSeparatorField;

    private JCheckBox m_useScientificFormatChecker;

    private JCheckBox m_keepTrailingZeroChecker;

    /**
     * Default constructor - creates a populated JPanel
     */
    public AdvancedPanel() {
        super(new GridBagLayout());

        m_missingValuePatternField = new JTextField("", TEXT_FIELD_WIDTH);
        m_compressWithGzipChecker = new JCheckBox("Compress output file (gzip)");

        ButtonGroup bg = new ButtonGroup();
        m_quoteIfNeededButton = new JRadioButton("If needed");
        bg.add(m_quoteIfNeededButton);
        m_quoteStringsButton = new JRadioButton("Strings only");
        bg.add(m_quoteStringsButton);
        m_quoteAlwaysButton = new JRadioButton("Always");
        bg.add(m_quoteAlwaysButton);
        m_quoteNeverButton = new JRadioButton("Never");
        bg.add(m_quoteNeverButton);

        m_separatorReplacementField = new JTextField("", TEXT_FIELD_WIDTH);

        m_decimalSeparatorField = new JTextField(".", TEXT_FIELD_WIDTH);
        m_useScientificFormatChecker = new JCheckBox("Use scientific format for very large and very small values");
        m_keepTrailingZeroChecker = new JCheckBox("Keep trailing zeros from double values");

        initLayout();
    }

    /**
     * Helper method to create and initialize {@link GridBagConstraints}.
     *
     * @return initialized {@link GridBagConstraints}
     */
    private static final GridBagConstraints createAndInitGBC() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        return gbc;
    }

    private void initLayout() {
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        add(createAdvancedSubOptionsPanel(), gbc);
        gbc.gridy++;
        add(createAdvancedQuoteOptionsPanel(), gbc);
        gbc.gridy++;
        add(createAdvancedNumericOptionsPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        add(Box.createVerticalBox(), gbc);
    }

    private JPanel createAdvancedSubOptionsPanel() {
        final JPanel advancedOptionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        advancedOptionsPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Advanced: "));

        gbc.insets = new Insets(5, 5, 5, 5);
        advancedOptionsPanel.add(new JLabel("Missing values pattern"), gbc);
        gbc.gridx++;
        advancedOptionsPanel.add(m_missingValuePatternField, gbc);

        gbc.insets = new Insets(10, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        advancedOptionsPanel.add(m_compressWithGzipChecker, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        advancedOptionsPanel.add(Box.createHorizontalBox(), gbc);
        return advancedOptionsPanel;
    }

    private JPanel createAdvancedQuoteOptionsPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel advancedQuotePanel = new JPanel(new GridBagLayout());
        advancedQuotePanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Advanced quote settings: "));

        gbc.gridwidth = 5;
        gbc.insets = new Insets(5, 0, 3, 0);
        advancedQuotePanel.add(new JLabel("Enclose values inside quotes "), gbc);

        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.gridwidth = 1;
        gbc.gridy++;
        gbc.gridx++;
        advancedQuotePanel.add(m_quoteIfNeededButton, gbc);
        gbc.gridx++;
        advancedQuotePanel.add(m_quoteStringsButton, gbc);
        gbc.gridx++;
        advancedQuotePanel.add(m_quoteAlwaysButton, gbc);
        gbc.gridx++;
        advancedQuotePanel.add(m_quoteNeverButton, gbc);

        gbc.insets = new Insets(10, 0, 5, 0);
        gbc.gridwidth = 3;
        gbc.gridx = 0;
        gbc.gridy++;
        advancedQuotePanel.add(new JLabel("Column separator replacement"), gbc);
        gbc.gridwidth = 2;
        gbc.gridx = 3;
        gbc.insets = new Insets(10, 10, 5, 0);
        advancedQuotePanel.add(m_separatorReplacementField, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        advancedQuotePanel.add(Box.createHorizontalBox(), gbc);

        return advancedQuotePanel;
    }

    private JPanel createAdvancedNumericOptionsPanel() {
        final JPanel advancedNumOptionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        advancedNumOptionsPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Advanced numeric options: "));

        gbc.insets = new Insets(5, 5, 5, 5);
        advancedNumOptionsPanel.add(new JLabel("Decimal separator character "), gbc);
        gbc.gridx++;
        advancedNumOptionsPanel.add(m_decimalSeparatorField, gbc);

        gbc.insets = new Insets(10, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        advancedNumOptionsPanel.add(m_useScientificFormatChecker, gbc);

        gbc.gridy++;
        advancedNumOptionsPanel.add(m_keepTrailingZeroChecker, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        advancedNumOptionsPanel.add(Box.createHorizontalBox(), gbc);
        return advancedNumOptionsPanel;
    }

    /**
     * @return the missingValuePattern from the corresponding {@link JTextField}
     */
    public String getMissingValuePattern() {
        return m_missingValuePatternField.getText();
    }

    /**
     * @return {@code true} if compressWithGzipChecker is selected
     */
    public boolean compressWithGzip() {
        return m_compressWithGzipChecker.isSelected();
    }

    /**
     * @return the selected quoteMode from the radio buttons quoteIfNeededButton, quoteStringsButton, quoteAlwaysButton
     *         and quoteNeverButton
     */
    public quoteMode getQuoteMode() {
        if (m_quoteAlwaysButton.isSelected()) {
            return quoteMode.ALWAYS;
        } else if (m_quoteIfNeededButton.isSelected()) {
            return quoteMode.IF_NEEDED;
        } else if (m_quoteNeverButton.isSelected()) {
            return quoteMode.REPLACE;
        } else {
            return quoteMode.STRINGS;
        }
    }

    /**
     * @return the separatorReplacement from the corresponding {@link JTextField}
     */
    public String getSeparatorReplacement() {
        return m_separatorReplacementField.getText();
    }

    /**
     * @return the decimalSeparator from the corresponding {@link JTextField}
     */
    public char getDecimalSeparator() {
        //TODO check if single char is provided
        return m_decimalSeparatorField.getText().charAt(0);
    }

    /**
     * @return {@code true} if useScientificFormatChecker is selected
     */
    public boolean useScientificFormat() {
        return m_useScientificFormatChecker.isSelected();
    }

    /**
     * @return {@code true} if trailingZeroChecker is selected
     */
    public boolean keepTrailingZero() {
        return m_keepTrailingZeroChecker.isSelected();
    }

    /**
     * @param missingValuePattern to set in missingValuePatternField
     */
    public void setMissingValuePattern(final String missingValuePattern) {
        m_missingValuePatternField.setText(missingValuePattern);
    }

    /**
     * @param compressWithGzip whether or not to select compressWithGzipChecker
     */
    public void setCompressWithGzip(final boolean compressWithGzip) {
        m_compressWithGzipChecker.setSelected(compressWithGzip);
    }

    /**
     * @param separatorReplacement
     */
    public void setSeparatorReplacement(final String separatorReplacement) {
        m_separatorReplacementField.setText(separatorReplacement);
    }

    /**
     * @param decimalSeparator
     */
    public void setDecimalSeparator(final char decimalSeparator) {
        m_decimalSeparatorField.setText(String.valueOf(decimalSeparator));
    }

    /**
     * @param useScientificFormat whether or not to select scientificFormatChecker
     */
    public void setUseScientificFormat(final boolean useScientificFormat) {
        m_useScientificFormatChecker.setSelected(useScientificFormat);
    }

    /**
     * @param keepTrailingZero whether or not to select trailingZeroChecker
     */
    public void setKeepTrailingZero(final boolean keepTrailingZero) {
        m_keepTrailingZeroChecker.setSelected(keepTrailingZero);
    }

    /**
     *
     * @param mode the quoteMode to be selected
     */
    public void setQuoteMode(final quoteMode mode) {
        switch (mode) {
            case STRINGS:
                m_quoteStringsButton.setSelected(true);
                break;
            case ALWAYS:
                m_quoteAlwaysButton.setSelected(true);
                break;
            case IF_NEEDED:
                m_quoteIfNeededButton.setSelected(true);
                break;
            case REPLACE:
                m_quoteNeverButton.setSelected(true);
                break;
            default:
                m_quoteStringsButton.setSelected(true);
                break;
        }
    }

    /**
     * Loads dialog components with values from the provided configuration
     *
     * @param config the configuration to read values from
     */
    public void loadDialogSettings(final AdvancedConfig config) {
        setMissingValuePattern(config.getMissingValuePattern());
        setCompressWithGzip(config.compressWithGzip());

        setUseScientificFormat(config.useScientificFormat());
        setKeepTrailingZero(config.keepTrailingZero());
        setDecimalSeparator(config.getDecimalSeparator());

        setQuoteMode(config.getQuoteMode());
        setSeparatorReplacement(config.getSeparatorReplacement());
    }

    /**
     * Reads values from dialog and updates the provided configuration.
     *
     * @param config the configuration to read values from
     */
    public void saveDialogSettings(final AdvancedConfig config) {
        config.setMissingValuePattern(getMissingValuePattern());
        config.setCompressWithGzip(compressWithGzip());

        config.setUseScientificFormat(useScientificFormat());
        config.setKeepTrailingZero(keepTrailingZero());
        config.setDecimalSeparator(getDecimalSeparator());

        config.setQuoteMode(getQuoteMode());
        config.setSeparatorReplacement(getSeparatorReplacement());
    }
}
