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

package org.tinymediamanager.core.tvshow.filenaming;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.tvshow.ITvShowSeasonFileNaming;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;

/**
 * The Enum TvShowSeasonFanartNaming.
 * 
 * @author Manuel Laggner
 */
public enum TvShowSeasonFanartNaming implements ITvShowSeasonFileNaming {
  /** seasonXX-fanart.* */
  SEASON_FANART {
    @Override
    public String getFilename(TvShow tvShow, int season, String extension) {
      if (season == -1) {
        return "season-all-fanart." + extension;
      }
      else if (season == 0 && TvShowModuleManager.getInstance().getSettings().isSpecialSeason()) {
        return "season-specials-fanart." + extension;
      }
      else if (season > -1) {
        return String.format("season%02d-fanart.%s", season, extension);
      }
      else {
        return "";
      }
    }
  },

  /** season_folder/seasonXX-fanart.* */
  SEASON_FOLDER {
    @Override
    public String getFilename(TvShow tvShow, int season, String extension) {
      String seasonFoldername = TvShowHelpers.detectSeasonFolder(tvShow, season);

      // check whether the season folder name exists or not; do not create it just for the artwork!
      if (StringUtils.isBlank(seasonFoldername)) {
        // no season folder name in the templates found - fall back to the the show base filename style
        return SEASON_FANART.getFilename(tvShow, season, extension);
      }

      String filename = seasonFoldername + File.separator;

      if (season == -1) {
        filename += "season-all-fanart";
      }
      else if (season == 0 && TvShowModuleManager.getInstance().getSettings().isSpecialSeason()) {
        filename += "season-specials-fanart";
      }
      else if (season > -1) {
        filename += String.format("season%02d-fanart", season);
      }
      else {
        return "";
      }
      return filename + "." + extension;
    }
  }
}
