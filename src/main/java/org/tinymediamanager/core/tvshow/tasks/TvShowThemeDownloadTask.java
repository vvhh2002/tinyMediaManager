/*
 * Copyright 2012 - 2020 Manuel Laggner
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
package org.tinymediamanager.core.tvshow.tasks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmThreadPool;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.http.Url;

/**
 * The class {@link TvShowThemeDownloadTask} is used to download Tv show theme files (music)
 * 
 * @author Manuel Laggner
 */
public class TvShowThemeDownloadTask extends TmmThreadPool {
  private static final Logger         LOGGER  = LoggerFactory.getLogger(TvShowThemeDownloadTask.class);
  private static final ResourceBundle BUNDLE  = ResourceBundle.getBundle("messages");

  private final List<TvShow>          tvShows = new ArrayList<>();

  public TvShowThemeDownloadTask(List<TvShow> tvShows) {
    super(BUNDLE.getString("theme.download"));
    this.tvShows.addAll(tvShows);
  }

  @Override
  protected void doInBackground() {
    initThreadPool(3, "themeDownload");
    start();

    for (TvShow tvShow : tvShows) {
      submitTask(new Worker(tvShow));
    }

    waitForCompletionOrCancel();

    LOGGER.info("Done downloading themes");
  }

  @Override
  public void callback(Object obj) {
    // do not publish task description here, because with different workers the text is never right
    publishState(progressDone);
  }

  /****************************************************************************************
   * Helper classes
   ****************************************************************************************/
  private static class Worker implements Runnable {
    private final TvShow tvShow;

    Worker(TvShow tvShow) {
      this.tvShow = tvShow;
    }

    @Override
    public void run() {
      try {
        int tvdbId = tvShow.getIdAsInt(MediaMetadata.TVDB);

        if (tvdbId > 0) {
          LOGGER.debug("found tvdbId '{}' - try to download the theme for it", tvdbId);

          String filename = "theme.mp3";
          Path destFile = tvShow.getPathNIO().resolve(filename);
          Path tempFile = null;
          try {
            long timestamp = System.currentTimeMillis();

            try {
              // create a temp file/folder inside the temp folder or tmm folder
              Path tempFolder = Paths.get(Utils.getTempFolder());
              if (!Files.exists(tempFolder)) {
                Files.createDirectory(tempFolder);
              }
              tempFile = tempFolder.resolve("theme." + tvdbId + "." + timestamp + ".part"); // multi episode same file
            }
            catch (Exception e) {
              LOGGER.debug("could not write to temp folder: {}", e.getMessage());

              // could not create the temp folder somehow - put the files into the entity dir
              tempFile = destFile.resolveSibling("theme." + tvdbId + "." + timestamp + ".part"); // multi episode same file
            }

            // fetch and store images
            String urlAsString = "http://tvthemes.plexapp.com/" + tvdbId + ".mp3";
            Url url;
            try {
              url = new Url(urlAsString);
            }
            catch (Exception e) {
              LOGGER.error("downloading {} - {}", urlAsString, e.getMessage());
              throw e;
            }

            try (InputStream is = url.getInputStreamWithRetry(5); FileOutputStream outputStream = new FileOutputStream(tempFile.toFile())) {

              IOUtils.copy(is, outputStream);
              Utils.flushFileOutputStreamToDisk(outputStream);
            }

            // check if the file has been downloaded
            if (!Files.exists(tempFile) || Files.size(tempFile) == 0) {
              // cleanup the file
              FileUtils.deleteQuietly(tempFile.toFile());
              throw new IOException("0byte file downloaded: " + filename);
            }

            // delete new destination if existing
            Utils.deleteFileSafely(destFile);

            // move the temp file to the expected filename
            if (!Utils.moveFileSafe(tempFile, destFile)) {
              throw new IOException("renaming temp file failed: " + filename);
            }

            // and add it to the media files
            MediaFile mediaFile = new MediaFile(destFile);
            mediaFile.gatherMediaInformation();
            tvShow.addToMediaFiles(mediaFile);
          }
          finally {
            // remove temp file
            if (tempFile != null && Files.exists(tempFile)) {
              Utils.deleteFileSafely(tempFile);
            }
          }
        }
      }
      catch (Exception e) {
        LOGGER.error("Thread crashed", e);
        MessageManager.instance.pushMessage(
            new Message(MessageLevel.ERROR, "ThemeDownloader", "message.scrape.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
      }
    }
  }
}