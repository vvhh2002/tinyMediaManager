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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.thirdparty.trakttv.MovieSyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;

/**
 * The class {@link MovieSyncTraktTvAction}. To synchronize your movie library/watched state with trakt.tv
 * 
 * @author Manuel Laggner
 */
public class MovieSyncTraktTvAction extends TmmAction {
  public MovieSyncTraktTvAction() {
    putValue(NAME, TmmResourceBundle.getString("movie.synctrakt"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.synctrakt.desc"));
    putValue(SMALL_ICON, IconManager.SYNC);
    putValue(LARGE_ICON_KEY, IconManager.SYNC);
  }

  @Override
  protected void processAction(ActionEvent e) {
    MovieSyncTraktTvTask task = new MovieSyncTraktTvTask(MovieModuleManager.getInstance().getMovieList().getMovies());
    task.setSyncCollection(true);
    task.setSyncWatched(true);
    task.setSyncRating(true);

    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
