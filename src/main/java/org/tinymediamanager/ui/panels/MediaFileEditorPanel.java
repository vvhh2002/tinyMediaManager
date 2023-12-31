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
package org.tinymediamanager.ui.panels;

import static org.tinymediamanager.core.MediaFileType.NFO;
import static org.tinymediamanager.core.MediaFileType.SAMPLE;
import static org.tinymediamanager.core.MediaFileType.TRAILER;
import static org.tinymediamanager.core.MediaFileType.VIDEO;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Converter;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.observablecollections.ObservableCollections;
import org.jdesktop.swingbinding.JTableBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.tinymediamanager.core.AspectRatio;
import org.tinymediamanager.core.MediaFileHelper;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFile;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.core.entities.MediaFileSubtitle;
import org.tinymediamanager.core.tasks.ARDetectorTask;
import org.tinymediamanager.core.tasks.MediaFileARDetectorTask;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle;
import org.tinymediamanager.core.threading.TmmTaskListener;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.thirdparty.FFmpeg;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.IntegerInputVerifier;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.SquareIconButton;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTable;

import net.miginfocom.swing.MigLayout;

/**
 * The class MediaFileEditorPanel is used to maintain associated media files
 *
 * @author Manuel Laggner
 */
public class MediaFileEditorPanel extends JPanel {
  private static final long                serialVersionUID = -2416409052145301941L;

  private final BindingGroup               bindingGroup     = new BindingGroup();

  private TmmTask                          ardTask;
  private List<MediaFileContainer>         mediaFiles;
  private TmmTable                         tableMediaFiles;
  private JLabel                           lblFilename;
  private JTextField                       tfCodec;
  private JTextField                       tfContainerFormat;
  private JTextField                       tfWidth;
  private JTextField                       tfHeight;
  private TmmTable                         tableAudioStreams;
  private TmmTable                         tableSubtitles;
  private JButton                          btnAddAudioStream;
  private JButton                          btnRemoveAudioStream;
  private JButton                          btnAddSubtitle;
  private JButton                          btnRemoveSubtitle;
  private JComboBox<String>                cb3dFormat;
  private JComboBox                        cbAspectRatio;
  private JComboBox                        cbAspectRatio2;
  private JSpinner                         spFrameRate;
  private JTextField                       tfBitDepth;
  private JTextField                       tfHdrFormat;
  private JTextField                       tfVideoBitrate;
  private JTextField                       tfRuntime;
  private JButton                          btnARD;

  private final List<AspectRatioContainer> aspectRatios     = new ArrayList<>();
  private final List<AspectRatioContainer> aspectRatios2    = new ArrayList<>();

