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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.swingbinding.JListBinding;
import org.jdesktop.swingbinding.SwingBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.ExportTemplate;
import org.tinymediamanager.core.MediaEntityExporter.TemplateType;
import org.tinymediamanager.core.TmmProperties;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.Utils;
import org.tinymediamanager.core.tasks.ExportTask;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.TvShowExporter;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.TmmFontHelper;
import org.tinymediamanager.ui.TmmUIHelper;
import org.tinymediamanager.ui.TmmUILayoutStore;
import org.tinymediamanager.ui.components.NoBorderScrollPane;
import org.tinymediamanager.ui.components.ReadOnlyTextArea;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The Class TvShowExporter.
 * 
 * @author Manuel Laggner
 */
public class TvShowExporterDialog extends TmmDialog {
  private static final long    serialVersionUID = -2197076428245222349L;
  private static final Logger  LOGGER           = LoggerFactory.getLogger(TvShowExporterDialog.class);

  private static final String  DIALOG_ID        = "tvShowExporter";

  private List<TvShow>         tvShows;
  private List<ExportTemplate> templatesFound;

  private final JTextField     tfExportDir;
  private final JList          list;
  private final JLabel         lblTemplateName;
  private final JLabel         lblUrl;
  private final JTextArea      tpDescription;
  private final JCheckBox      chckbxTemplateWithDetail;

  /**
   * Create the dialog.
   * 
   * @param tvShowsToExport
   *          the movies to export
   */
  public TvShowExporterDialog(List<TvShow> tvShowsToExport) {
    super(TmmResourceBundle.getString("tvshow.export"), DIALOG_ID);
    {
      JPanel panelContent = new JPanel();
      getContentPane().add(panelContent);
      panelContent.setLayout(new MigLayout("", "[600lp,grow]", "[300lp,grow][]"));

      JSplitPane splitPane = new JSplitPane();
      splitPane.setName(getName() + ".splitPane");
      TmmUILayoutStore.getInstance().install(splitPane);
      splitPane.setResizeWeight(0.7);
      panelContent.add(splitPane, "cell 0 0,grow");

      JScrollPane scrollPane = new JScrollPane();
      splitPane.setLeftComponent(scrollPane);

      list = new JList();
      scrollPane.setViewportView(list);

      JPanel panelExporterDetails = new JPanel();
      splitPane.setRightComponent(panelExporterDetails);
      panelExporterDetails.setLayout(new MigLayout("", "[100lp,grow]", "[][][][200lp,grow]"));

      lblTemplateName = new TmmLabel("");
      TmmFontHelper.changeFont(lblTemplateName, TmmFontHelper.H1);
      panelExporterDetails.add(lblTemplateName, "cell 0 0,growx");

      lblUrl = new JLabel("");
      TmmFontHelper.changeFont(lblUrl, TmmFontHelper.L1);
      panelExporterDetails.add(lblUrl, "cell 0 1,growx");

      chckbxTemplateWithDetail = new JCheckBox("");
      chckbxTemplateWithDetail.setEnabled(false);
      panelExporterDetails.add(chckbxTemplateWithDetail, "flowx,cell 0 2");

      JLabel lblDetails = new TmmLabel(TmmResourceBundle.getString("export.detail"));
      panelExporterDetails.add(lblDetails, "cell 0 2,growx,aligny center");

      JScrollPane scrollPaneDescription = new NoBorderScrollPane();
      panelExporterDetails.add(scrollPaneDescription, "cell 0 3,grow");

      tpDescription = new ReadOnlyTextArea();
      scrollPaneDescription.setViewportView(tpDescription);
      splitPane.setDividerLocation(300);

      tfExportDir = new JTextField();
      panelContent.add(tfExportDir, "flowx,cell 0 1,growx");
      tfExportDir.setColumns(10);

      JButton btnSetDestination = new JButton(TmmResourceBundle.getString("export.setdestination"));
      panelContent.add(btnSetDestination, "cell 0 1");
      btnSetDestination.addActionListener(e -> {
        String path = TmmProperties.getInstance().getProperty(DIALOG_ID + ".path");
        Path file = TmmUIHelper.selectDirectory(TmmResourceBundle.getString("export.selectdirectory"), path);
        if (file != null) {
          tfExportDir.setText(file.toAbsolutePath().toString());
          TmmProperties.getInstance().putProperty(DIALOG_ID + ".path", file.toAbsolutePath().toString());
        }
      });
    }
    {
      JButton btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
      btnCancel.setIcon(IconManager.CANCEL_INV);
      btnCancel.addActionListener(arg0 -> setVisible(false));
      addButton(btnCancel);

      JButton btnExport = new JButton("Export");
      btnExport.setIcon(IconManager.EXPORT);
      btnExport.addActionListener(arg0 -> {
        if (StringUtils.isBlank(tfExportDir.getText())) {
          return;
        }
        // check selected template
        int index = list.getSelectedIndex();
        if (index < 0) {
          return;
        }

        ExportTemplate selectedTemplate = templatesFound.get(index);
        if (selectedTemplate != null) {
          // check whether the chosen export path exists/is empty or not
          Path exportPath = Paths.get(tfExportDir.getText());
          if (!Files.exists(exportPath)) {
            // export dir does not exist
            JOptionPane.showMessageDialog(TvShowExporterDialog.this, TmmResourceBundle.getString("export.foldernotfound"));
            return;
          }

          try {
            if (!Utils.isFolderEmpty(exportPath)) {
              int decision = JOptionPane.showConfirmDialog(TvShowExporterDialog.this, TmmResourceBundle.getString("export.foldernotempty"), "",
                  JOptionPane.YES_NO_OPTION);// $NON-NLS-1$
              if (decision == JOptionPane.NO_OPTION) {
                return;
              }
            }
          }
          catch (IOException e) {
            LOGGER.warn("could not open folder: {}", e.getMessage());
            return;
          }

          try {
            TvShowExporter exporter = new TvShowExporter(Paths.get(selectedTemplate.getPath()));
            TmmTaskManager.getInstance().addMainTask(new ExportTask(TmmResourceBundle.getString("tvshow.export"), exporter, tvShows, exportPath));
          }
          catch (Exception e) {
            LOGGER.error("Error exporting tv shows: ", e);
          }
          setVisible(false);
        }
      });
      addDefaultButton(btnExport);
    }

    tvShows = tvShowsToExport;
    templatesFound = TvShowExporter.findTemplates(TemplateType.TV_SHOW);
    bindingGroup = initDataBindings();

    // set the last used template as default
    String lastTemplateName = TmmProperties.getInstance().getProperty(DIALOG_ID + ".template");
    if (StringUtils.isNotBlank(lastTemplateName)) {
      list.setSelectedValue(lastTemplateName, true);
    }
  }

