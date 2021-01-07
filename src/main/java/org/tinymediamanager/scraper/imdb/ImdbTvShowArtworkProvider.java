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
package org.tinymediamanager.scraper.imdb;

import java.util.Collections;
import java.util.List;

import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;

/**
 * the class {@link ImdbTvShowArtworkProvider} is used to provide artwork for TV shows
 *
 * @author Manuel Laggner
 */
public class ImdbTvShowArtworkProvider extends ImdbMetadataProvider implements ITvShowArtworkProvider {

  @Override
  protected String getSubId() {
    return "tvshow_artwork";
  }

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public List<MediaArtwork> getArtwork(ArtworkSearchAndScrapeOptions options) throws ScrapeException, MissingIdException {
    if (options.getArtworkType() == MediaArtwork.MediaArtworkType.ALL || options.getArtworkType() == MediaArtwork.MediaArtworkType.POSTER) {
      switch (options.getMediaType()) {
        case MOVIE:
          return (new ImdbMovieParser(getProviderInfo().getConfig(), executor)).getMovieArtwork(options);

        case TV_SHOW:
          return (new ImdbTvShowParser(getProviderInfo().getConfig(), executor)).getTvShowArtwork(options);

        default:
          break;
      }
    }
    return Collections.emptyList();
  }
}
