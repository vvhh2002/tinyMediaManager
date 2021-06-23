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
package org.tinymediamanager.ui.tvshows;

import java.awt.CardLayout;

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.Globals;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.core.tvshow.entities.TvShowSeason;
import org.tinymediamanager.license.License;
import org.tinymediamanager.thirdparty.KodiRPC;
import org.tinymediamanager.ui.AbstractTmmUIModule;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.MainTabbedPane;
import org.tinymediamanager.ui.components.PopupMenuScroller;
import org.tinymediamanager.ui.movies.panels.TrailerPanel;
import org.tinymediamanager.ui.settings.TmmSettingsNode;
import org.tinymediamanager.ui.thirdparty.KodiRPCMenu;
import org.tinymediamanager.ui.tvshows.actions.DebugDumpShowAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowAddDatasourceAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowAspectRatioDetectAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowBulkEditAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowChangeDatasourceAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowChangeSeasonArtworkAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowChangeToAiredOrderAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowChangeToDvdOrderAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowCleanUpFilesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowClearImageCacheAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowDeleteAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowDeleteMediainfoXmlAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowDownloadActorImagesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowDownloadMissingArtworkAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowDownloadThemeAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowEditAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowExportAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowFetchImdbRatingAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowMediaInformationAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowMissingEpisodeListAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowReadEpisodeNfoAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowReadNfoAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRebuildImageCacheAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRebuildMediainfoXmlAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRemoveAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRenameAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRenamePreviewAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRewriteEpisodeNfoAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowRewriteNfoAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowScrapeEpisodesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowScrapeMissingEpisodesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowScrapeNewItemsAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSelectedScrapeAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSingleScrapeAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSubtitleDownloadAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSubtitleSearchAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncSelectedCollectionTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncSelectedRatingTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncSelectedTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncSelectedWatchedTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowSyncTraktTvAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowToggleWatchedFlagAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowUpdateAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowUpdateDatasourcesAction;
import org.tinymediamanager.ui.tvshows.actions.TvShowUpdateSingleDatasourceAction;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowFilterDialog;
import org.tinymediamanager.ui.tvshows.panels.TvShowTreePanel;
import org.tinymediamanager.ui.tvshows.panels.episode.TvShowEpisodeCastPanel;
import org.tinymediamanager.ui.tvshows.panels.episode.TvShowEpisodeInformationPanel;
import org.tinymediamanager.ui.tvshows.panels.episode.TvShowEpisodeMediaInformationPanel;
import org.tinymediamanager.ui.tvshows.panels.season.TvShowSeasonInformationPanel;
import org.tinymediamanager.ui.tvshows.panels.season.TvShowSeasonMediaFilesPanel;
import org.tinymediamanager.ui.tvshows.panels.tvshow.TvShowArtworkPanel;
import org.tinymediamanager.ui.tvshows.panels.tvshow.TvShowCastPanel;
import org.tinymediamanager.ui.tvshows.panels.tvshow.TvShowInformationPanel;
import org.tinymediamanager.ui.tvshows.panels.tvshow.TvShowMediaInformationPanel;
import org.tinymediamanager.ui.tvshows.settings.TvShowSettingsNode;

import net.miginfocom.swing.MigLayout;

public class TvShowUIModule extends AbstractTmmUIModule {
  private static final String       ID       = "tvShows";

  private static TvShowUIModule     instance = null;

  final TvShowSelectionModel        tvShowSelectionModel;
  final TvShowSeasonSelectionModel  tvShowSeasonSelectionModel;
  final TvShowEpisodeSelectionModel tvShowEpisodeSelectionModel;

  private final TvShowTreePanel     listPanel;
  private final JPanel              detailPanel;
  private final JPanel              dataPanel;
  private final TvShowFilterDialog  tvShowFilterDialog;

  private final TmmSettingsNode     settingsNode;

