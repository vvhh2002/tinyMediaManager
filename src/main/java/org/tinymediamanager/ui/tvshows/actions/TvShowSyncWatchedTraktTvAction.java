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
package org.tinymediamanager.ui.tvshows.actions;

import java.awt.event.ActionEvent;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.thirdparty.trakttv.TvShowSyncTraktTvTask;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.actions.TmmAction;

/**
 * The class {@link TvShowSyncWatchedTraktTvAction}. To synchronize the watched state of your TV show library with trakt.tv
 * 
 * @author Manuel Laggner
 */
public class TvShowSyncWatchedTraktTvAction extends TmmAction {
  private static final long           serialVersionUID = 6640292090443882545L;
  

  public TvShowSyncWatchedTraktTvAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.synctraktwatched"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.synctraktwatched.desc"));
    putValue(SMALL_ICON, IconManager.WATCHED_MENU);
    putValue(LARGE_ICON_KEY, IconManager.WATCHED_MENU);
  }

  @Override
  protected void processAction(ActionEvent e) {
    TvShowSyncTraktTvTask task = new TvShowSyncTraktTvTask(TvShowList.getInstance().getTvShows());
    task.setSyncWatched(true);

    TmmTaskManager.getInstance().addUnnamedTask(task);
  }
}
