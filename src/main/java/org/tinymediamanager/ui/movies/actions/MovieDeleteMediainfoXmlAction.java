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

package org.tinymediamanager.ui.movies.actions;

import static org.tinymediamanager.ui.TmmFontHelper.L1;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * the class {@link MovieDeleteMediainfoXmlAction} is used to delete mediainfo.xml for selected movies
 *
 * @author Manuel Laggner
 */
public class MovieDeleteMediainfoXmlAction extends TmmAction {

  private static final long           serialVersionUID = -2019243514238173721L;
  

  public MovieDeleteMediainfoXmlAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.deletemediainfoxml"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.deletemediainfoxml"));
    putValue(SMALL_ICON, IconManager.DELETE);
    putValue(LARGE_ICON_KEY, IconManager.DELETE);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // display warning and ask the user again
    if (!TmmProperties.getInstance().getPropertyAsBoolean("movie.hidedeletemediainfoxmlhint")) {
      JCheckBox checkBox = new JCheckBox(TmmResourceBundle.getString("tmm.donotshowagain"));
      TmmFontHelper.changeFont(checkBox, L1);
      checkBox.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

      Object[] options = { TmmResourceBundle.getString("Button.yes"), TmmResourceBundle.getString("Button.no") };
      Object[] params = { TmmResourceBundle.getString("movie.deletemediainfoxml.desc"), checkBox };
      int answer = JOptionPane.showOptionDialog(MainWindow.getInstance(), params, TmmResourceBundle.getString("movie.deletemediainfoxml"),
          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);

      // the user don't want to show this dialog again
      if (checkBox.isSelected()) {
        TmmProperties.getInstance().putProperty("movie.hidedeletemediainfoxmlhint", String.valueOf(checkBox.isSelected()));
      }

      if (answer != JOptionPane.YES_OPTION) {
        return;
      }
    }

    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    for (Movie movie : selectedMovies) {
      movie.getMediaFiles(MediaFileType.MEDIAINFO).forEach(mediaFile -> {
        Utils.deleteFileSafely(mediaFile.getFileAsPath());
        movie.removeFromMediaFiles(mediaFile);
      });
    }
    MainWindow.getInstance().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
  }
}
