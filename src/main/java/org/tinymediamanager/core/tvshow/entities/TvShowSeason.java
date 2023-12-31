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
package org.tinymediamanager.core.tvshow.entities;

import static org.tinymediamanager.core.Constants.ADDED_EPISODE;
import static org.tinymediamanager.core.Constants.BANNER;
import static org.tinymediamanager.core.Constants.BANNER_URL;
import static org.tinymediamanager.core.Constants.FANART;
import static org.tinymediamanager.core.Constants.FANART_URL;
import static org.tinymediamanager.core.Constants.FIRST_AIRED;
import static org.tinymediamanager.core.Constants.MEDIA_FILES;
import static org.tinymediamanager.core.Constants.POSTER;
import static org.tinymediamanager.core.Constants.POSTER_URL;
import static org.tinymediamanager.core.Constants.REMOVED_EPISODE;
import static org.tinymediamanager.core.Constants.SEASON;
import static org.tinymediamanager.core.Constants.THUMB;
import static org.tinymediamanager.core.Constants.THUMB_URL;

import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.tinymediamanager.core.AbstractModelObject;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.TvShowScraperMetadataConfig;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;

/**
 * The Class TvShowSeason.
 * 
 * @author Manuel Laggner
 */
public class TvShowSeason extends AbstractModelObject implements Comparable<TvShowSeason> {

  private final TvShow                 tvShow;
  private final List<TvShowEpisode>    episodes    = new CopyOnWriteArrayList<>();
  private final PropertyChangeListener listener;
  private final int                    season;

  private String                       title       = "";
  private Date                         lastWatched = null;

  public TvShowSeason(int season, TvShow tvShow) {
    this.season = season;
    this.tvShow = tvShow;
    listener = evt -> {
      if (evt.getSource() instanceof TvShowEpisode) {
        TvShowEpisode episode = (TvShowEpisode) evt.getSource();

        switch (evt.getPropertyName()) {
          case MEDIA_FILES:
            firePropertyChange(MEDIA_FILES, null, evt.getNewValue());
            break;

          case SEASON:
            if (episode.getSeason() != season) {
              removeEpisode(episode);
            }
            break;

          case FIRST_AIRED:
            firePropertyChange(FIRST_AIRED, null, evt.getNewValue());
            break;
        }
      }
    };
  }

  public int getSeason() {
    return season;
  }

  /**
   * checks whether the parent {@link TvShow} is locked
   * 
   * @return true/false
   */
  public boolean isLocked() {
    return getTvShow() == null || getTvShow().isLocked();
  }

  public void setTitle(String newValue) {
    String oldValue = this.title;
    this.title = newValue;
    firePropertyChange("title", oldValue, newValue);

    // store the title inside the TV show itself
    getTvShow().addSeasonTitle(season, newValue);
  }

  public String getTitle() {
    return this.title;
  }

  public TvShow getTvShow() {
    return tvShow;
  }

  public synchronized void addEpisode(TvShowEpisode episode) {
    // do not add twice
    if (episode == null || episodes.contains(episode)) {
      return;
    }

    // when adding a new episode, check:
    for (TvShowEpisode e : episodes) {
      // - if that is a dummy episode; do not add it if a the real episode is available
      if (episode.isDummy() && ((episode.getEpisode() == e.getEpisode() && episode.getSeason() == e.getSeason())
          || (episode.getDvdEpisode() > 0 && episode.getDvdEpisode() == e.getDvdEpisode() && episode.getDvdSeason() == e.getDvdSeason()))) {
        return;
      }
      // - if that is a real episode; remove the corresponding dummy episode if available
      if (!episode.isDummy() && e.isDummy() && ((episode.getEpisode() == e.getEpisode() && episode.getSeason() == e.getSeason())
          || (episode.getDvdEpisode() > 0 && episode.getDvdEpisode() == e.getDvdEpisode() && episode.getDvdSeason() == e.getDvdSeason()))) {
        tvShow.removeEpisode(e);
      }
    }

    episodes.add(episode);
    episodes.sort(TvShowEpisode::compareTo);
    episode.addPropertyChangeListener(listener);
    firePropertyChange(ADDED_EPISODE, null, episodes);
    firePropertyChange(FIRST_AIRED, null, getFirstAired());
  }