  protected BindingGroup initDataBindings() {
    JListBinding<ExportTemplate, List<ExportTemplate>, JList> jListBinding = SwingBindings.createJListBinding(UpdateStrategy.READ, templatesFound,
        list);
    //
    BeanProperty<ExportTemplate, String> exportTemplateBeanProperty = BeanProperty.create("name");
    jListBinding.setDetailBinding(exportTemplateBeanProperty);
    //
    jListBinding.bind();
    //
    BeanProperty<JList, String> jListBeanProperty = BeanProperty.create("selectedElement.name");
    BeanProperty<JLabel, String> jLabelBeanProperty = BeanProperty.create("text");
    AutoBinding<JList, String, JLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, list, jListBeanProperty, lblTemplateName,
        jLabelBeanProperty);
    autoBinding.bind();
    //
    BeanProperty<JList, String> jListBeanProperty_1 = BeanProperty.create("selectedElement.url");
    AutoBinding<JList, String, JLabel, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, list, jListBeanProperty_1, lblUrl,
        jLabelBeanProperty);
    autoBinding_1.bind();
    //
    BeanProperty<JList, String> jListBeanProperty_2 = BeanProperty.create("selectedElement.description");
    BeanProperty<JTextArea, String> JTextAreaBeanProperty = BeanProperty.create("text");
    AutoBinding<JList, String, JTextArea, String> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, list, jListBeanProperty_2,
        tpDescription, JTextAreaBeanProperty);
    autoBinding_2.bind();
    //
    BeanProperty<JList, Boolean> jListBeanProperty_3 = BeanProperty.create("selectedElement.detail");
    BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
    AutoBinding<JList, Boolean, JCheckBox, Boolean> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, list, jListBeanProperty_3,
        chckbxTemplateWithDetail, jCheckBoxBeanProperty);
    autoBinding_3.bind();
    //
    BindingGroup bindingGroup = new BindingGroup();
    //
    bindingGroup.addBinding(jListBinding);
    bindingGroup.addBinding(autoBinding);
    bindingGroup.addBinding(autoBinding_1);
    bindingGroup.addBinding(autoBinding_2);
    bindingGroup.addBinding(autoBinding_3);
    return bindingGroup;
  }
}
