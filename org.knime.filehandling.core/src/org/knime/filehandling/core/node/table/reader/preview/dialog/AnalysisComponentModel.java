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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

    private boolean m_progressBarVisible = false;

    private boolean m_quickScanButtonVisible = false;

    private String m_analysisProgressText = EMPTY_SPACE_RESERVING_LABEL_TEXT;

    private String m_currentPath = EMPTY_SPACE_RESERVING_LABEL_TEXT;

    private String m_errorText = EMPTY_SPACE_RESERVING_LABEL_TEXT;

    private MessageType m_analysisProgressMessageType = MessageType.NONE;

    private MessageType m_errorMessageType = MessageType.NONE;

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

    private void setProgress(final int progress) {
        m_progress.setValue(progress);
    }

    private boolean setProgressLabelInternal(final String text) {
        if (!Objects.equal(m_analysisProgressText, text)) {
            m_analysisProgressText = text;
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
        m_analysisProgressMessageType = MessageType.NONE;
        m_analysisProgressText = EMPTY_SPACE_RESERVING_LABEL_TEXT;
        m_quickScan.setEnabled(true);
    }

    /**
     * Resets the error label.
     */
    public void resetErrorLabel() {
        startUpdate()//
            .clearErrorMessage()//
            .finishUpdate();
    }

    /**
     * Sets an info text to be displayed.
     *
     * @param text the info text
     */
    public void setInfo(final String text) {
        m_errorMessageType = MessageType.INFO;
        m_errorText = text;
        notifyListeners();
    }

    void setProgressLabel(final MessageType messageType, final String message) {
        startUpdate()//
            .setAnalysisProgressMessage(messageType, message)//
            .finishUpdate();
    }

    void clearProgressLabel() {
        startUpdate()//
            .clearAnalysisProgressMessage()//
            .finishUpdate();
    }

    void setErrorLabel(final MessageType messageType, final String message) {
        startUpdate()//
            .setErrorMessage(messageType, message)//
            .finishUpdate();
    }

    void setErrorLabel(final String message, final long row) {
        startUpdate()//
            .setErrorMessage(MessageType.ERROR, message, row)//
            .finishUpdate();
    }

    /**
     * Sets the provided error message and notifies the listeners.<br>
     * Convenience short cut for {@code startUpdate().setErrorMessage(MessageType.ERROR, message).finishUpdate()}.
     *
     *
     * @param message error message to set
     */
    public void setErrorLabel(final String message) {
        startUpdate()//
        .setErrorMessage(MessageType.ERROR, message)//
        .finishUpdate();
    }

    /**
     * Reset the model.
     */
    public void reset() {
        startUpdate()//
            .reset()//
            .finishUpdate();
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
    boolean showProgressBar() {
        return m_progressBarVisible;
    }

    boolean showQuickScanButton() {
        return m_quickScanButtonVisible;
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
    MessageType getAnalysisProgressMessageType() {
        return m_analysisProgressMessageType;
    }

    /**
     * @return the errorIcon
     */
    MessageType getErrorMessageType() {
        return m_errorMessageType;
    }

    /**
     * Starts an update for this model.<br>
     * An update allows to modify multiple parameters at once.
     * The listeners are notified once the update is finished via the {@link Update#finishUpdate()} method.
     *
     * @return a fresh Update instance
     */
    public Update startUpdate() {
        return new Update(this);
    }

    private void finishUpdate(final Update update) {//NOSONAR, moving it inside Update would defeat its purpose
        m_progressIndeterminate = update.m_progressIndeterminate;
        m_progressBarVisible = update.m_showProgressBar;
        m_quickScanButtonVisible = update.m_showQuickScanButton;
        m_errorText = update.m_errorText;
        m_errorMessageType = update.m_errorMessageType;
        m_analysisProgressMessageType = update.m_analysisProgressMessageType;
        m_analysisProgressText = update.m_analysisProgressText;
        notifyListeners();
    }

    enum MessageType {
            INFO, ERROR, WARNING, SUCCESS, NONE;

    }

    /**
     * Represents an update of the AnalysisComponentModel.<br>
     * Allows for fluid updates via method chaining.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    public static final class Update {

        private final AnalysisComponentModel m_model;

        private boolean m_progressIndeterminate;

        private boolean m_showProgressBar;

        private boolean m_showQuickScanButton;

        private String m_analysisProgressText;

        private MessageType m_analysisProgressMessageType;

        private String m_errorText;

        private MessageType m_errorMessageType;

        private boolean m_somethingChanged = false;

        private int m_progress;

        private Update(final AnalysisComponentModel model) {
            m_model = model;
            m_progressIndeterminate = model.m_progressIndeterminate;
            m_showProgressBar = model.m_progressBarVisible;
            m_showQuickScanButton = model.m_quickScanButtonVisible;
            m_analysisProgressText = model.m_analysisProgressText;
            m_analysisProgressMessageType = model.m_analysisProgressMessageType;
            m_errorText = model.m_errorText;
            m_errorMessageType = model.m_errorMessageType;
            m_progress = model.m_progress.getValue();
        }

        /**
         * Finishes the update and notifies the listeners if anything changed.
         */
        public void finishUpdate() {
            if (m_somethingChanged) {
                m_model.finishUpdate(this);
            }
        }

        final Update reset() {
            setProgressValue(0);
            clearAnalysisProgressMessage();
            clearErrorMessage();
            return this;
        }

        final Update setAnalysisProgressMessage(final MessageType progressType, final String info) {
            m_somethingChanged |=
                (m_analysisProgressMessageType != progressType || !Objects.equal(m_analysisProgressText, info));
            m_analysisProgressText = info;
            m_analysisProgressMessageType = progressType;
            return this;
        }

        final Update clearAnalysisProgressMessage() {
            return setAnalysisProgressMessage(MessageType.NONE, EMPTY_SPACE_RESERVING_LABEL_TEXT);
        }

        final Update setProgressValue(final int progress) {
            m_somethingChanged |= (m_progress != progress);
            m_progress = progress;
            return this;
        }

        final Update setProgressIndeterminate(final boolean progressIndeterminate) {
            m_somethingChanged |= (m_progressIndeterminate != progressIndeterminate);
            m_progressIndeterminate = progressIndeterminate;
            return this;
        }

        /**
         * Sets whether or not to display the progress bar.
         *
         * @param showProgressBar {@code true} if the progress bar should be displayed
         * @return this Update
         */
        public final Update showProgressBar(final boolean showProgressBar) {
            m_somethingChanged |= (m_showProgressBar != showProgressBar);
            m_showProgressBar = showProgressBar;
            return this;
        }

        /**
         * Sets whether or not to show the "Quick Scan" button.
         *
         * @param showQuickScanButton {@code true} if the "Quick Scan" button should be displayed
         * @return this Update
         */
        public final Update showQuickScanButton(final boolean showQuickScanButton) {
            m_somethingChanged |= (m_showQuickScanButton != showQuickScanButton);
            m_showQuickScanButton = showQuickScanButton;
            return this;
        }

        final Update setAnalysisProgressText(final String analysisProgressText) {
            m_somethingChanged |= !Objects.equal(m_analysisProgressText, analysisProgressText);
            m_analysisProgressText = analysisProgressText;
            return this;
        }

        final Update setErrorMessage(final MessageType messageType, final String message, final long row) {
            final String messageWithRowInfo = combineErrorMessage(message, row);
            m_somethingChanged |=
                (m_errorMessageType != messageType || !Objects.equal(m_errorText, messageWithRowInfo));
            m_errorText = messageWithRowInfo;
            m_errorMessageType = messageType;
            return this;
        }

        private static String combineErrorMessage(final String message, final long row) {
            return (row < 0 ? "" : ("Row " + row + ": "))
                + (message == null || message.isEmpty() ? EMPTY_SPACE_RESERVING_LABEL_TEXT : message);
        }

        final Update setErrorMessage(final MessageType messageType, final String message) {
            return setErrorMessage(messageType, message, -1);
        }

        final Update clearErrorMessage() {
            return setErrorMessage(MessageType.NONE, EMPTY_SPACE_RESERVING_LABEL_TEXT, -1);
        }

    }

}