  public void removeEpisode(TvShowEpisode episode) {
    episodes.remove(episode);
    episode.removePropertyChangeListener(listener);
    firePropertyChange(REMOVED_EPISODE, null, episodes);
    firePropertyChange(FIRST_AIRED, null, getFirstAired());
  }

  public List<TvShowEpisode> getEpisodes() {
    List<TvShowEpisode> episodes = new ArrayList<>();
    for (TvShowEpisode episode : this.episodes) {
      if (!episode.isDummy()) {
        episodes.add(episode);
      }
    }
    return episodes;
  }

  /**
   * get the firstAired of the first episode here
   * 
   * @return the first aired date of the first episode or null
   */
  public Date getFirstAired() {
    if (episodes.isEmpty()) {
      return null;
    }

    return episodes.get(0).getFirstAired();
  }

  /**
   * Checks if all episodes are watched.
   *
   * @return true, if all episodes are watched
   */
  public boolean isWatched() {
    boolean episodeFound = false;
    boolean watched = true;

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy()) {
        episodeFound = true;
        watched = watched && episode.isWatched();
      }
    }

    // at least 1 non-dummy found -> pass the collected watched state
    if (episodeFound) {
      return watched;
    }

    // only dummy episodes -> return false
    return false;
  }

  /**
   * checks if all episode has subtitles
   *
   * @return true, is all episodes have subtitles
   */
  public boolean hasEpisodeSubtitles() {
    boolean subtitles = true;

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy() && !episode.getHasSubtitles()) {
        subtitles = false;
        break;
      }
    }

    return subtitles;
  }

  /**
   * Checks if all episodes of that season have artwork assigned
   *
   * @return true if artwork is available
   */
  public Boolean getHasEpisodeImages() {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy() && !tvShowList.detectMissingArtwork(episode).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if all episodes of that season have valis metadata
   *
   * @return true if all chosen metadata fields are filled
   */
  public Boolean getHasEpisodeMetadata() {
    TvShowList tvShowList = TvShowModuleManager.getInstance().getTvShowList();

    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy() && !tvShowList.detectMissingMetadata(episode).isEmpty()) {
        return false;
      }
    }

    return true;
  }

  public boolean isDummy() {
    for (TvShowEpisode episode : episodes) {
      if (!episode.isDummy()) {
        return false;
      }
    }
    return true;
  }

  public List<TvShowEpisode> getEpisodesForDisplay() {
    return episodes;
  }

  public long getVideoFilesize() {
    long filesize = 0;
    for (TvShowEpisode episode : episodes) {
      filesize += episode.getVideoFilesize();
    }
    return filesize;
  }

  public long getTotalFilesize() {
    long filesize = 0;
    for (TvShowEpisode episode : episodes) {
      filesize += episode.getTotalFilesize();
    }
    return filesize;
  }

  public void setArtwork(MediaFile mediaFile) {
    MediaArtworkType artworkType = MediaFileType.getMediaArtworkType(mediaFile.getType());
    String oldValue = getArtworkFilename(artworkType);
    String newValue = mediaFile.getFile().toString();

    tvShow.setSeasonArtwork(season, mediaFile);

    switch (artworkType) {
      case SEASON_POSTER:
        firePropertyChange(POSTER, oldValue, newValue);
        break;

      case SEASON_FANART:
        firePropertyChange(FANART, oldValue, newValue);
        break;

      case SEASON_BANNER:
        firePropertyChange(BANNER, oldValue, newValue);
        break;

      case SEASON_THUMB:
        firePropertyChange(THUMB, oldValue, newValue);
        break;

      default:
        return;
    }

    for (TvShowEpisode episode : episodes) {
      episode.setSeasonArtworkChanged(artworkType);
    }
  }

  public void clearArtwork(MediaArtworkType artworkType) {
    tvShow.clearSeasonArtwork(season, artworkType);

    switch (artworkType) {
      case SEASON_POSTER:
        firePropertyChange(POSTER, null, "");
        break;

      case SEASON_FANART:
        firePropertyChange(FANART, null, "");
        break;

      case SEASON_BANNER:
        firePropertyChange(BANNER, null, "");
        break;

      case SEASON_THUMB:
        firePropertyChange(THUMB, null, "");
        break;
    }
  }

  public String getArtworkFilename(MediaArtworkType type) {
    return tvShow.getSeasonArtwork(season, type);
  }

  public Dimension getArtworkSize(MediaArtworkType type) {
    return tvShow.getSeasonArtworkSize(season, type);
  }

  /**
   * <b>PHYSICALLY</b> deletes all {@link MediaFile}s of the given type
   *
   * @param type
   *          the {@link MediaArtworkType} for all {@link MediaFile}s to delete
   */
  public void deleteArtworkFiles(MediaArtworkType type) {
    tvShow.deleteSeasonArtworkFiles(season, type);

    switch (type) {
      case SEASON_POSTER:
        firePropertyChange(POSTER, null, "");
        break;

      case SEASON_FANART:
        firePropertyChange(FANART, null, "");
        break;

      case SEASON_BANNER:
        firePropertyChange(BANNER, null, "");
        break;

      case SEASON_THUMB:
        firePropertyChange(THUMB, null, "");
        break;
    }
  }

  public void setArtworkUrl(String newValue, MediaArtworkType artworkType) {
    String oldValue = getArtworkUrl(artworkType);
    tvShow.setSeasonArtworkUrl(season, newValue, artworkType);

    switch (artworkType) {
      case SEASON_POSTER:
        firePropertyChange(POSTER_URL, oldValue, newValue);
        break;

      case SEASON_FANART:
        firePropertyChange(FANART_URL, oldValue, newValue);
        break;

      case SEASON_BANNER:
        firePropertyChange(BANNER_URL, oldValue, newValue);
        break;

      case SEASON_THUMB:
        firePropertyChange(THUMB_URL, oldValue, newValue);
        break;

      default:
        return;
    }

    for (TvShowEpisode episode : episodes) {
      episode.setSeasonArtworkChanged(artworkType);
    }
  }

  public void removeArtworkUrl(MediaArtworkType artworkType) {
    tvShow.clearSeasonArtworkUrl(season, artworkType);
  }

  public String getArtworkUrl(MediaArtworkType type) {
    return tvShow.getSeasonArtworkUrl(season, type);
  }

  public void downloadArtwork(MediaArtworkType artworkType) {
    tvShow.downloadSeasonArtwork(season, artworkType);
  }

  public List<MediaFile> getMediaFiles() {
    ArrayList<MediaFile> mfs = new ArrayList<>();
    Set<MediaFile> unique = new LinkedHashSet<>(mfs);
    for (TvShowEpisode episode : episodes) {
      unique.addAll(episode.getMediaFiles());
    }
    mfs.addAll(unique);
    return mfs;
  }

  public List<MediaFile> getMediaFiles(MediaFileType type) {
    ArrayList<MediaFile> mfs = new ArrayList<>();
    Set<MediaFile> unique = new LinkedHashSet<>(mfs);
    for (TvShowEpisode episode : episodes) {
      unique.addAll(episode.getMediaFiles(type));
    }
    mfs.addAll(unique);
    return mfs;
  }

  public boolean isNewlyAdded() {
    for (TvShowEpisode episode : episodes) {
      if (episode.isNewlyAdded()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the images to cache.
   *
   * @return the images to cache
   */
  public List<MediaFile> getImagesToCache() {
    // get files to cache
    return getMediaFiles().stream().filter(MediaFile::isGraphic).collect(Collectors.toList());
  }

  public Date getLastWatched() {
    return lastWatched;
  }

  public void setLastWatched(Date lastWatched) {
    this.lastWatched = lastWatched;
  }

  public Object getValueForMetadata(TvShowScraperMetadataConfig metadataConfig) {

    switch (metadataConfig) {
      case TITLE:
        return getTitle();

      case SEASON_POSTER:
        return getArtworkFilename(MediaArtworkType.SEASON_POSTER);

      case SEASON_FANART:
        return getArtworkFilename(MediaArtworkType.SEASON_FANART);

      case SEASON_BANNER:
        return getArtworkFilename(MediaArtworkType.SEASON_BANNER);

      case SEASON_THUMB:
        return getArtworkFilename(MediaArtworkType.SEASON_THUMB);
    }

    return null;
  }

  @Override
  public int compareTo(TvShowSeason o) {
    if (getTvShow() != o.getTvShow()) {
      return getTvShow().getTitle().compareTo(o.getTvShow().getTitle());
    }
    return Integer.compare(getSeason(), o.getSeason());
  }
}