  public MediaFileEditorPanel(List<MediaFile> mediaFiles) {

    this.mediaFiles = ObservableCollections.observableList(new ArrayList<>());
    for (MediaFile mediaFile : mediaFiles) {
      MediaFileContainer container = new MediaFileContainer(mediaFile);
      this.mediaFiles.add(container);
    }

    Set<MediaFileType> videoTypes = new HashSet<>(Arrays.asList(VIDEO, SAMPLE, TRAILER));

    // predefined 3D Formats
    String[] threeDFormats = { "", MediaFileHelper.VIDEO_3D, MediaFileHelper.VIDEO_3D_SBS, MediaFileHelper.VIDEO_3D_HSBS,
        MediaFileHelper.VIDEO_3D_TAB, MediaFileHelper.VIDEO_3D_HTAB, MediaFileHelper.VIDEO_3D_MVC };

    setLayout(new MigLayout("", "[300lp:450lp,grow]", "[200lp:450lp,grow]"));
    {
      JSplitPane splitPane = new JSplitPane();
      splitPane.setName("mediafileEditor.splitPane");
      TmmUILayoutStore.getInstance().install(splitPane);
      add(splitPane, "cell 0 0,grow");
      {
        JPanel panelMediaFiles = new JPanel();
        panelMediaFiles.setLayout(new MigLayout("", "[200lp:250lp,grow]", "[200lp:300lp,grow]"));

        JScrollPane scrollPaneMediaFiles = new JScrollPane();
        panelMediaFiles.add(scrollPaneMediaFiles, "cell 0 0,grow");
        splitPane.setLeftComponent(panelMediaFiles);

        tableMediaFiles = new TmmTable();
        tableMediaFiles.configureScrollPane(scrollPaneMediaFiles);
      }
      {
        JPanel panelDetails = new JPanel();
        splitPane.setRightComponent(panelDetails);
        panelDetails
            .setLayout(new MigLayout("", "[][75lp:n][20lp:n][][75lp:n][20lp:n][][75lp:n][50lp:n,grow]", "[][][][][][][100lp:150lp][100lp:150lp]"));
        {
          lblFilename = new JLabel("");
          TmmFontHelper.changeFont(lblFilename, 1.167, Font.BOLD);
          panelDetails.add(lblFilename, "cell 0 0 9 1,growx");
        }
        {
          JLabel lblCodec = new TmmLabel(TmmResourceBundle.getString("metatag.codec"));
          panelDetails.add(lblCodec, "cell 0 1,alignx right");

          tfCodec = new JTextField();
          panelDetails.add(tfCodec, "cell 1 1,growx");
          tfCodec.setColumns(10);
        }
        {
          JLabel lblContainerFormat = new TmmLabel(TmmResourceBundle.getString("metatag.container"));
          panelDetails.add(lblContainerFormat, "cell 3 1,alignx right");

          tfContainerFormat = new JTextField();
          panelDetails.add(tfContainerFormat, "cell 4 1,growx");
          tfContainerFormat.setColumns(10);
        }
        {
          JLabel lblWidth = new TmmLabel(TmmResourceBundle.getString("metatag.width"));
          panelDetails.add(lblWidth, "cell 0 2,alignx right");

          tfWidth = new JTextField();
          tfWidth.setInputVerifier(new IntegerInputVerifier());
          panelDetails.add(tfWidth, "cell 1 2,growx");
          tfWidth.setColumns(10);
        }
        {
          JLabel lblHeight = new TmmLabel(TmmResourceBundle.getString("metatag.height"));
          panelDetails.add(lblHeight, "cell 3 2,alignx right");

          tfHeight = new JTextField();
          tfHeight.setInputVerifier(new IntegerInputVerifier());
          panelDetails.add(tfHeight, "cell 4 2,growx");
          tfHeight.setColumns(10);
        }
        {
          btnARD = new JButton(new ScanAspectRationAction());
          MediaFile mf = MediaFileEditorPanel.this.mediaFiles.get(0).mediaFile;
          btnARD.setEnabled(videoTypes.contains(mf.getType()));
          panelDetails.add(btnARD, "cell 7 1");
        }
        {
          JLabel lblAspectT = new TmmLabel(TmmResourceBundle.getString("metatag.aspect"));
          panelDetails.add(lblAspectT, "cell 6 2,alignx right");

          cbAspectRatio = new JComboBox(getAspectRatios().toArray(new AspectRatioContainer[0]));
          cbAspectRatio.setEditable(true);
          panelDetails.add(cbAspectRatio, "cell 7 2");
        }
        {
          JLabel lblAspectT = new TmmLabel(TmmResourceBundle.getString("metatag.aspect2"));
          panelDetails.add(lblAspectT, "cell 6 3,alignx right");

          cbAspectRatio2 = new JComboBox(getAspectRatios2().toArray(new AspectRatioContainer[0]));
          cbAspectRatio2.setEditable(true);
          panelDetails.add(cbAspectRatio2, "cell 7 3");
        }
        {
          JLabel lblFrameRate = new TmmLabel(TmmResourceBundle.getString("metatag.framerate"));
          panelDetails.add(lblFrameRate, "cell 0 3,alignx trailing");

          spFrameRate = new JSpinner(new SpinnerNumberModel(0, 0, 999, 0.01d));
          panelDetails.add(spFrameRate, "cell 1 3,growx");
        }
        {
          JLabel lblVideoBitrate = new TmmLabel(TmmResourceBundle.getString("metatag.bitrate"));
          panelDetails.add(lblVideoBitrate, "cell 3 3,alignx trailing");

          tfVideoBitrate = new JTextField();
          panelDetails.add(tfVideoBitrate, "cell 4 3");
          tfVideoBitrate.setColumns(10);
          tfVideoBitrate.setInputVerifier(new IntegerInputVerifier());
        }
        {
          JLabel lblBitDepthT = new TmmLabel(TmmResourceBundle.getString("metatag.videobitdepth"));
          panelDetails.add(lblBitDepthT, "cell 0 4,alignx trailing");

          tfBitDepth = new JTextField();
          tfBitDepth.setInputVerifier(new IntegerInputVerifier());
          panelDetails.add(tfBitDepth, "cell 1 4,growx");
          tfBitDepth.setColumns(10);
        }
        {
          JLabel lblHdrFormatT = new TmmLabel(TmmResourceBundle.getString("metatag.hdrformat"));
          panelDetails.add(lblHdrFormatT, "cell 3 4,alignx trailing");

          tfHdrFormat = new JTextField();
          panelDetails.add(tfHdrFormat, "cell 4 4,growx");
          tfHdrFormat.setColumns(10);
        }
        {
          JLabel lblRuntimeT = new TmmLabel(TmmResourceBundle.getString("metatag.runtime"));
          panelDetails.add(lblRuntimeT, "cell 0 5,alignx trailing");

          tfRuntime = new JTextField();
          panelDetails.add(tfRuntime, "cell 1 5");
          tfRuntime.setColumns(5);
          tfRuntime.setInputVerifier(new IntegerInputVerifier());
        }
        {
          JLabel lbl3d = new TmmLabel(TmmResourceBundle.getString("metatag.3dformat"));
          panelDetails.add(lbl3d, "cell 3 5,alignx right");

          cb3dFormat = new JComboBox(threeDFormats);
          panelDetails.add(cb3dFormat, "cell 4 5");
        }
        {
          JLabel lblAudiostreams = new TmmLabel(TmmResourceBundle.getString("metatag.countAudioStreams"));
          panelDetails.add(lblAudiostreams, "flowy,cell 0 6,alignx right,aligny top");

          JScrollPane scrollPane = new JScrollPane();
          panelDetails.add(scrollPane, "cell 1 6 8 1,grow");

          tableAudioStreams = new TmmTable();
          tableAudioStreams.configureScrollPane(scrollPane);
        }
        {
          JLabel lblSubtitles = new TmmLabel(TmmResourceBundle.getString("metatag.subtitles"));
          panelDetails.add(lblSubtitles, "flowy,cell 0 7,alignx right,aligny top");

          JScrollPane scrollPane = new JScrollPane();
          panelDetails.add(scrollPane, "cell 1 7 8 1,grow");

          tableSubtitles = new TmmTable();
          tableSubtitles.configureScrollPane(scrollPane);
        }
        {
          btnAddAudioStream = new SquareIconButton(new AddAudioStreamAction());
          panelDetails.add(btnAddAudioStream, "cell 0 6,alignx right,aligny top");
        }
        {
          btnRemoveAudioStream = new SquareIconButton(new RemoveAudioStreamAction());
          panelDetails.add(btnRemoveAudioStream, "cell 0 6,alignx right,aligny top");
        }
        {
          btnAddSubtitle = new SquareIconButton(new AddSubtitleAction());
          panelDetails.add(btnAddSubtitle, "cell 0 7,alignx right,aligny top");
        }
        {
          btnRemoveSubtitle = new SquareIconButton(new RemoveSubtitleAction());
          panelDetails.add(btnRemoveSubtitle, "cell 0 7,alignx right,aligny top");
        }
      }
    }

    initDataBindings();

    // select first
    if (!this.mediaFiles.isEmpty()) {
      tableMediaFiles.getSelectionModel().setSelectionInterval(0, 0);
    }

    // add selection listener to disable editing when needed
    tableMediaFiles.getSelectionModel().addListSelectionListener(listener -> {
      if (!listener.getValueIsAdjusting()) {
        int selectedRow = tableMediaFiles.convertRowIndexToModel(tableMediaFiles.getSelectedRow());
        if (selectedRow > -1) {
          MediaFile mf = MediaFileEditorPanel.this.mediaFiles.get(selectedRow).mediaFile;
          // codec should not be enabled for NFOs
          tfCodec.setEnabled(mf.getType() != NFO);
          // audio streams and subtitles should not be enabled for anything except VIDEOS/TRAILER/SAMPLES
          btnAddAudioStream.setEnabled(videoTypes.contains(mf.getType()));
          btnRemoveAudioStream.setEnabled(videoTypes.contains(mf.getType()));
          btnAddSubtitle.setEnabled(videoTypes.contains(mf.getType()));
          btnRemoveSubtitle.setEnabled(videoTypes.contains(mf.getType()));
          // 3D is only available for video types
          cb3dFormat.setEnabled(videoTypes.contains(mf.getType()));
          // runtime is also only available for video types
          tfRuntime.setEnabled(videoTypes.contains(mf.getType()));
          btnARD.setEnabled(videoTypes.contains(mf.getType()) && FFmpeg.isAvailable());
        }
      }
    });

    // adjust columnn titles - we have to do it this way - thx to windowbuilder pro
    tableAudioStreams.getColumnModel().getColumn(0).setHeaderValue(TmmResourceBundle.getString("metatag.language"));
    tableAudioStreams.getColumnModel().getColumn(1).setHeaderValue(TmmResourceBundle.getString("metatag.codec"));
    tableAudioStreams.getColumnModel().getColumn(2).setHeaderValue(TmmResourceBundle.getString("metatag.channels"));
    tableAudioStreams.getColumnModel().getColumn(3).setHeaderValue(TmmResourceBundle.getString("metatag.bitrate"));
    tableAudioStreams.getColumnModel().getColumn(4).setHeaderValue(TmmResourceBundle.getString("metatag.title"));

    tableSubtitles.getColumnModel().getColumn(0).setHeaderValue(TmmResourceBundle.getString("metatag.language"));
    tableSubtitles.getColumnModel().getColumn(1).setHeaderValue(TmmResourceBundle.getString("metatag.forced"));
    tableSubtitles.getColumnModel().getColumn(2).setHeaderValue(TmmResourceBundle.getString("metatag.sdh"));
    tableSubtitles.getColumnModel().getColumn(3).setHeaderValue(TmmResourceBundle.getString("metatag.title"));
  }

