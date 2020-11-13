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
 *   Aug 14, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BoundedRangeModel;
import javax.swing.ButtonModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.util.SharedIcons;

import com.google.common.base.Objects;

/**
 * Model of an AnalysisComponent.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class AnalysisComponentModel {

    private static final String EMPTY_SPACE_RESERVING_LABEL_TEXT = " ";

    private final CopyOnWriteArrayList<ChangeListener> m_listeners = new CopyOnWriteArrayList<>();

    private final ChangeEvent m_changeEvent = new ChangeEvent(this);

    private final ButtonModel m_quickScan = new DefaultButtonModel();

    private final BoundedRangeModel m_progress = new DefaultBoundedRangeModel();

    private boolean m_progressIndeterminate = false;

    private boolean m_analysisComponentsVisible = false;

    private String m_analysisProgressText = EMPTY_SPACE_RESERVING_LABEL_TEXT;

    private String m_currentPath = EMPTY_SPACE_RESERVING_LABEL_TEXT;

    private String m_errorText = EMPTY_SPACE_RESERVING_LABEL_TEXT;

    private Icon m_analysisProgressIcon = null;

    private Icon m_errorIcon = null;

    void addChangeListener(final ChangeListener listener) {
        if (!m_listeners.contains(listener)) {//NOSONAR
            m_listeners.add(listener);//NOSONAR a small price to pay for thread-safety
        }
    }

    private void notifyListeners() {
        for (ChangeListener listener : m_listeners) {
            listener.stateChanged(m_changeEvent);
        }
    }

    void setProgressIndeterminate(final boolean indeterminate) {
        if (setProgressIndeterminateInternal(indeterminate)) {
            notifyListeners();
        }
    }

    boolean setProgressIndeterminateInternal(final boolean indeterminate) {
        if (m_progressIndeterminate != indeterminate) {
            m_progressIndeterminate = indeterminate;
            return true;
        }
        return false;
    }

    void setProgress(final int progress) {
        m_progress.setValue(progress);
    }

    void setProgressLabel(final String text) {
        if (setProgressLabelInternal(text)) {
            notifyListeners();
        }
    }

    private boolean setProgressLabelInternal(final String text) {
        if (!Objects.equal(m_analysisProgressText, text)) {
            m_analysisProgressText = text;
            return true;
        }
        return false;
    }

    void setProgressLabel(final Icon icon, final String text) {
        if (setProgressLabelInternal(icon, text)) {
            notifyListeners();
        }
    }

    private boolean setProgressLabelInternal(final Icon icon, final String text) {
        if (!Objects.equal(m_analysisProgressIcon, icon) || !Objects.equal(m_analysisProgressText, text)) {
            m_analysisProgressText = text;
            m_analysisProgressIcon = icon;
            return true;
        }
        return false;
    }

    void resetAnalysisComponents() {
        resetAnalysisComponentsInternal();
        notifyListeners();
    }

    private void resetAnalysisComponentsInternal() {
        m_progress.setValue(0);
        m_analysisProgressIcon = null;
        m_analysisProgressText = null;
        m_quickScan.setEnabled(true);
    }

    void resetErrorLabel() {
        resetErrorLabelInternal();
        notifyListeners();
    }

    private void resetErrorLabelInternal() {
        m_errorIcon = null;
        m_errorText = EMPTY_SPACE_RESERVING_LABEL_TEXT;
    }

    void setError(final long row, final String text) {
        // only set error if not another one is already set
        if (m_errorText.equals(EMPTY_SPACE_RESERVING_LABEL_TEXT)) {
            m_errorIcon = SharedIcons.ERROR.get();
            m_errorText = (row < 0 ? "" : ("Row " + row + ": "))
                + (text == null || text.isEmpty() ? EMPTY_SPACE_RESERVING_LABEL_TEXT : text);
            notifyListeners();
        }
    }

    void setError(final String text) {
        setError(-1, text);
    }

    void setInfo(final String text) {
        m_errorIcon = SharedIcons.INFO_BALLOON.get();
        m_errorText = text;
        notifyListeners();
    }

    void reset() {
        resetAnalysisComponentsInternal();
        resetErrorLabelInternal();
        notifyListeners();
    }

    void setVisible(final boolean visible) {
        if (setVisibleInternal(visible)) {
            m_progress.setValue(0);
            notifyListeners();
        }
    }

    private boolean setVisibleInternal(final boolean visible) {
        if (m_analysisComponentsVisible != visible) {
            m_analysisComponentsVisible = visible;
            return true;
        }
        return false;
    }

    void setProgressPathLabel(final String text) {
        if (setProgressPathLabelInternal(text)) {
            notifyListeners();
        }
    }

    private boolean setProgressPathLabelInternal(final String text) {
        if (!Objects.equal(m_currentPath, text)) {
            m_currentPath = text;
            return true;
        }
        return false;
    }

    boolean hasNoError() {
        return m_errorText.equals(EMPTY_SPACE_RESERVING_LABEL_TEXT);
    }

    void update(final AnalysisUpdate update) {
        boolean notifyListeners = false;
        notifyListeners |= setProgressLabelInternal(update.getProgressLabel());
        notifyListeners |= setProgressPathLabelInternal(update.getProgressPathLabel());
        if (update.isSizeAssessable()) {
            notifyListeners |= setProgressIndeterminateInternal(false);
            setProgress(update.getProgress());
        } else {
            notifyListeners |= setProgressIndeterminateInternal(true);
        }
        if (notifyListeners) {
            notifyListeners();
        }
    }

    /**
     * @return the quickScan
     */
    ButtonModel getQuickScanModel() {
        return m_quickScan;
    }

    /**
     * @return the progress
     */
    BoundedRangeModel getProgress() {
        return m_progress;
    }

    /**
     * @return the progressIndeterminate
     */
    boolean isProgressIndeterminate() {
        return m_progressIndeterminate;
    }

    /**
     * @return the analysisComponentsVisible
     */
    boolean areAnalysisComponentsVisible() {
        return m_analysisComponentsVisible;
    }

    /**
     * @return the analysisProgressText
     */
    String getAnalysisProgressText() {
        return m_analysisProgressText;
    }

    /**
     * @return the currentPath
     */
    String getCurrentPath() {
        return m_currentPath;
    }

    /**
     * @return the errorText
     */
    String getErrorText() {
        return m_errorText;
    }

    /**
     * @return the analysisProgressIcon
     */
    Icon getAnalysisProgressIcon() {
        return m_analysisProgressIcon;
    }

    /**
     * @return the errorIcon
     */
    Icon getErrorIcon() {
        return m_errorIcon;
    }

}
