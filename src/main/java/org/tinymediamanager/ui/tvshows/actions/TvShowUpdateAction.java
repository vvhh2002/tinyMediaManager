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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.tasks.TvShowUpdateDatasourceTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;

/**
 * The class TvShowUpdateAction. Update a single TV show rather than the whole data source
 * 
 * @author Manuel Laggner
 */
public class TvShowUpdateAction extends TmmAction {
  private static final long serialVersionUID = 7216738427209633666L;

  public TvShowUpdateAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.update"));
    putValue(LARGE_ICON_KEY, IconManager.REFRESH);
    putValue(SMALL_ICON, IconManager.REFRESH);
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShowsRecursive();
    List<Path> tvShowFolders = new ArrayList<>();

    if (selectedTvShows.isEmpty()) {
      return;
    }

    for (TvShow tvShow : selectedTvShows) {
      tvShowFolders.add(tvShow.getPathNIO());
    }

    TmmThreadPool task = new TvShowUpdateDatasourceTask(tvShowFolders);
    TmmTaskManager.getInstance().addMainTask(task);
  }
}
