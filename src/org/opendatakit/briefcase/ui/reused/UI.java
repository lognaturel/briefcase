/*
 * Copyright (C) 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.briefcase.ui.reused;

import static java.awt.Color.GRAY;
import static java.awt.Cursor.HAND_CURSOR;
import static java.awt.Cursor.getPredefinedCursor;
import static java.awt.Desktop.getDesktop;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.SwingUtilities.invokeLater;
import static org.opendatakit.briefcase.ui.MainBriefcaseWindow.APP_NAME;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.opendatakit.briefcase.reused.BriefcaseException;
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.http.Credentials;

public class UI {

  /**
   * Pops up an informative dialog
   * <ul>
   * <li>uses {@link org.opendatakit.briefcase.ui.MainBriefcaseWindow#APP_NAME} as the title</li>
   * </ul>
   */
  public static void infoMessage(String message) {
    infoMessage(APP_NAME, message);
  }

  /**
   * Pops up an informative dialog
   */
  public static void infoMessage(String title, String message) {
    Runnable dialog = () -> JOptionPane.showMessageDialog(buildDialogParent(), message, title, PLAIN_MESSAGE);
    if (SwingUtilities.isEventDispatchThread())
      dialog.run();
    else
      SwingUtilities.invokeLater(dialog);
  }

  /**
   * Pops up a confirmation (YES/NO) dialog
   * <ul>
   * <li>uses {@link org.opendatakit.briefcase.ui.MainBriefcaseWindow#APP_NAME} as the title</li>
   * </ul>
   */
  public static boolean confirm(String message) {
    return confirm(APP_NAME, message);
  }

  /**
   * Pops up a confirmation (YES/NO) dialog
   */
  public static boolean confirm(String title, String message) {
    return JOptionPane.showConfirmDialog(buildDialogParent(), message, title, YES_NO_OPTION, PLAIN_MESSAGE) == YES_OPTION;
  }

  /**
   * Pops up an error dialog
   */
  public static void errorMessage(String title, String message) {
    Runnable dialog = () -> JOptionPane.showMessageDialog(buildDialogParent(), buildScrollPane(message), title, ERROR_MESSAGE);
    if (SwingUtilities.isEventDispatchThread())
      dialog.run();
    else
      SwingUtilities.invokeLater(dialog);
  }

  private static JDialog buildDialogParent() {
    JDialog dialog = new JDialog();
    // We want all dialogs show on top
    dialog.setAlwaysOnTop(true);
    return dialog;
  }

  private static JScrollPane buildScrollPane(String message) {
    // create a n-character wide label for aiding layout calculations...
    // the dialog box will display this width of text.
    JLabel probe = new JLabel("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM");

    JTextArea textArea = new JTextArea(message);
    textArea.setEditable(false);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    // Take font and colors from the probe
    textArea.setFont(probe.getFont());
    textArea.setBackground(probe.getBackground());
    textArea.setForeground(probe.getForeground());

    JScrollPane scrollPane = new JScrollPane(textArea);

    // don't show the gray border of the scroll pane
    // unless we are showing the scroll bar, in which case we do show it.
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.getVerticalScrollBar().addComponentListener(new ComponentAdapter() {
      @Override
      public void componentHidden(ComponentEvent component) {
        if (component.getComponent().equals(scrollPane.getVerticalScrollBar())) {
          scrollPane.setBorder(BorderFactory.createEmptyBorder());
        }
      }

      @Override
      public void componentShown(ComponentEvent component) {
        if (component.getComponent().equals(scrollPane.getVerticalScrollBar())) {
          scrollPane.setBorder(BorderFactory.createLineBorder(GRAY));
        }
      }
    });

    Dimension dimension = probe.getPreferredSize();
    dimension.setSize(dimension.getWidth(), 5.3 * dimension.getHeight());
    scrollPane.setMinimumSize(dimension);
    scrollPane.setPreferredSize(dimension);
    return scrollPane;
  }

  public static void removeAllMouseListeners(JComponent component) {
    for (MouseListener listener : component.getMouseListeners())
      component.removeMouseListener(listener);
  }

  public static void makeClickable(JLabel label, Runnable callback) {
    label.setForeground(Color.BLUE);
    label.setCursor(getPredefinedCursor(HAND_CURSOR));
    removeAllMouseListeners(label);
    label.addMouseListener(new MouseAdapterBuilder().onClick(__ -> invokeLater(callback)).build());
  }

  /**
   * Opens a tab in the default desktop web browser with the provided URL
   */
  public static void uncheckedBrowse(URL url) {
    try {
      getDesktop().browse(url.toURI());
    } catch (URISyntaxException | IOException e) {
      throw new BriefcaseException(e);
    }
  }

  public static Optional<Credentials> credentialsFromFields(JTextField username, JPasswordField password) {
    return OptionalProduct.all(
        Optional.ofNullable(username.getText()).map(String::trim).filter(s -> !s.isEmpty()),
        Optional.of(new String(password.getPassword())).filter(s -> !s.isEmpty())
    ).map(Credentials::new);
  }

  /**
   * Sort Comparator for columns providing booleans
   *
   * @param o1 The first object to be compared
   * @param o2 The second object to be compared
   * @return int for sorting of two elements (0(equals) , -1(less than), 1(greater than)
   */
  public static int compareSelectionButton(Boolean o1, Boolean o2) {
    if (o1.equals(o2))
      return 0;
    if (o1.equals(Boolean.TRUE) && o2.equals(Boolean.FALSE))
      return -1;
    return 1;
  }
}
