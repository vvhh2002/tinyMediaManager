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
package org.tinymediamanager.ui.tvshows.dialogs;

import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_BANNER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_FANART;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_POSTER;
import static org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType.SEASON_THUMB;
import static org.tinymediamanager.ui.TmmUIHelper.createLinkForImage;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowHelpers;
import org.tinymediamanager.core.tvshow.TvShowList;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.ShadowLayerUI;
import org.tinymediamanager.ui.components.FlatButton;
import org.tinymediamanager.ui.components.ImageLabel;
import org.tinymediamanager.ui.components.LinkLabel;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.TmmTabbedPane;
import org.tinymediamanager.ui.dialogs.ImageChooserDialog;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowSeasonEditor.
 * 
 * @author Manuel Laggner
 */
public class TvShowSeasonEditorDialog extends TmmDialog {
  private static final long   serialVersionUID    = 3270218410302989845L;
  private static final String ORIGINAL_IMAGE_SIZE = "originalImageSize";
  private static final String SPACER              = "        ";

  private final TvShowSeason  tvShowSeasonToEdit;
  private final TvShowList    tvShowList          = TvShowModuleManager.getInstance().getTvShowList();
  private final JTabbedPane   tabbedPane          = new TmmTabbedPane();

  private boolean             continueQueue       = true;
  private boolean             navigateBack        = false;
  private final int           queueIndex;
  private final int           queueSize;

  /**
   * UI elements
   */
  private ImageLabel          lblPoster;
  private ImageLabel          lblFanart;
  private ImageLabel          lblBanner;
  private ImageLabel          lblThumb;

  private JTextField          tfPoster;
  private JTextField          tfFanart;
  private JTextField          tfBanner;
  private JTextField          tfThumb;
  private JTextField          tfTitle;

  /**
   * Instantiates a new tv show season editor dialog.
   *
   * @param tvShowSeason
   *          the tv show season
   * @param queueIndex
   *          the actual index in the queue
   * @param queueSize
   *          the queue size
   */
  public TvShowSeasonEditorDialog(TvShowSeason tvShowSeason, int queueIndex, int queueSize, int selectedTab) {
    super(TmmResourceBundle.getString("tvshowseason.edit") + (queueSize > 1 ? " " + (queueIndex + 1) + "/" + queueSize : ""), "tvShowSeasonEditor");

    this.tvShowSeasonToEdit = tvShowSeason;
    this.queueIndex = queueIndex;
    this.queueSize = queueSize;

    initComponents();

    {
      tfTitle.setText(tvShowSeason.getTitle());
      lblPoster.setImagePath(tvShowSeason.getArtworkFilename(SEASON_POSTER));
      lblFanart.setImagePath(tvShowSeason.getArtworkFilename(SEASON_FANART));
      lblThumb.setImagePath(tvShowSeason.getArtworkFilename(SEASON_THUMB));
      lblBanner.setImagePath(tvShowSeason.getArtworkFilename(SEASON_BANNER));

      tfPoster.setText(tvShowSeason.getArtworkUrl(SEASON_POSTER));
      tfFanart.setText(tvShowSeason.getArtworkUrl(SEASON_FANART));
      tfThumb.setText(tvShowSeason.getArtworkUrl(SEASON_THUMB));
      tfBanner.setText(tvShowSeason.getArtworkUrl(SEASON_BANNER));
    }

    tabbedPane.setSelectedIndex(selectedTab);
  }

  /**
   * Returns the tab number
   * 
   * @return 0-X
   */
  public int getSelectedTab() {
    return tabbedPane.getSelectedIndex();
  }