  private List<AspectRatioContainer> getAspectRatios() {
    if (aspectRatios.isEmpty()) {
      aspectRatios.add(new AspectRatioContainer(0f, TmmResourceBundle.getString("aspectratio.calculated")));
      for (Float ar : AspectRatio.getDefaultValues()) {
        aspectRatios.add(new AspectRatioContainer(ar));
      }
    }
    return aspectRatios;
  }

  private List<AspectRatioContainer> getAspectRatios2() {
    if (aspectRatios2.isEmpty()) {
      aspectRatios2.add(new AspectRatioContainer(null, TmmResourceBundle.getString("aspectratio.nomultiformat")));
      for (Float ar : AspectRatio.getDefaultValues()) {
        aspectRatios2.add(new AspectRatioContainer(ar));
      }
    }
    return aspectRatios2;
  }

  private class AddAudioStreamAction extends AbstractAction {
    private static final long serialVersionUID = 2903255414523349267L;

    public AddAudioStreamAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("audiostream.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int mediaFileRow = tableMediaFiles.getSelectedRow();
      if (mediaFileRow > -1) {
        mediaFileRow = tableMediaFiles.convertRowIndexToModel(mediaFileRow);
        MediaFileContainer mf = mediaFiles.get(mediaFileRow);
        mf.addAudioStream();
      }
    }
  }

