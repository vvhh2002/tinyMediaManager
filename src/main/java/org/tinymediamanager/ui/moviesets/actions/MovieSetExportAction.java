/*
 * Copyright 2012 - 2022 Manuel Laggner
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
package org.tinymediamanager.ui.moviesets.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;
import org.tinymediamanager.ui.moviesets.dialogs.MovieSetExporterDialog;

/**
 * The {@link MovieSetExportAction} - to export all selected movie sets via a template
 * 
 * @author Manuel Laggner
 */
public class MovieSetExportAction extends TmmAction {
  public MovieSetExportAction() {
    putValue(LARGE_ICON_KEY, IconManager.EXPORT);
    putValue(SMALL_ICON, IconManager.EXPORT);
    putValue(NAME, TmmResourceBundle.getString("movieset.export"));
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_DOWN_MASK));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<MovieSet> movieSets = MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovieSets();

    if (movieSets.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // export selected movies
    MovieSetExporterDialog dialog = new MovieSetExporterDialog(movieSets);
    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.setVisible(true);
  }
}