  private void initComponents() {
    // to draw the shadow beneath window frame, encapsulate the panel
    JLayer<JComponent> rootLayer = new JLayer(tabbedPane, new ShadowLayerUI()); // removed <> because this leads WBP to crash
    getContentPane().add(rootLayer, BorderLayout.CENTER);

    /**********************************************************************************
     * local artwork pane
     **********************************************************************************/
    {
      JPanel artworkPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("metatag.details"), null, artworkPanel, null);
      artworkPanel.setLayout(new MigLayout("", "[150lp:200lp,grow][20lp:n][300lp:400lp,grow][20lp:n][300lp:400lp,grow]",
          "[][][100lp:125lp,grow][20lp:n][][100lp:125lp,grow][]"));
      {
        JLabel lblTitleT = new TmmLabel(TmmResourceBundle.getString("metatag.title"));
        artworkPanel.add(lblTitleT, "flowx,cell 0 0 5 1");

        tfTitle = new JTextField();
        tfTitle.setColumns(10);
        artworkPanel.add(tfTitle, "cell 0 0 5 1,growx");
      }
      {
        JLabel lblPosterT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster"));
        artworkPanel.add(lblPosterT, "cell 0 1");

        LinkLabel lblPosterSize = new LinkLabel();
        artworkPanel.add(lblPosterSize, "cell 0 1");

        JButton btnDeletePoster = new FlatButton(SPACER, IconManager.DELETE_GRAY);
        btnDeletePoster.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeletePoster.addActionListener(e -> {
          lblPoster.clearImage();
          tfPoster.setText("");
        });
        artworkPanel.add(btnDeletePoster, "cell 0 1");

        lblPoster = new ImageLabel();
        lblPoster.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblPoster.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            openImageChooser(lblPoster, tfPoster, SEASON_POSTER);
          }
        });

        artworkPanel.add(lblPoster, "cell 0 2,grow");
        lblPoster.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblPosterSize, lblPoster, MediaArtwork.MediaArtworkType.SEASON_POSTER));
      }
      {
        JLabel lblFanartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart"));
        artworkPanel.add(lblFanartT, "cell 2 1");

        LinkLabel lblFanartSize = new LinkLabel();
        artworkPanel.add(lblFanartSize, "cell 2 1");

        JButton btnDeleteFanart = new FlatButton(SPACER, IconManager.DELETE_GRAY);
        btnDeleteFanart.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteFanart.addActionListener(e -> {
          lblFanart.clearImage();
          tfFanart.setText("");
        });
        artworkPanel.add(btnDeleteFanart, "cell 2 1");

        lblFanart = new ImageLabel();
        lblFanart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblFanart.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            openImageChooser(lblFanart, tfFanart, SEASON_FANART);
          }
        });

        artworkPanel.add(lblFanart, "cell 2 2,grow");
        lblFanart.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblFanartSize, lblFanart, MediaArtwork.MediaArtworkType.SEASON_FANART));
      }
      {
        JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
        artworkPanel.add(lblThumbT, "cell 4 1");

        LinkLabel lblThumbSize = new LinkLabel();
        artworkPanel.add(lblThumbSize, "cell 4 1");

        JButton btnDeleteThumb = new FlatButton(SPACER, IconManager.DELETE_GRAY);
        btnDeleteThumb.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteThumb.addActionListener(e -> {
          lblThumb.clearImage();
          tfThumb.setText("");
        });
        artworkPanel.add(btnDeleteThumb, "cell 4 1");

        lblThumb = new ImageLabel();
        lblThumb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblThumb.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            openImageChooser(lblThumb, tfThumb, SEASON_THUMB);
          }
        });

        artworkPanel.add(lblThumb, "cell 4 2,grow");
        lblThumb.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblThumbSize, lblThumb, MediaArtwork.MediaArtworkType.SEASON_THUMB));
      }
      {
        JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
        artworkPanel.add(lblBannerT, "cell 0 4");

        LinkLabel lblBannerSize = new LinkLabel();
        artworkPanel.add(lblBannerSize, "cell 0 4");

        JButton btnDeleteBanner = new FlatButton(SPACER, IconManager.DELETE_GRAY);
        btnDeleteBanner.setToolTipText(TmmResourceBundle.getString("Button.deleteartwork.desc"));
        btnDeleteBanner.addActionListener(e -> {
          lblBanner.clearImage();
          tfBanner.setText("");
        });
        artworkPanel.add(btnDeleteBanner, "cell 0 4");

        lblBanner = new ImageLabel();
        lblBanner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblBanner.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            openImageChooser(lblBanner, tfBanner, SEASON_BANNER);
          }
        });
        artworkPanel.add(lblBanner, "cell 0 5 5 1,grow");

        lblBanner.addPropertyChangeListener(ORIGINAL_IMAGE_SIZE,
            e -> setImageSizeAndCreateLink(lblBannerSize, lblBanner, MediaArtwork.MediaArtworkType.SEASON_BANNER));
      }
    }

    /**********************************************************************************
     * artwork urls
     **********************************************************************************/
    {
      JPanel artworkPanel = new JPanel();
      tabbedPane.addTab(TmmResourceBundle.getString("edit.artwork"), null, artworkPanel, null);
      artworkPanel.setLayout(new MigLayout("", "[][grow]", "[][][][]"));
      {
        JLabel lblPosterT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.poster"));
        artworkPanel.add(lblPosterT, "cell 0 0,alignx right");

        tfPoster = new JTextField();
        artworkPanel.add(tfPoster, "cell 1 0,growx");
      }
      {
        JLabel lblFanartT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.fanart"));
        artworkPanel.add(lblFanartT, "cell 0 1,alignx right");

        tfFanart = new JTextField();
        artworkPanel.add(tfFanart, "cell 1 1,growx");
      }
      {
        JLabel lblBannerT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.banner"));
        artworkPanel.add(lblBannerT, "cell 0 2,alignx right");

        tfBanner = new JTextField();
        artworkPanel.add(tfBanner, "cell 1 2,growx");
      }
      {
        JLabel lblThumbT = new TmmLabel(TmmResourceBundle.getString("mediafiletype.thumb"));
        artworkPanel.add(lblThumbT, "cell 0 3,alignx right");

        tfThumb = new JTextField();
        artworkPanel.add(tfThumb, "cell 1 3,growx");
      }
    }

    /**********************************************************************************
     * button pane
     **********************************************************************************/
    {
      if (queueSize > 1) {
        JButton btnAbort = new JButton(new AbortAction());
        addButton(btnAbort);
        if (queueIndex > 0) {
          JButton backButton = new JButton(new NavigateBackAction());
          addButton(backButton);
        }
      }

      JButton cancelButton = new JButton(new CancelAction());
      addButton(cancelButton);

      JButton okButton = new JButton(new OKAction());
      addDefaultButton(okButton);
    }
  }

  private void updateArtworkUrl(ImageLabel imageLabel, JTextField textField) {
    if (StringUtils.isNotBlank(imageLabel.getImageUrl())) {
      textField.setText(imageLabel.getImageUrl());
    }
  }

  private void openImageChooser(ImageLabel label, JTextField textField, MediaArtwork.MediaArtworkType artworkType) {
    Map<String, Object> ids = new HashMap<>(tvShowSeasonToEdit.getTvShow().getIds());
    ids.put("tvShowSeason", tvShowSeasonToEdit.getSeason());
    ImageChooserDialog dialog = new ImageChooserDialog(TvShowSeasonEditorDialog.this, ids, artworkType, tvShowList.getDefaultArtworkScrapers(), label,
        MediaType.TV_SHOW);

    if (Settings.getInstance().isImageChooserUseEntityFolder()) {
      TvShow tvShow = tvShowSeasonToEdit.getTvShow();
      Path seasonPath = tvShow.getPathNIO().resolve(TvShowHelpers.detectSeasonFolder(tvShow, tvShowSeasonToEdit.getSeason())).toAbsolutePath();
      if (!Files.exists(seasonPath)) {
        seasonPath = tvShow.getPathNIO().toAbsolutePath();
      }
      dialog.setOpenFolderPath(seasonPath.toString());
    }

    dialog.setLocationRelativeTo(MainWindow.getInstance());
    dialog.setVisible(true);
    updateArtworkUrl(label, textField);
  }

  private void setImageSizeAndCreateLink(LinkLabel lblSize, ImageLabel imageLabel, MediaArtwork.MediaArtworkType type) {
    createLinkForImage(lblSize, imageLabel);
    // image has been deleted
    if (imageLabel.getOriginalImageSize().width == 0 && imageLabel.getOriginalImageSize().height == 0) {
      lblSize.setText("");
      return;
    }

    Dimension dimension = tvShowSeasonToEdit.getArtworkSize(type);
    if (dimension.width == 0 && dimension.height == 0) {
      lblSize.setText(imageLabel.getOriginalImageSize().width + "x" + imageLabel.getOriginalImageSize().height);
    }
    else {
      lblSize.setText(dimension.width + "x" + dimension.height);
    }
  }

  private void processArtwork(MediaFileType type, ImageLabel imageLabel, JTextField textField) {
    MediaArtwork.MediaArtworkType artworkType = MediaFileType.getMediaArtworkType(type);
    if (StringUtils.isAllBlank(imageLabel.getImagePath(), imageLabel.getImageUrl())
        && StringUtils.isNotBlank(tvShowSeasonToEdit.getArtworkFilename(artworkType))) {
      // artwork has been explicitly deleted
      tvShowSeasonToEdit.deleteArtworkFiles(artworkType);
    }

    if (StringUtils.isNotEmpty(textField.getText()) && !textField.getText().equals(tvShowSeasonToEdit.getArtworkUrl(artworkType))) {
      // artwork url and textfield do not match -> redownload
      tvShowSeasonToEdit.setArtworkUrl(textField.getText(), artworkType);
      tvShowSeasonToEdit.downloadArtwork(artworkType);
    }
    else if (StringUtils.isEmpty(textField.getText())) {
      // remove the artwork url
      tvShowSeasonToEdit.removeArtworkUrl(artworkType);
    }
    else {
      // they match, but check if there is a need to download the artwork
      if (StringUtils.isBlank(tvShowSeasonToEdit.getArtworkFilename(artworkType))) {
        tvShowSeasonToEdit.downloadArtwork(artworkType);
      }
    }
  }

  private class OKAction extends AbstractAction {
    private static final long serialVersionUID = 6699599213348390696L;

    OKAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.ok"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.change"));
      putValue(SMALL_ICON, IconManager.APPLY_INV);
      putValue(LARGE_ICON_KEY, IconManager.APPLY_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      tvShowSeasonToEdit.setTitle(tfTitle.getText());

      // process artwork
      processArtwork(MediaFileType.SEASON_POSTER, lblPoster, tfPoster);
      processArtwork(MediaFileType.SEASON_FANART, lblFanart, tfFanart);
      processArtwork(MediaFileType.SEASON_BANNER, lblBanner, tfBanner);
      processArtwork(MediaFileType.SEASON_THUMB, lblThumb, tfThumb);

      tvShowSeasonToEdit.getTvShow().writeNFO();
      tvShowSeasonToEdit.getTvShow().saveToDb();

      setVisible(false);
    }
  }

  private class CancelAction extends AbstractAction {
    private static final long serialVersionUID = -4617793684152607277L;

    CancelAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.cancel"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("edit.discard"));
      putValue(SMALL_ICON, IconManager.CANCEL_INV);
      putValue(LARGE_ICON_KEY, IconManager.CANCEL_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      setVisible(false);
    }
  }

  private class NavigateBackAction extends AbstractAction {
    private static final long serialVersionUID = -1652218154720642310L;

    public NavigateBackAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.back"));
      putValue(SMALL_ICON, IconManager.BACK_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      navigateBack = true;
      setVisible(false);
    }
  }

  /**
   * Shows the dialog and returns whether the work on the queue should be continued.
   * 
   * @return true, if successful
   */
  public boolean showDialog() {
    setVisible(true);
    return continueQueue;
  }

  public boolean isContinueQueue() {
    return continueQueue;
  }

  public boolean isNavigateBack() {
    return navigateBack;
  }

  private class AbortAction extends AbstractAction {
    private static final long serialVersionUID = -7652218354710642510L;

    AbortAction() {
      putValue(NAME, TmmResourceBundle.getString("Button.abortqueue"));
      putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.edit.abortqueue.desc"));
      putValue(SMALL_ICON, IconManager.STOP_INV);
      putValue(LARGE_ICON_KEY, IconManager.STOP_INV);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      continueQueue = false;
      setVisible(false);
    }
  }
}