  private TvShowUIModule() {

    tvShowSelectionModel = new TvShowSelectionModel();
    tvShowSeasonSelectionModel = new TvShowSeasonSelectionModel();
    tvShowEpisodeSelectionModel = new TvShowEpisodeSelectionModel();

    listPanel = new TvShowTreePanel(tvShowSelectionModel);

    detailPanel = new JPanel();
    detailPanel.setLayout(new MigLayout("insets 0", "[grow]", "[grow]"));

    dataPanel = new JPanel();
    dataPanel.setLayout(new CardLayout());
    detailPanel.add(dataPanel, "cell 0 0, grow");

    // panel for TV shows
    JTabbedPane tvShowDetailPanel = new MainTabbedPane() {
      private static final long serialVersionUID = 3344548865608767661L;

      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        putClientProperty("roundEdge", Boolean.FALSE);
        super.updateUI();
      }
    };

    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.details"), new TvShowInformationPanel(tvShowSelectionModel));
    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.cast"), new TvShowCastPanel(tvShowSelectionModel));
    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.mediafiles"), new TvShowMediaInformationPanel(tvShowSelectionModel));
    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.artwork"), new TvShowArtworkPanel(tvShowSelectionModel));
    tvShowDetailPanel.add(TmmResourceBundle.getString("metatag.trailer"), new TrailerPanel(tvShowSelectionModel));
    dataPanel.add(tvShowDetailPanel, "tvShow");