  private class RemoveAudioStreamAction extends AbstractAction {
    private static final long serialVersionUID = -7079826940827356996L;

    public RemoveAudioStreamAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("audiostream.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int[] audioRows = convertSelectedRowsToModelRows(tableAudioStreams);
      if (audioRows.length > 0) {
        int mediaFileRow = tableMediaFiles.getSelectedRow();
        if (mediaFileRow > -1) {
          mediaFileRow = tableMediaFiles.convertRowIndexToModel(mediaFileRow);
          MediaFileContainer mf = mediaFiles.get(mediaFileRow);

          for (int row : audioRows) {
            mf.removeAudioStream(row);
          }
        }
      }
    }
  }

  private class AddSubtitleAction extends AbstractAction {
    private static final long serialVersionUID = 2903255414523349767L;

    public AddSubtitleAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("subtitle.add"));
      putValue(SMALL_ICON, IconManager.ADD_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int mediaFileRow = tableMediaFiles.getSelectedRow();
      if (mediaFileRow > -1) {
        mediaFileRow = tableMediaFiles.convertRowIndexToModel(mediaFileRow);
        MediaFileContainer mf = mediaFiles.get(mediaFileRow);
        mf.addSubtitle();
      }
    }
  }

  private class RemoveSubtitleAction extends AbstractAction {
    private static final long serialVersionUID = -7079866940827356996L;

    public RemoveSubtitleAction() {
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("subtitle.remove"));
      putValue(SMALL_ICON, IconManager.REMOVE_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int[] subtitleRows = convertSelectedRowsToModelRows(tableSubtitles);
      if (subtitleRows.length > 0) {
        int mediaFileRow = tableMediaFiles.getSelectedRow();
        if (mediaFileRow > -1) {
          mediaFileRow = tableMediaFiles.convertRowIndexToModel(mediaFileRow);
          MediaFileContainer mf = mediaFiles.get(mediaFileRow);

          for (int row : subtitleRows) {
            mf.removeSubtitle(row);
          }
        }
      }
    }
  }

  private class ScanAspectRationAction extends AbstractAction implements TmmTaskListener {
    private static final long serialVersionUID = 8777310652284455423L;

    public ScanAspectRationAction() {
      putValue(NAME, TmmResourceBundle.getString("task.ard"));
      putValue(SMALL_ICON, IconManager.ASPECT_RATIO);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int mediaFileRow = tableMediaFiles.getSelectedRow();
      if (mediaFileRow > -1) {
        mediaFileRow = tableMediaFiles.convertRowIndexToModel(mediaFileRow);
        MediaFileContainer mf = mediaFiles.get(mediaFileRow);

        ARDetectorTask task = new MediaFileARDetectorTask(mf.getMediaFile());
        task.addListener(this);
        TmmTaskManager.getInstance().addUnnamedTask(task);
        MediaFileEditorPanel.this.ardTask = task;
      }
    }

    @Override
    public void processTaskEvent(TmmTaskHandle task) {
      if (TmmTaskHandle.TaskState.QUEUED.equals(task.getState())) {
        btnARD.setEnabled(false);
      }
      else if (TmmTaskHandle.TaskState.FINISHED.equals(task.getState())) {
        btnARD.setEnabled(true);
      }
    }
  }

  private int[] convertSelectedRowsToModelRows(JTable table) {
    int[] tableRows = table.getSelectedRows();
    int[] modelRows = new int[tableRows.length];
    for (int i = 0; i < tableRows.length; i++) {
      modelRows[i] = table.convertRowIndexToModel(tableRows[i]);
    }

    // sort it (descending)
    ArrayUtils.reverse(modelRows);
    return modelRows;
  }

  /*
   * Container needed to make the audio streams and subtitles editable
   */
  public static class MediaFileContainer {
    private final MediaFile                  mediaFile;
    private final List<MediaFileAudioStream> audioStreams;
    private final List<MediaFileSubtitle>    subtitles;

    private MediaFileContainer(MediaFile mediaFile) {
      this.mediaFile = mediaFile;
      this.audioStreams = ObservableCollections.observableList(new ArrayList<>(mediaFile.getAudioStreams()));
      this.subtitles = ObservableCollections.observableList(new ArrayList<>(mediaFile.getSubtitles()));
    }

    public MediaFile getMediaFile() {
      return mediaFile;
    }

    public List<MediaFileAudioStream> getAudioStreams() {
      return audioStreams;
    }

    public List<MediaFileSubtitle> getSubtitles() {
      return subtitles;
    }

    public void addAudioStream() {
      audioStreams.add(new MediaFileAudioStream());
      mediaFile.setAudioStreams(audioStreams);
    }

    public void removeAudioStream(int index) {
      audioStreams.remove(index);
      mediaFile.setAudioStreams(audioStreams);
    }

    public void addSubtitle() {
      subtitles.add(new MediaFileSubtitle());
      mediaFile.setSubtitles(subtitles);
    }

    public void removeSubtitle(int index) {
      subtitles.remove(index);
      mediaFile.setSubtitles(subtitles);
    }
  }

  /**
   * Sync media files edited from this editor with the ones from the media entity without removing/adding all of them
   * 
   * @param mfsFromEditor
   *          the edited media files
   * @param mfsFromMediaEntity
   *          the original media files
   */
  public static void syncMediaFiles(List<MediaFile> mfsFromEditor, List<MediaFile> mfsFromMediaEntity) {
    for (MediaFile mfEditor : mfsFromEditor) {
      for (MediaFile mfOriginal : mfsFromMediaEntity) {
        if (mfEditor.equals(mfOriginal)) {
          // here we check all field which can be edited from the editor
          if (!mfEditor.getVideoCodec().equals(mfOriginal.getVideoCodec())) {
            mfOriginal.setVideoCodec(mfEditor.getVideoCodec());
          }
          if (!mfEditor.getContainerFormat().equals(mfOriginal.getContainerFormat())) {
            mfOriginal.setContainerFormat(mfEditor.getContainerFormat());
          }
          if (mfEditor.getVideoWidth() != mfOriginal.getVideoWidth()) {
            mfOriginal.setVideoWidth(mfEditor.getVideoWidth());
          }
          if (mfEditor.getVideoHeight() != mfOriginal.getVideoHeight()) {
            mfOriginal.setVideoHeight(mfEditor.getVideoHeight());
          }
          if (MetadataUtil.unboxFloat(mfEditor.getAspectRatio()) != MetadataUtil.unboxFloat(mfOriginal.getAspectRatio())) {
            mfOriginal.setAspectRatio(mfEditor.getAspectRatio());
          }
          if (MetadataUtil.unboxFloat(mfEditor.getAspectRatio2()) != MetadataUtil.unboxFloat(mfOriginal.getAspectRatio2())) {
            mfOriginal.setAspectRatio2(mfEditor.getAspectRatio2());
          }
          if (mfEditor.getFrameRate() != mfOriginal.getFrameRate()) {
            mfOriginal.setFrameRate(mfEditor.getFrameRate());
          }
          if (!mfEditor.getVideo3DFormat().equals(mfOriginal.getVideo3DFormat())) {
            mfOriginal.setVideo3DFormat(mfEditor.getVideo3DFormat());
          }
          if (!mfEditor.getHdrFormat().equals(mfOriginal.getHdrFormat())) {
            mfOriginal.setHdrFormat(mfEditor.getHdrFormat());
          }
          if (mfEditor.getBitDepth() != mfOriginal.getBitDepth()) {
            mfOriginal.setBitDepth(mfEditor.getBitDepth());
          }
          if (mfEditor.getOverallBitRate() != mfOriginal.getOverallBitRate()) {
            mfOriginal.setOverallBitRate(mfEditor.getOverallBitRate());
          }
          if (mfEditor.getDuration() != mfOriginal.getDuration()) {
            mfOriginal.setDuration(mfEditor.getDuration());
          }

          // audio streams and subtitles will be completely set (except empty ones)
          List<MediaFileAudioStream> audioStreams = new ArrayList<>();
          for (MediaFileAudioStream audioStream : mfEditor.getAudioStreams()) {
            if (StringUtils.isBlank(audioStream.getCodec()) || audioStream.getAudioChannels() == 0) {
              continue;
            }
            audioStreams.add(audioStream);
          }
          mfOriginal.setAudioStreams(audioStreams);

          List<MediaFileSubtitle> subtitles = new ArrayList<>();
          for (MediaFileSubtitle subtitle : mfEditor.getSubtitles()) {
            if (StringUtils.isBlank(subtitle.getLanguage())) {
              continue;
            }
            subtitles.add(subtitle);
          }
          mfOriginal.setSubtitles(subtitles);

          break;
        }
      }
    }
  }

  public void unbindBindings() {
    try {
      bindingGroup.unbind();
    }
    catch (Exception ignored) {
      // just not crash
    }
  }

  protected void initDataBindings() {
    JTableBinding jTableBinding = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, mediaFiles, tableMediaFiles);
    //
    Property mediaFileContainerBeanProperty = BeanProperty.create("mediaFile.filename");
    jTableBinding.addColumnBinding(mediaFileContainerBeanProperty).setColumnName("Filename").setEditable(false);
    //
    jTableBinding.setEditable(false);
    jTableBinding.bind();
    //
    Property jTableBeanProperty = BeanProperty.create("selectedElement.mediaFile.filename");
    Property jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty, lblFilename,
        jLabelBeanProperty);
    autoBinding.bind();
    //
    Property jTableBeanProperty_1 = BeanProperty.create("selectedElement.mediaFile.videoCodec");
    Property jTextFieldBeanProperty = BeanProperty.create("text");
    AutoBinding autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_1, tfCodec,
        jTextFieldBeanProperty);
    autoBinding_1.bind();
    //
    Property jTableBeanProperty_3 = BeanProperty.create("selectedElement.mediaFile.containerFormat");
    Property jTextFieldBeanProperty_2 = BeanProperty.create("text");
    AutoBinding autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_3, tfContainerFormat,
        jTextFieldBeanProperty_2);
    autoBinding_3.bind();
    //
    Property jTableBeanProperty_5 = BeanProperty.create("selectedElement.mediaFile.videoWidth");
    Property jTextFieldBeanProperty_4 = BeanProperty.create("text");
    AutoBinding autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_5, tfWidth,
        jTextFieldBeanProperty_4);
    autoBinding_5.bind();
    //
    Property jTableBeanProperty_6 = BeanProperty.create("selectedElement.mediaFile.videoHeight");
    Property jTextFieldBeanProperty_5 = BeanProperty.create("text");
    AutoBinding autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_6, tfHeight,
        jTextFieldBeanProperty_5);
    autoBinding_6.bind();
    //
    Property jTableBeanProperty_2 = BeanProperty.create("selectedElement.audioStreams");
    JTableBinding jTableBinding_1 = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_2,
        tableAudioStreams);
    //
    Property mediaFileAudioStreamBeanProperty = BeanProperty.create("language");
    jTableBinding_1.addColumnBinding(mediaFileAudioStreamBeanProperty).setColumnName("Language").setColumnClass(String.class);
    //
    Property mediaFileAudioStreamBeanProperty_1 = BeanProperty.create("codec");
    jTableBinding_1.addColumnBinding(mediaFileAudioStreamBeanProperty_1).setColumnName("Codec");
    //
    Property mediaFileAudioStreamBeanProperty_2 = BeanProperty.create("audioChannels");
    jTableBinding_1.addColumnBinding(mediaFileAudioStreamBeanProperty_2).setColumnName("Channels");
    //
    Property mediaFileAudioStreamBeanProperty_3 = BeanProperty.create("bitrate");
    jTableBinding_1.addColumnBinding(mediaFileAudioStreamBeanProperty_3).setColumnName("Bitrate").setColumnClass(Integer.class);
    //
    Property mediaFileAudioStreamBeanProperty_4 = BeanProperty.create("audioTitle");
    jTableBinding_1.addColumnBinding(mediaFileAudioStreamBeanProperty_4).setColumnName("Audio Title").setColumnClass(String.class);

    jTableBinding_1.bind();
    //
    Property jTableBeanProperty_4 = BeanProperty.create("selectedElement.subtitles");
    JTableBinding jTableBinding_2 = SwingBindings.createJTableBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_4,
        tableSubtitles);
    //
    Property mediaFileSubtitleBeanProperty = BeanProperty.create("language");
    jTableBinding_2.addColumnBinding(mediaFileSubtitleBeanProperty).setColumnName("Language").setColumnClass(String.class);
    //
    Property mediaFileSubtitleBeanProperty_1 = BeanProperty.create("forced");
    jTableBinding_2.addColumnBinding(mediaFileSubtitleBeanProperty_1).setColumnName("Forced").setColumnClass(Boolean.class);
    //
    Property mediaFileSubtitleBeanProperty_2 = BeanProperty.create("sdh");
    jTableBinding_2.addColumnBinding(mediaFileSubtitleBeanProperty_2).setColumnName("Hearing Impaired").setColumnClass(Boolean.class);
    //
    Property mediaFileSubtitleBeanProperty_3 = BeanProperty.create("title");
    jTableBinding_2.addColumnBinding(mediaFileSubtitleBeanProperty_3).setColumnName("Title").setColumnClass(String.class);
    //
    jTableBinding_2.bind();
    //
    Property jTableBeanProperty_7 = BeanProperty.create("selectedElement.mediaFile.video3DFormat");
    Property jComboBoxBeanProperty = BeanProperty.create("selectedItem");
    AutoBinding autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, jTableBeanProperty_7, cb3dFormat,
        jComboBoxBeanProperty);
    autoBinding_2.bind();
    //
    Property tmmTableBeanProperty = BeanProperty.create("selectedElement.mediaFile.aspectRatio");
    AutoBinding autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty, cbAspectRatio,
        jComboBoxBeanProperty);
    autoBinding_4.setConverter(new AspectRatioConverter(getAspectRatios()));
    autoBinding_4.bind();
    //
    Property tmmTableBeanProperty_6 = BeanProperty.create("selectedElement.mediaFile.aspectRatio2");
    AutoBinding autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_6, cbAspectRatio2,
        jComboBoxBeanProperty);
    autoBinding_12.setConverter(new AspectRatioConverter(getAspectRatios2()));
    autoBinding_12.bind();
    //
    Property tmmTableBeanProperty_1 = BeanProperty.create("selectedElement.mediaFile.frameRate");
    Property jSpinnerBeanProperty = BeanProperty.create("value");
    AutoBinding autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_1, spFrameRate,
        jSpinnerBeanProperty);
    autoBinding_7.bind();
    //
    Property tmmTableBeanProperty_2 = BeanProperty.create("selectedElement.mediaFile.bitDepth");
    Property jTextFieldBeanProperty_1 = BeanProperty.create("text");
    AutoBinding autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_2, tfBitDepth,
        jTextFieldBeanProperty_1);
    autoBinding_8.bind();
    //
    Property tmmTableBeanProperty_3 = BeanProperty.create("selectedElement.mediaFile.overallBitRate");
    Property jTextFieldBeanProperty_3 = BeanProperty.create("text");
    AutoBinding autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_3, tfVideoBitrate,
        jTextFieldBeanProperty_3);
    autoBinding_9.bind();
    //
    Property tmmTableBeanProperty_4 = BeanProperty.create("selectedElement.mediaFile.hdrFormat");
    Property jTextFieldBeanProperty_6 = BeanProperty.create("text");
    AutoBinding autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_4, tfHdrFormat,
        jTextFieldBeanProperty_6);
    autoBinding_10.bind();
    //
    Property tmmTableBeanProperty_5 = BeanProperty.create("selectedElement.mediaFile.durationInMinutes");
    Property jTextFieldBeanProperty_7 = BeanProperty.create("text");
    AutoBinding autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, tableMediaFiles, tmmTableBeanProperty_5, tfRuntime,
        jTextFieldBeanProperty_7);
    autoBinding_11.bind();
    //
    bindingGroup.addBinding(jTableBinding);
    bindingGroup.addBinding(jTableBinding_1);
    bindingGroup.addBinding(jTableBinding_2);
  }

  public void cancelTask() {
    if (this.ardTask != null) {
      this.ardTask.cancel();
      this.ardTask = null;
    }
  }

  private static class AspectRatioContainer {
    private final Float  aspectRatio;
    private final String customText;

    private AspectRatioContainer(Float aspectRatio) {
      this(aspectRatio, null);
    }

    private AspectRatioContainer(Float aspectRatio, String customText) {
      this.aspectRatio = aspectRatio;
      this.customText = customText;
    }

    private Float getAspectRatio() {
      return aspectRatio;
    }

    @Override
    public String toString() {
      if (StringUtils.isNotBlank(customText)) {
        if (aspectRatio != null && aspectRatio > 0) {
          return customText + String.format(" (%.2f:1)", aspectRatio);
        }
        else {
          return customText;
        }
      }
      return AspectRatio.getDescription(aspectRatio);
    }
  }

  public static class AspectRatioConverter extends Converter<Float, AspectRatioContainer> {

    private final List<AspectRatioContainer> values;

    public AspectRatioConverter(List<AspectRatioContainer> values) {
      this.values = values;
    }

    @Override
    public AspectRatioContainer convertForward(Float value) {
      return values.stream().filter(entry -> Objects.equals(entry.aspectRatio, value)).findFirst().orElse(null);
    }

    @Override
    public Float convertReverse(AspectRatioContainer value) {
      return value.aspectRatio;
    }
  }
}
