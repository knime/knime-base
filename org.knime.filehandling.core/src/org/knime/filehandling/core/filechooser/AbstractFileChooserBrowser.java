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
 *   April 16, 2020 (Adrian Nembach): created
 */
package org.knime.filehandling.core.filechooser;

import static java.util.stream.Collectors.toList;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.util.AbstractJFileChooserBrowser;
import org.knime.core.node.util.FileSystemBrowser;
import org.knime.core.util.SimpleFileFilter;

/**
 * An abstract file system browser that uses the {@link JFileChooser} but allows sub-classes to provide, e.g., custom
 * implementations of the JFileChooser's {@link FileSystemView}. Adapted from {@link AbstractJFileChooserBrowser} to
 * satisfy the requirements for the new file-handling while keeping existing code that depends on
 * AbstractJFileChooserBrowser unaffected.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractFileChooserBrowser implements FileSystemBrowser {

    @Override
    public String openDialogAndGetSelectedFileName(final FileSelectionMode fileSelectionMode,
        final DialogType dialogType, final Component parent, final String forcedFileExtensionOnSave,
        final String selectedFile, final String[] suffixes) {
        final JFileChooser fileChooser = prepareFileChooser(fileSelectionMode, dialogType, selectedFile, suffixes);

        int r = showDialog(dialogType, parent, fileChooser);

        if (r == JFileChooser.APPROVE_OPTION) {
            return processUserSelection(fileSelectionMode, dialogType, parent, forcedFileExtensionOnSave, suffixes,
                fileChooser);
        } else {
            return null;
        }
    }

    private String processUserSelection(final FileSelectionMode fileSelectionMode, final DialogType dialogType,
        final Component parent, final String forcedFileExtensionOnSave, final String[] suffixes,
        final JFileChooser fileChooser) {
        File file = fileChooser.getSelectedFile();
        // appending a file extension only makes sense if the selected file is not a directory
        if (dialogType == DialogType.SAVE_DIALOG && fileSelectionMode == FileSelectionMode.FILES_ONLY) {
            file = appendExtensionIfNecessary(forcedFileExtensionOnSave, suffixes, fileChooser.getFileFilter(), file);
        }
        if (fileSelectionMode == FileSelectionMode.FILES_ONLY && file.isDirectory()) {
            JOptionPane.showMessageDialog(parent, "Error: Please select a file, not a directory.");
            return null;
        }
        return postprocessSelectedFilePath(file.getAbsolutePath());
    }

    private JFileChooser prepareFileChooser(final FileSelectionMode fileSelectionMode, final DialogType dialogType,
        final String selectedFile, final String[] suffixes) {
        final JFileChooser fileChooser = new JFileChooser(getFileSystemView());
        setFileView(fileChooser);
        setupFilters(dialogType, suffixes, fileChooser);
        fileChooser.setFileSelectionMode(fileSelectionMode.getJFileChooserCode());
        fileChooser.setDialogType(dialogType.getJFileChooserCode());

        // AP-2562
        // It seems only resized event is happening when showing the dialog
        // Grabbing the focus then makes two clicks to single click selection.
        fileChooser.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                fileChooser.grabFocus();
            }
        });

        setupSelectedFile(selectedFile, fileChooser);
        return fileChooser;
    }

    private File appendExtensionIfNecessary(final String forcedFileExtensionOnSave, final String[] suffixes,
        final FileFilter filter, File file) {
        String forceFileExtension;
        if (filter instanceof SimpleFileFilter) {
            final String[] currentFilter = ((SimpleFileFilter)filter).getValidExtensions();
            if (currentFilter.length == 1) {
                forceFileExtension = currentFilter[0];
            } else {
                forceFileExtension = StringUtils.defaultString(forcedFileExtensionOnSave);
            }
        } else {
            forceFileExtension = StringUtils.defaultString(forcedFileExtensionOnSave);
        }
        final String fileName = file.getName();
        if (!(StringUtils.endsWithAny(fileName, suffixes)
            || StringUtils.endsWithIgnoreCase(fileName, forcedFileExtensionOnSave))) {
            file = addFileExtension(file, forceFileExtension);
        }
        return file;
    }

    private static int showDialog(final DialogType dialogType, final Component parent, final JFileChooser fileChooser) {
        /* This if construct is result of a fix for bug 5841.
        * showDialog does not resolve localized folder names correctly under Mac OS,
        * so we use the methods showSaveDialog and showOpenDialog if possible.
        */
        if (dialogType == DialogType.SAVE_DIALOG) {
            return fileChooser.showSaveDialog(parent);
        } else if (dialogType == DialogType.OPEN_DIALOG) {
            return fileChooser.showOpenDialog(parent);
        } else {
            return fileChooser.showDialog(parent, "OK");
        }
    }

    private void setupSelectedFile(final String selectedFile, final JFileChooser fileChooser) {
        final File selected = createFileFromPath(selectedFile);
        if (selected == null) {
            fileChooser.setSelectedFile(null);
        } else {
            if (selected.isDirectory()) {
                fileChooser.setSelectedFile(null);
                fileChooser.setCurrentDirectory(selected.getAbsoluteFile());
            } else {
                fileChooser.setSelectedFile(selected.getAbsoluteFile());
            }
        }
    }

    private static void setupFilters(final DialogType dialogType, final String[] suffixes,
        final JFileChooser fileChooser) {
        fileChooser.setAcceptAllFileFilterUsed(true);
        if (suffixes == null || suffixes.length == 0) {
            return;
        }
        final List<SimpleFileFilter> filters = createFiltersFromSuffixes(suffixes);
        for (final SimpleFileFilter filter : filters) {
            fileChooser.addChoosableFileFilter(filter);
        }
        if (!filters.isEmpty()) {
            fileChooser.setFileFilter(filters.get(0));
            if (dialogType == DialogType.SAVE_DIALOG) {
                // no need for the include all "*" filter if we know which extensions should be supported
                fileChooser.setAcceptAllFileFilterUsed(false);
            }
        }
    }

    /**
     * Creates a new file with the given fileExtension concatenated to the files file name.
     *
     * Protected method offered as a hook for concrete implementations to add the extension in their own implementation
     * specific way.
     *
     * This method should not be called on files that already contain a file extension in their name!
     *
     * @param file file to which the extension should be added
     * @param fileExtension the file extension
     * @return a new file with the extension added to the file name
     */
    protected File addFileExtension(final File file, final String fileExtension) {
        return new File(file.getParentFile(), file.getName().concat(fileExtension));
    }

    private static List<SimpleFileFilter> createFiltersFromSuffixes(final String... extensions) {

        // collect individual extensions
        final List<String> splitSuffixes = Arrays.stream(extensions)//
            .flatMap(AbstractFileChooserBrowser::flattenSuffix)//
            .collect(toList());
        if (splitSuffixes.size() > 1) {
            // create filter that accepts all extensions (provided there is more than 1)
            final SimpleFileFilter combined = new SimpleFileFilter(splitSuffixes.toArray(new String[0])) {
                @Override
                public String getDescription() {
                    return "All Formats";
                }
            };
            // return a list of the combined filter as well as all individual filters
            return Stream.concat(Stream.of(combined), splitSuffixes.stream()//
                .map(SimpleFileFilter::new))//
                .collect(toList());
        } else {
            return splitSuffixes.stream()//
                .map(SimpleFileFilter::new)//
                .collect(toList());
        }
    }

    private static Stream<String> flattenSuffix(final String extension) {
        if (extension.contains("|")) {
            final String[] splitExtensions = extension.split("\\|");
            return Arrays.stream(splitExtensions);
        } else {
            return Stream.of(extension);
        }
    }

    /**
     * @return <code>null</code> if the default should be selected
     */
    protected abstract FileSystemView getFileSystemView();

    /**
     * @return <code>null</code> if the default should be used
     */
    protected abstract FileView getFileView();

    /**
     * Turns a path into a file.
     *
     * @param filePath
     * @return a file representing the given path
     */
    protected abstract File createFileFromPath(String filePath);

    /**
     * Allows one to modify the selected file path as returned from the file chooser.
     *
     * @param selectedFile the file path as output by the file chooser
     * @return a potentially modified file path/url
     */
    protected String postprocessSelectedFilePath(final String selectedFile) {
        return selectedFile;
    }

    private void setFileView(final JFileChooser jfc) {
        final FileView fileView = getFileView();
        if (fileView != null) {
            jfc.setFileView(fileView);
        }
    }
}
