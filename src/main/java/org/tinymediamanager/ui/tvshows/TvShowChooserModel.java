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
package org.tinymediamanager.ui.tvshows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaRating;
import org.tinymediamanager.core.entities.MediaTrailer;
import org.tinymediamanager.core.entities.Person;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.tasks.TvShowThemeDownloadTask;
import org.tinymediamanager.scraper.ArtworkSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScraper;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.TrailerSearchAndScrapeOptions;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.MissingIdException;
import org.tinymediamanager.scraper.exceptions.NothingFoundException;
import org.tinymediamanager.scraper.exceptions.ScrapeException;
import org.tinymediamanager.scraper.interfaces.ITvShowArtworkProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;
import org.tinymediamanager.scraper.interfaces.ITvShowTrailerProvider;
import org.tinymediamanager.scraper.rating.RatingProvider;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MediaIdUtil;
import org.tinymediamanager.scraper.util.StrgUtils;

/**
 * @author Manuel Laggner
 */
public class TvShowChooserModel extends AbstractModelObject {

  private static final Logger            LOGGER          = LoggerFactory.getLogger(TvShowChooserModel.class);
  public static final TvShowChooserModel emptyResult     = new TvShowChooserModel();

  private MediaScraper                   mediaScraper    = null;
  private List<MediaScraper>             artworkScrapers = null;
  private List<MediaScraper>             trailerScrapers = null;
  private MediaLanguages                 language        = null;
  private MediaSearchResult              result          = null;
  private MediaMetadata                  metadata        = null;

  private float                          score           = 0;
  private String                         title           = "";
  private String                         originalTitle   = "";
  private String                         overview        = "";
  private String                         year            = "";
  private String                         id              = "";
  private String                         combinedName    = "";
  private String                         posterUrl       = "";
  private boolean                        scraped         = false;

  public TvShowChooserModel(MediaScraper mediaScraper, List<MediaScraper> artworkScrapers, List<MediaScraper> trailerScrapers,
      MediaSearchResult result, MediaLanguages language) {
    this.mediaScraper = mediaScraper;
    this.artworkScrapers = artworkScrapers;
    this.result = result;
    this.language = language;
    this.trailerScrapers = trailerScrapers;

    setTitle(result.getTitle());
    setOriginalTitle(result.getOriginalTitle());

    if (result.getYear() != 0) {
      setYear(Integer.toString(result.getYear()));
    }
    else {
      setYear("");
    }

    Object obj = result.getId();
    if (obj != null) {
      setId(obj.toString());
    }

    // combined title (title (year))
    setCombinedName();

    score = result.getScore();
  }

  /**
   * create the empty search result.
   */
  private TvShowChooserModel() {
    setTitle(TmmResourceBundle.getString("chooser.nothingfound"));
    combinedName = title;
  }

  public float getScore() {
    return score;
  }

  public void setTitle(String title) {
    String oldValue = this.title;
    this.title = StrgUtils.getNonNullString(title);
    firePropertyChange("title", oldValue, this.title);
  }

  public void setOriginalTitle(String originalTitle) {
    String oldValue = this.originalTitle;
    this.originalTitle = StrgUtils.getNonNullString(originalTitle);
    firePropertyChange("originalTitle", oldValue, this.originalTitle);
  }

  public void setOverview(String overview) {
    String oldValue = this.overview;
    this.overview = StrgUtils.getNonNullString(overview);
    firePropertyChange("overview", oldValue, this.overview);
  }

  public String getTitle() {
    return title;
  }

  public String getOriginalTitle() {
    return originalTitle;
  }

  public String getOverview() {
    return overview;
  }

  public String getPosterUrl() {
    return posterUrl;
  }

  public void setPosterUrl(String newValue) {
    String oldValue = posterUrl;
    posterUrl = StrgUtils.getNonNullString(newValue);
    firePropertyChange("posterUrl", oldValue, newValue);
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    String oldValue = this.year;
    this.year = year;
    firePropertyChange("year", oldValue, this.year);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    String oldValue = this.id;
    this.id = id;
    firePropertyChange("id", oldValue, this.id);
  }

  public void setCombinedName() {
    String oldValue = this.combinedName;

    if (StringUtils.isNotBlank(getYear())) {
      this.combinedName = getTitle() + " (" + getYear() + ")";
    }
    else {
      this.combinedName = getTitle();
    }
    firePropertyChange("combinedName", oldValue, this.combinedName);
  }

  public String getCombinedName() {
    return combinedName;
  }

  public MediaScraper getMediaScraper() {
    return mediaScraper;
  }

  public List<MediaScraper> getArtworkScrapers() {
    return artworkScrapers;
  }

  public void startTrailerScrapeTask(TvShow tvShow, boolean overwrite) {
    TmmTaskManager.getInstance().addUnnamedTask(new TrailerScrapeTask(tvShow, overwrite));
  }

