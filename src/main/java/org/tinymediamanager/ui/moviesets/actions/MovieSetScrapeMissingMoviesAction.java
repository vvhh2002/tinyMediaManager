/*
 * Copyright 2012 - 2021 Manuel Laggner
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieSetArtworkHelper;
import org.tinymediamanager.core.movie.MovieSetSearchAndScrapeOptions;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.movie.tasks.MovieSetScrapeTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;

/**
 * The class {@link MovieSetScrapeMissingMoviesAction}. To update the information of missing movies
 * 
 * @author Manuel Laggner
 */
public class MovieSetScrapeMissingMoviesAction extends TmmAction {
  private static final long           serialVersionUID = -389165862194237592L;
  

  public MovieSetScrapeMissingMoviesAction() {
    putValue(NAME, TmmResourceBundle.getString("movieset.scrape.missingmovies"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movieset.scrape.missingmovies.desc"));

    putValue(LARGE_ICON_KEY, IconManager.SEARCH);
    putValue(SMALL_ICON, IconManager.SEARCH);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<MovieSet> selectedMovieSets = new ArrayList<>(MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovieSets());

    if (selectedMovieSets.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    for (MovieSet movieSet : selectedMovieSets) {
      MovieSetArtworkHelper.cleanupArtwork(movieSet);
    }

    MovieSetSearchAndScrapeOptions options = new MovieSetSearchAndScrapeOptions();
    options.loadDefaults();

    TmmThreadPool scrapeTask = new MovieSetScrapeTask(selectedMovieSets, options, new ArrayList<>());
    TmmTaskManager.getInstance().addMainTask(scrapeTask);
  }
}
