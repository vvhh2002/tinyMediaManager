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
package org.tinymediamanager.core.tasks;

import static org.tinymediamanager.core.Constants.MEDIA_INFORMATION;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Message;
import org.tinymediamanager.core.Message.MessageLevel;
import org.tinymediamanager.core.MessageManager;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.threading.TmmTaskManager;

/**
 * The Class MediaFileInformationFetcherTask.
 * 
 * @author Manuel Laggner
 */
public class MediaFileInformationFetcherTask implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(MediaFileInformationFetcherTask.class);

  protected final MediaFile   mediaFile;
  protected final MediaEntity mediaEntity;
  private final long          uniqueId;
  private final boolean       forceUpdate;

  /**
   * Instantiates a new media file information fetcher task.
   * 
   * @param mediaFile
   *          the media files
   * @param mediaEntity
   *          the media entity
   * @param forceUpdate
   *          force an update
   */
  public MediaFileInformationFetcherTask(MediaFile mediaFile, MediaEntity mediaEntity, boolean forceUpdate) {
    this.mediaFile = mediaFile;
    this.mediaEntity = mediaEntity;
    this.forceUpdate = forceUpdate;
    this.uniqueId = TmmTaskManager.getInstance().GLOB_THRD_CNT.incrementAndGet();
  }

  @Override
  public void run() {
    // try/catch block in the root of the thread to log crashes
    try {
      String name = Thread.currentThread().getName();
      if (!name.contains("-G")) {
        name = name + "-G0";
      }
      name = name.replaceAll("\\-G\\d+", "-G" + uniqueId);
      Thread.currentThread().setName(name);

      mediaFile.gatherMediaInformation(forceUpdate);
      if (mediaFile.getType() == MediaFileType.SUBTITLE || mediaFile.getType() == MediaFileType.AUDIO) {
        // also re-evaluate subtitle language/title if it starts with the same basename
        MediaFile mainVideoFile = mediaEntity.getMainFile();
        if (StringUtils.isNotBlank(mainVideoFile.getBasename()) && mediaFile.getFilename().startsWith(mainVideoFile.getBasename())) {
          MediaFileHelper.gatherLanguageInformation(mediaFile, mainVideoFile.getBasename());
        }
      }

      if (mediaFile.hasSubtitles()) {
        mediaEntity.firePropertyChange("hasSubtitles", false, true);
      }

      // add the -mediainfo.xml if it has been written
      if (Settings.getInstance().isWriteMediaInfoXml()) {
        Path xmlFile = Paths.get(mediaFile.getPath(), FilenameUtils.getBaseName(mediaFile.getFilename()) + "-mediainfo.xml");
        if (Files.exists(xmlFile)) {
          mediaEntity.addToMediaFiles(new MediaFile(xmlFile));
        }
      }
    }
    catch (Exception e) {
      LOGGER.error("Thread crashed: ", e);
      MessageManager.instance.pushMessage(
          new Message(MessageLevel.ERROR, "MediaInformation", "message.mediainfo.threadcrashed", new String[] { ":", e.getLocalizedMessage() }));
    }

    if (mediaEntity != null) {
      mediaEntity.callbackForGatheredMediainformation(mediaFile);
      mediaEntity.saveToDb();
      mediaEntity.firePropertyChange(MEDIA_INFORMATION, false, true);
    }

    callbackForGatheredMediainformation();
  }

  /**
   * a callback which could be called after finishing this task
   */
  public void callbackForGatheredMediainformation() {
    // can be overwritten to include coding
  }
}
