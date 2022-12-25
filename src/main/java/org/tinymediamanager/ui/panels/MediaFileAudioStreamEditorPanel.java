/*
 * Copyright 2012 - 2022 Manuel Laggner
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.entities.MediaFileAudioStream;
import org.tinymediamanager.scraper.util.LanguageUtils;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTableFormat;

import net.miginfocom.swing.MigLayout;

/**
 * the class {@link MediaFileAudioStreamEditorPanel} is used to add/edit audio streams
 * 
 * @author Manuel Laggner
 */
public class MediaFileAudioStreamEditorPanel extends JPanel implements IModalPopupPanel {

  private final JTextField                      tfCodec;
  private final JSpinner                        spChannels;
  private final JSpinner                        spBitrate;
  private final JSpinner                        spBitdepth;
  private final JComboBox<String>               cbLanguage;
  private final JTextField                      tfTitle;

  private final JButton                         btnClose;
  private final JButton                         btnCancel;

  private final TmmTableFormat.StringComparator stringComparator;

  private boolean                               cancel = false;

  public MediaFileAudioStreamEditorPanel(MediaFileAudioStream audioStream) {
    super();

    stringComparator = new TmmTableFormat.StringComparator();

    List<LocaleContainer> languages = new ArrayList<>();
    for (Locale locale : Locale.getAvailableLocales()) {
      LocaleContainer localeContainer = new LocaleContainer(locale);
      if (!languages.contains(localeContainer)) {
        languages.add(localeContainer);
      }
    }
    languages.sort((o1, o2) -> stringComparator.compare(o1.toString(), o2.toString()));

    {
      setLayout(new MigLayout("", "[][][300lp,grow]", "[][][][][][]"));
      {
        JLabel lblCodecT = new TmmLabel(TmmResourceBundle.getString("metatag.codec"));
        add(lblCodecT, "cell 0 0,alignx trailing");

        tfCodec = new JTextField();
        tfCodec.setColumns(10);
        add(tfCodec, "cell 1 0 2 1, growx");
      }
      {
        JLabel lblChannelsT = new TmmLabel(TmmResourceBundle.getString("metatag.channels"));
        add(lblChannelsT, "cell 0 1,alignx trailing");

        spChannels = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
        add(spChannels, "cell 1 1, growx");
      }
      {
        JLabel lblBitrate = new TmmLabel(TmmResourceBundle.getString("metatag.bitrate"));
        add(lblBitrate, "cell 0 2,alignx trailing");

        spBitrate = new JSpinner(new SpinnerNumberModel(0, 0, 9000, 1));
        add(spBitrate, "cell 1 2, growx");
      }
      {
        JLabel lblBitdepthT = new TmmLabel(TmmResourceBundle.getString("metatag.bitdepth"));
        add(lblBitdepthT, "cell 0 3,alignx trailing");

        spBitdepth = new JSpinner(new SpinnerNumberModel(0, 0, 20, 1));
        add(spBitdepth, "cell 1 3, growx");
      }
      {
        JLabel lblLanguageT = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
        add(lblLanguageT, "cell 0 4,alignx trailing");

        cbLanguage = new JComboBox(languages.toArray());
        add(cbLanguage, "cell 1 4 2 1");
      }
      {
        JLabel lblTitleT = new TmmLabel(TmmResourceBundle.getString("metatag.title"));
        add(lblTitleT, "cell 0 5,alignx trailing");

        tfTitle = new JTextField();
        tfTitle.setColumns(30);
        add(tfTitle, "cell 1 5 2 1, growx");
      }
      {
        btnCancel = new JButton(TmmResourceBundle.getString("Button.cancel"));
        btnCancel.addActionListener(e -> {
          cancel = true;
          setVisible(false);
        });

        btnClose = new JButton(TmmResourceBundle.getString("Button.save"));
        btnClose.addActionListener(e -> {
          audioStream.setCodec(tfCodec.getText());
          audioStream.setAudioChannels((int) spChannels.getValue());
          audioStream.setBitrate((int) spBitrate.getValue());
          audioStream.setBitDepth((int) spBitdepth.getValue());

          Object obj = cbLanguage.getSelectedItem();
          if (obj instanceof LocaleContainer localeContainer) {
            audioStream.setLanguage(localeContainer.iso3);
          }

          audioStream.setTitle(tfTitle.getText());

          setVisible(false);
        });
      }
    }

    tfCodec.setText(audioStream.getCodec());
    spChannels.setValue(audioStream.getAudioChannels());
    spBitrate.setValue(audioStream.getBitrate());
    spBitdepth.setValue(audioStream.getBitDepth());
    cbLanguage.setSelectedItem(new LocaleContainer(LocaleUtils.toLocale(audioStream.getLanguage())));
    tfTitle.setText(audioStream.getTitle());

    // set focus to the first combobox
    SwingUtilities.invokeLater(tfCodec::requestFocus);
  }

  @Override
  public JComponent getContent() {
    return this;
  }

  @Override
  public JButton getCloseButton() {
    return btnClose;
  }

  @Override
  public JButton getCancelButton() {
    return btnCancel;
  }

  @Override
  public boolean isCancelled() {
    return cancel;
  }

  private static class LocaleContainer {
    private final Locale locale;
    private final String iso3;
    private final String description;

    public LocaleContainer(@NotNull Locale locale) {
      this.locale = locale;
      this.iso3 = LanguageUtils.getIso3Language(locale);
      this.description = StringUtils.isNotBlank(locale.getDisplayLanguage()) ? locale.getDisplayLanguage() + " (" + this.iso3 + ")" : "";
    }

    @Override
    public String toString() {
      return description;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LocaleContainer that = (LocaleContainer) o;
      return iso3.equals(that.iso3);
    }

    @Override
    public int hashCode() {
      return Objects.hash(iso3);
    }
  }
}