  public void startThemeDownloadTask(TvShow tvShow, boolean overwrite) {
    TmmTaskManager.getInstance().addUnnamedTask(new TvShowThemeDownloadTask(Collections.singletonList(tvShow), overwrite));
  }

  /**
   * Scrape meta data.
   */
  public void scrapeMetaData() {
    try {
      // poster for preview
      setPosterUrl(result.getPosterUrl());

      TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
      options.setSearchResult(result);
      options.setLanguage(language);
      options.setCertificationCountry(TvShowModuleManager.getInstance().getSettings().getCertificationCountry());
      options.setReleaseDateCountry(TvShowModuleManager.getInstance().getSettings().getReleaseDateCountry());
      options.setIds(result.getIds());

      LOGGER.info("=====================================================");
      LOGGER.info("Scrape metadata with scraper: {}", mediaScraper.getMediaProvider().getProviderInfo().getId());
      LOGGER.info("{}", options);
      LOGGER.info("=====================================================");
      metadata = ((ITvShowMetadataProvider) mediaScraper.getMediaProvider()).getMetadata(options);
      // also inject other ids
      MediaIdUtil.injectMissingIds(metadata.getIds(), MediaType.TV_SHOW);

      if (TvShowModuleManager.getInstance().getSettings().isFetchAllRatings()) {
        for (MediaRating rating : ListUtils.nullSafe(RatingProvider.getRatings(metadata.getIds(), MediaType.TV_SHOW))) {
          if (!metadata.getRatings().contains(rating)) {
            metadata.addRating(rating);
          }
        }
      }

      setOverview(metadata.getPlot());

      if (StringUtils.isBlank(posterUrl) && !metadata.getMediaArt(MediaArtworkType.POSTER).isEmpty()) {
        setPosterUrl(metadata.getMediaArt(MediaArtworkType.POSTER).get(0).getPreviewUrl());
      }

      setScraped(true);

    }
    catch (MissingIdException e) {
      LOGGER.warn("missing id for scrape");
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowChooser", "scraper.error.missingid"));
    }
    catch (NothingFoundException ignored) {
      LOGGER.debug("nothing found");
    }
    catch (ScrapeException e) {
      LOGGER.error("getMetadata", e);
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowChooser", "message.scrape.metadatatvshowfailed",
          new String[] { ":", e.getLocalizedMessage() }));
    }
  }

  public List<TvShowEpisode> getEpisodesForDisplay() {
    List<TvShowEpisode> episodes = new ArrayList<>();

    if (!scraped) {
      return episodes;
    }

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setLanguage(language);
    options.setCertificationCountry(TvShowModuleManager.getInstance().getSettings().getCertificationCountry());
    options.setReleaseDateCountry(TvShowModuleManager.getInstance().getSettings().getReleaseDateCountry());

    for (Entry<String, Object> entry : metadata.getIds().entrySet()) {
      options.setId(entry.getKey(), entry.getValue().toString());
    }

    try {
      List<MediaMetadata> mediaEpisodes = ((ITvShowMetadataProvider) mediaScraper.getMediaProvider()).getEpisodeList(options);
      for (MediaMetadata me : mediaEpisodes) {
        TvShowEpisode ep = new TvShowEpisode();
        ep.setEpisode(me.getEpisodeNumber());
        ep.setSeason(me.getSeasonNumber());
        ep.setDvdEpisode(me.getDvdEpisodeNumber());
        ep.setDvdSeason(me.getDvdSeasonNumber());
        ep.setFirstAired(me.getReleaseDate());
        ep.setTitle(me.getTitle());
        ep.setOriginalTitle(me.getOriginalTitle());
        ep.setPlot(me.getPlot());
        ep.setActors(me.getCastMembers(Person.Type.ACTOR));
        ep.setDirectors(me.getCastMembers(Person.Type.DIRECTOR));
        ep.setWriters(me.getCastMembers(Person.Type.WRITER));

        episodes.add(ep);
      }
    }
    catch (MissingIdException e) {
      LOGGER.warn("missing id for scrape");
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowChooser", "scraper.error.missingid"));
    }
    catch (ScrapeException e) {
      LOGGER.error("getEpisodeList", e);
      MessageManager.instance.pushMessage(new Message(Message.MessageLevel.ERROR, "TvShowChooser", "message.scrape.episodelistfailed",
          new String[] { ":", e.getLocalizedMessage() }));
    }
    return episodes;
  }

  public MediaMetadata getMetadata() {
    return metadata;
  }

  private void setScraped(boolean newvalue) {
    boolean oldValue = scraped;
    scraped = newvalue;
    firePropertyChange("scraped", oldValue, newvalue);
  }

  public boolean isScraped() {
    return scraped;
  }

  public MediaLanguages getLanguage() {
    return language;
  }

  public void startArtworkScrapeTask(TvShow tvShow, List<TvShowScraperMetadataConfig> config, boolean overwrite) {
    TmmTaskManager.getInstance().addUnnamedTask(new ArtworkScrapeTask(tvShow, config, overwrite));
  }

  private class ArtworkScrapeTask extends TmmTask {
    private final TvShow                            tvShowToScrape;
    private final List<TvShowScraperMetadataConfig> config;
    private final boolean                           overwrite;

    public ArtworkScrapeTask(TvShow tvShow, List<TvShowScraperMetadataConfig> config, boolean overwrite) {
      super(TmmResourceBundle.getString("message.scrape.artwork") + " " + tvShow.getTitle(), 0, TaskType.BACKGROUND_TASK);
      this.tvShowToScrape = tvShow;
      this.config = config;
      this.overwrite = overwrite;
    }

    @Override
    protected void doInBackground() {
      if (!scraped) {
        return;
      }

      List<MediaArtwork> artwork = new ArrayList<>();

      ArtworkSearchAndScrapeOptions options = new ArtworkSearchAndScrapeOptions(MediaType.TV_SHOW);
      options.setArtworkType(MediaArtworkType.ALL);
      options.setLanguage(language);
      options.setMetadata(metadata);
      options.setIds(metadata.getIds());
      options.setLanguage(TvShowModuleManager.getInstance().getSettings().getImageScraperLanguage());
      options.setFanartSize(TvShowModuleManager.getInstance().getSettings().getImageFanartSize());
      options.setPosterSize(TvShowModuleManager.getInstance().getSettings().getImagePosterSize());

      for (Entry<String, Object> entry : tvShowToScrape.getIds().entrySet()) {
        options.setId(entry.getKey(), entry.getValue().toString());
      }

      // scrape providers till one artwork has been found
      for (MediaScraper artworkScraper : artworkScrapers) {
        ITvShowArtworkProvider artworkProvider = (ITvShowArtworkProvider) artworkScraper.getMediaProvider();
        try {
          artwork.addAll(artworkProvider.getArtwork(options));
        }
        catch (MissingIdException e) {
          LOGGER.debug("no id found for scraper {}", artworkScraper.getMediaProvider().getProviderInfo().getId());
        }
        catch (NothingFoundException e) {
          LOGGER.debug("did not find artwork for '{}'", tvShowToScrape.getTitle());
        }
        catch (ScrapeException e) {
          LOGGER.error("getArtwork", e);
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, tvShowToScrape, "message.scrape.tvshowartworkfailed", new String[] { ":", e.getLocalizedMessage() }));
        }
      }

      // at last take the poster from the result
      if (StringUtils.isNotBlank(getPosterUrl())) {
        MediaArtwork ma = new MediaArtwork(result.getProviderId(), MediaArtworkType.POSTER);
        ma.setDefaultUrl(getPosterUrl());
        ma.setPreviewUrl(getPosterUrl());
        artwork.add(ma);
      }

      tvShowToScrape.setArtwork(artwork, config, overwrite);
    }
  }

  private class TrailerScrapeTask extends TmmTask {
    private final TvShow  tvShowtoScrape;
    private final boolean overwrite;

    public TrailerScrapeTask(TvShow tvShow, boolean overwrite) {
      super(TmmResourceBundle.getString("message.scrape.trailer") + " " + tvShow.getTitle(), 0, TaskType.BACKGROUND_TASK);
      this.tvShowtoScrape = tvShow;
      this.overwrite = overwrite;
    }

    @Override
    protected void doInBackground() {
      if (!scraped) {
        return;
      }

      if (!overwrite && !tvShowtoScrape.getTrailer().isEmpty()) {
        return;
      }

      List<MediaTrailer> trailer = new ArrayList<>();

      TrailerSearchAndScrapeOptions options = new TrailerSearchAndScrapeOptions(MediaType.TV_SHOW);
      options.setMetadata(metadata);
      options.setIds(metadata.getIds());
      options.setLanguage(language);

      // scrape trailers
      for (MediaScraper trailerScraper : trailerScrapers) {
        try {
          ITvShowTrailerProvider trailerProvider = (ITvShowTrailerProvider) trailerScraper.getMediaProvider();
          trailer.addAll(trailerProvider.getTrailers(options));
        }
        catch (MissingIdException ignored) {
          LOGGER.debug("no id found for scraper {}", trailerScraper.getMediaProvider().getProviderInfo().getId());
        }
        catch (ScrapeException e) {
          LOGGER.error("getTrailers {}", e.getMessage());
          MessageManager.instance.pushMessage(
              new Message(MessageLevel.ERROR, "TvShowChooser", "message.scrape.trailerfailed", new String[] { ":", e.getLocalizedMessage() }));
        }
      }

      tvShowtoScrape.setTrailers(trailer);
      tvShowtoScrape.saveToDb();
      tvShowtoScrape.writeNFO();

      // start automatic movie trailer download
      TvShowHelpers.startAutomaticTrailerDownload(tvShowtoScrape);
    }
  }
}
