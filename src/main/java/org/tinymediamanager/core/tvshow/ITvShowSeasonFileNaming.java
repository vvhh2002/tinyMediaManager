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

package org.tinymediamanager.core.tvshow;

import org.tinymediamanager.core.tvshow.entities.TvShow;

/**
 * the interface ITvShowFileNaming is used for generating file names for tv show related files
 */
public interface ITvShowSeasonFileNaming {
  /**
   * get the file name for this enum
   *
   * @param tvShow
   *          the TV show
   * @param season
   *          the season to create the filename for
   * @param extension
   *          the file extension
   * @return the file name or an empty string
   */
  String getFilename(TvShow tvShow, int season, String extension);
}