    // panel for seasons
    JTabbedPane tvShowSeasonDetailPanel = new MainTabbedPane() {
      private static final long serialVersionUID = 3134567895608767661L;

      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        super.updateUI();
      }
    };
    tvShowSeasonDetailPanel.add(TmmResourceBundle.getString("metatag.details"), new TvShowSeasonInformationPanel(tvShowSeasonSelectionModel));
    tvShowSeasonDetailPanel.add(TmmResourceBundle.getString("metatag.mediafiles"), new TvShowSeasonMediaFilesPanel(tvShowSeasonSelectionModel));
    dataPanel.add(tvShowSeasonDetailPanel, "tvShowSeason");

    // panel for episodes
    JTabbedPane tvShowEpisodeDetailPanel = new MainTabbedPane() {
      private static final long serialVersionUID = 3344548905108767661L;

      @Override
      public void updateUI() {
        putClientProperty("leftBorder", "half");
        putClientProperty("bottomBorder", Boolean.FALSE);
        super.updateUI();
      }
    };

    tvShowEpisodeDetailPanel.add(TmmResourceBundle.getString("metatag.details"), new TvShowEpisodeInformationPanel(tvShowEpisodeSelectionModel));
    tvShowEpisodeDetailPanel.add(TmmResourceBundle.getString("metatag.cast"), new TvShowEpisodeCastPanel(tvShowEpisodeSelectionModel));
    tvShowEpisodeDetailPanel.add(TmmResourceBundle.getString("metatag.mediafiles"),
        new TvShowEpisodeMediaInformationPanel(tvShowEpisodeSelectionModel));
    dataPanel.add(tvShowEpisodeDetailPanel, "tvShowEpisode");

    // glass pane for searching/filtering
    tvShowFilterDialog = new TvShowFilterDialog(listPanel.getTreeTable());

    // create actions and menus
    createActions();
    createPopupMenu();
    registerAccelerators();

    // build settings node
    settingsNode = new TvShowSettingsNode();

    // further initializations
    init();
  }

  private void init() {
    // re-set filters
    if (TvShowModuleManager.getInstance().getSettings().isStoreUiFilters()) {
      SwingUtilities.invokeLater(() -> {
        TvShowModuleManager.getInstance().getTvShowList().searchDuplicateEpisodes();
        listPanel.getTreeTable().setFilterValues(TvShowModuleManager.getInstance().getSettings().getUiFilters());
      });
    }
  }

  public static TvShowUIModule getInstance() {
    if (instance == null) {
      instance = new TvShowUIModule();
    }
    return instance;
  }

  public void setFilterDialogVisible(boolean selected) {
    tvShowFilterDialog.setVisible(selected);
  }

  @Override
  public String getModuleId() {
    return ID;
  }

  @Override
  public JPanel getTabPanel() {
    return listPanel;
  }

  @Override
  public String getTabTitle() {
    return TmmResourceBundle.getString("tmm.tvshows");
  }

  @Override
  public JPanel getDetailPanel() {
    return detailPanel;
  }

  public TvShowSelectionModel getSelectionModel() {
    return tvShowSelectionModel;
  }

  @Override
  public TmmSettingsNode getSettingsNode() {
    return settingsNode;
  }

  private void createActions() {
    searchAction = createAndRegisterAction(TvShowSingleScrapeAction.class);
    editAction = createAndRegisterAction(TvShowEditAction.class);
    updateAction = createAndRegisterAction(TvShowUpdateDatasourcesAction.class);
    renameAction = createAndRegisterAction(TvShowRenameAction.class);
  }

  private void createPopupMenu() {
    // popup menu
    popupMenu = new JPopupMenu();
    popupMenu.add(createAndRegisterAction(TvShowSingleScrapeAction.class));
    popupMenu.add(createAndRegisterAction(TvShowSelectedScrapeAction.class));
    popupMenu.add(createAndRegisterAction(TvShowScrapeEpisodesAction.class));
    popupMenu.add(createAndRegisterAction(TvShowScrapeNewItemsAction.class));
    popupMenu.add(createAndRegisterAction(TvShowScrapeMissingEpisodesAction.class));
    popupMenu.add(createAndRegisterAction(TvShowMissingEpisodeListAction.class));

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(TvShowUpdateAction.class));
    popupMenu.add(createAndRegisterAction(TvShowReadNfoAction.class));
    popupMenu.add(createAndRegisterAction(TvShowRewriteNfoAction.class));

    JMenu mediainfoMenu = new JMenu(TmmResourceBundle.getString("metatag.mediainformation"));
    mediainfoMenu.setIcon(IconManager.MENU);
    mediainfoMenu.add(createAndRegisterAction(TvShowMediaInformationAction.class));
    mediainfoMenu.add(createAndRegisterAction(TvShowRebuildMediainfoXmlAction.class));
    mediainfoMenu.add(createAndRegisterAction(TvShowDeleteMediainfoXmlAction.class));
    popupMenu.add(mediainfoMenu);

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(TvShowEditAction.class));
    popupMenu.add(createAndRegisterAction(TvShowBulkEditAction.class));

    JMenu enhancedEditMenu = new JMenu(TmmResourceBundle.getString("edit.enhanced"));
    enhancedEditMenu.setIcon(IconManager.MENU);
    enhancedEditMenu.add(createAndRegisterAction(TvShowToggleWatchedFlagAction.class));
    enhancedEditMenu.add(createAndRegisterAction(TvShowFetchImdbRatingAction.class));
    enhancedEditMenu.add(createAndRegisterAction(TvShowChangeDatasourceAction.class));
    enhancedEditMenu.add(createAndRegisterAction(TvShowChangeSeasonArtworkAction.class));
    enhancedEditMenu.add(createAndRegisterAction(TvShowReadEpisodeNfoAction.class));
    enhancedEditMenu.add(createAndRegisterAction(TvShowRewriteEpisodeNfoAction.class));
    enhancedEditMenu.add(createAndRegisterAction(TvShowChangeToDvdOrderAction.class));
    enhancedEditMenu.add(createAndRegisterAction(TvShowChangeToAiredOrderAction.class));
    enhancedEditMenu.add(createAndRegisterAction(TvShowAspectRatioDetectAction.class));
    popupMenu.add(enhancedEditMenu);

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(TvShowRenameAction.class));
    popupMenu.add(createAndRegisterAction(TvShowRenamePreviewAction.class));
    popupMenu.add(createAndRegisterAction(TvShowExportAction.class));

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(TvShowDownloadMissingArtworkAction.class));
    popupMenu.add(createAndRegisterAction(TvShowDownloadActorImagesAction.class));
    popupMenu.add(createAndRegisterAction(TvShowSubtitleSearchAction.class));
    popupMenu.add(createAndRegisterAction(TvShowSubtitleDownloadAction.class));
    popupMenu.add(createAndRegisterAction(TvShowDownloadThemeAction.class));

    popupMenu.addSeparator();
    JMenu traktMenu = new JMenu("Trakt.tv");
    traktMenu.setIcon(IconManager.MENU);
    traktMenu.add(createAndRegisterAction(TvShowSyncTraktTvAction.class));
    traktMenu.addSeparator();
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedCollectionTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedWatchedTraktTvAction.class));
    traktMenu.add(createAndRegisterAction(TvShowSyncSelectedRatingTraktTvAction.class));
    popupMenu.add(traktMenu);
    JMenu kodiRPCMenu = KodiRPCMenu.createMenuKodiMenuRightClickTvShows();
    popupMenu.add(kodiRPCMenu);

    popupMenu.addSeparator();
    popupMenu.add(createAndRegisterAction(TvShowCleanUpFilesAction.class));
    popupMenu.add(createAndRegisterAction(TvShowClearImageCacheAction.class));
    popupMenu.add(createAndRegisterAction(TvShowRebuildImageCacheAction.class));
    popupMenu.add(createAndRegisterAction(TvShowRemoveAction.class));
    popupMenu.add(createAndRegisterAction(TvShowDeleteAction.class));

    if (Globals.isDebug()) {
      final JMenu debugMenu = new JMenu("Debug");
      debugMenu.add(new DebugDumpShowAction());
      popupMenu.addSeparator();
      popupMenu.add(debugMenu);
    }

    // activate/deactivate menu items based on some status
    popupMenu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        if (StringUtils.isNotBlank(Settings.getInstance().getKodiHost())) {
          kodiRPCMenu.setText(KodiRPC.getInstance().getVersion());
          kodiRPCMenu.setEnabled(true);
        }
        else {
          kodiRPCMenu.setText("Kodi");
          kodiRPCMenu.setEnabled(false);
        }

        if (License.getInstance().isValidLicense() && StringUtils.isNotBlank(Settings.getInstance().getTraktAccessToken())) {
          traktMenu.setEnabled(true);
        }
        else {
          traktMenu.setEnabled(false);
        }
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });

    listPanel.setPopupMenu(popupMenu);

    // update popup menu
    updatePopupMenu = new JPopupMenu();
    PopupMenuScroller.setScrollerFor(updatePopupMenu, 20, 25, 2, 2);
    updatePopupMenu.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        updatePopupMenu.removeAll();
        updatePopupMenu.add(createAndRegisterAction(TvShowUpdateDatasourcesAction.class));
        updatePopupMenu.addSeparator();
        for (String ds : TvShowModuleManager.getInstance().getSettings().getTvShowDataSource()) {
          updatePopupMenu.add(new TvShowUpdateSingleDatasourceAction(ds));
        }
        updatePopupMenu.addSeparator();
        updatePopupMenu.add(new TvShowUpdateAction());
        updatePopupMenu.addSeparator();
        updatePopupMenu.add(createAndRegisterAction(TvShowAddDatasourceAction.class));
        updatePopupMenu.pack();
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });

    // scrape popup menu
    searchPopupMenu = new JPopupMenu();
    searchPopupMenu.add(createAndRegisterAction(TvShowSingleScrapeAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowSelectedScrapeAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowScrapeEpisodesAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowScrapeNewItemsAction.class));
    searchPopupMenu.add(createAndRegisterAction(TvShowScrapeMissingEpisodesAction.class));

    // edit popupmenu
    editPopupMenu = new JPopupMenu();
    editPopupMenu.add(createAndRegisterAction(TvShowEditAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowBulkEditAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowChangeDatasourceAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowChangeSeasonArtworkAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowToggleWatchedFlagAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowRewriteNfoAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowReadNfoAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowRewriteEpisodeNfoAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowReadEpisodeNfoAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowChangeToDvdOrderAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowChangeToAiredOrderAction.class));

    editPopupMenu.addSeparator();
    editPopupMenu.add(createAndRegisterAction(TvShowSyncTraktTvAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowSyncSelectedTraktTvAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowSyncSelectedCollectionTraktTvAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowSyncSelectedWatchedTraktTvAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowSyncSelectedRatingTraktTvAction.class));

    editPopupMenu.addSeparator();
    editPopupMenu.add(createAndRegisterAction(TvShowMediaInformationAction.class));
    editPopupMenu.add(createAndRegisterAction(TvShowDeleteMediainfoXmlAction.class));

    editPopupMenu.addSeparator();
    editPopupMenu.add(createAndRegisterAction(TvShowExportAction.class));

    editPopupMenu.addSeparator();
    editPopupMenu.add(createAndRegisterAction(TvShowCleanUpFilesAction.class));
  }

  /**
   * set the selected TV shows. This causes the right sided panel to switch to the TV show information panel
   * 
   * @param tvShow
   *          the selected TV show
   */
  public void setSelectedTvShow(TvShow tvShow) {
    tvShowSelectionModel.setSelectedTvShow(tvShow);
    CardLayout cl = (CardLayout) (dataPanel.getLayout());
    cl.show(dataPanel, "tvShow");
  }

  /**
   * set the selected TV show season. This causes the right sided panel to switch to the season information panel
   * 
   * @param tvShowSeason
   *          the selected season
   */
  public void setSelectedTvShowSeason(TvShowSeason tvShowSeason) {
    tvShowSeasonSelectionModel.setSelectedTvShowSeason(tvShowSeason);
    CardLayout cl = (CardLayout) (dataPanel.getLayout());
    cl.show(dataPanel, "tvShowSeason");
  }

  /**
   * set the selected TV show episode. This cases the right sided panel to switch to the episode information panel
   * 
   * @param tvShowEpisode
   *          the selected episode
   */
  public void setSelectedTvShowEpisode(TvShowEpisode tvShowEpisode) {
    tvShowEpisodeSelectionModel.setSelectedTvShowEpisode(tvShowEpisode);
    CardLayout cl = (CardLayout) (dataPanel.getLayout());
    cl.show(dataPanel, "tvShowEpisode");
  }
}
