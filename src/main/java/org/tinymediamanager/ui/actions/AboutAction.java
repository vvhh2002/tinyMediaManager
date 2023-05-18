/*
 * Copyright 2012 - 2023 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.ui.actions;

import java.awt.Dialog;
import java.awt.event.ActionEvent;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.dialogs.AboutDialog;

/**
 * The AboutAction to display the aboutbox
 * 
 * @author Manuel Laggner
 */
public class AboutAction extends TmmAction {
  private static final long serialVersionUID = -6578562721885387890L;

  public AboutAction() {
    putValue(NAME, TmmResourceBundle.getString("tmm.about"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    Dialog aboutDialog = new AboutDialog();
    aboutDialog.setLocationRelativeTo(MainWindow.getInstance());
    aboutDialog.setVisible(true);
  }
}
