package org.tinymediamanager.scraper.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.tvshow.TvShowEpisodeSearchAndScrapeOptions;
import org.tinymediamanager.core.tvshow.TvShowSearchAndScrapeOptions;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.CountryCode;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.interfaces.ITvShowMetadataProvider;

/**
 * @author Nikolas Mavropoylos
 */
public class ITTmdbTvShowMetadataProviderTest extends BasicTest {

  @Before
  public void setUpBeforeTest() throws Exception {
    BasicTest.setup();
    setLicenseKey();
  }

  @Test
  public void testTvShowScrapeDataIntegrityInGerman() throws Exception {
    ITvShowMetadataProvider mp = new TmdbTvShowMetadataProvider();

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setTmdbId(160);
    options.setLanguage(MediaLanguages.de);
    options.setCertificationCountry(CountryCode.DE);
    options.setReleaseDateCountry("DE");

    MediaMetadata md = mp.getMetadata(options);

    assertThat(md).isNotNull();
    assertThat(md.getTitle()).isEqualTo("Teenage Mutant Ninja Turtles");
    assertThat(md.getPlot()).isEqualTo(
        "Wer weiß schon, was in der Kanalisation von New York so alles lebt... Warum nicht auch vier Schildkröten? Allerdings vier ganz besondere Schildkröten, denn Leonardo, Donatello, Raphael und Michelangelo sind die Teenage Mutant Ninja Turtles! Durch eine geheimnisvolle Substanz, das Ooze, sind sie einst mutiert und haben nicht nur sprechen gelernt. Auch ihre sonstigen Fähigkeiten sind durchaus beachtlich. Denn ihr Meister, die ebenfalls mutierte Ratte Splinter, hat sie in der Kunst des Ninja-Kampfes unterrichtet. Mit erstaunlichen Ergebnissen.");

  }

  @Test
  public void testTvShowScrapeDataIntegrityInGreekWithFallBackLanguageReturnCorrectData() throws Exception {
    ITvShowMetadataProvider mp = new TmdbTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("titleFallback", true);
    mp.getProviderInfo().getConfig().setValue("titleFallbackLanguage", "el-GR");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setTmdbId(160);
    options.setLanguage(MediaLanguages.sq); // unavailable
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    MediaMetadata md = mp.getMetadata(options);

    assertThat(md).isNotNull();
    assertThat(md.getTitle()).isEqualTo("Χελωνονιντζάκια");
  }

  @Test
  public void testTvShowSearchDataIntegrityInEnglish() throws Exception {
    // 1399
    ITvShowMetadataProvider mp = new TmdbTvShowMetadataProvider();

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setSearchQuery("Game Of Thrones");
    options.setLanguage(MediaLanguages.en);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    List<MediaSearchResult> searchResults = new ArrayList<>(mp.search(options));

    assertThat(searchResults).isNotNull();
    assertThat(searchResults.get(0).getTitle()).isEqualTo("Game of Thrones");
    assertThat(searchResults.get(0).getId()).isEqualTo("1399");
  }

  @Test
  public void testTvShowSearchDataIntegrityInGreek() throws Exception {
    ITvShowMetadataProvider mp = new TmdbTvShowMetadataProvider();

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setSearchQuery("2057");
    options.setLanguage(MediaLanguages.el);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    List<MediaSearchResult> searchResults = new ArrayList<>(mp.search(options));

    assertThat(searchResults).isNotNull();
    assertThat(searchResults.get(0).getTitle()).isEqualTo("2057:  Ο κόσμος σε 50 χρόνια");
    assertThat(searchResults.get(0).getId()).isEqualTo("4104");
  }

  @Test
  public void testTvShowSearchDataWithFallBackLanguageShouldFallbackAndReturnCorrectData() throws Exception {
    ITvShowMetadataProvider mp = new TmdbTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("titleFallback", true);
    mp.getProviderInfo().getConfig().setValue("titleFallbackLanguage", "da-DK");

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setSearchQuery("Band of Brothers");
    options.setLanguage(MediaLanguages.bm); // BM not available!
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");

    List<MediaSearchResult> searchResults = new ArrayList<>(mp.search(options));

    assertThat(searchResults).isNotNull();
    assertThat(searchResults.size()).isGreaterThanOrEqualTo(1);

    assertThat(searchResults.get(0).getId()).isEqualTo("4613");
    assertThat(searchResults.get(0).getTitle()).isEqualTo("Kammerater i krig");
  }

  @Test
  public void testTvEpisodeListDataIntegrityWithoutFallBackLanguageAndReturnIncorrectData() throws Exception {
    ITvShowMetadataProvider mp = new TmdbTvShowMetadataProvider();

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setLanguage(MediaLanguages.el);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");
    options.setId(mp.getId(), "456");

    List<MediaMetadata> episodes = mp.getEpisodeList(options);

    assertThat(episodes).isNotNull();
    assertThat(episodes.size()).isGreaterThanOrEqualTo(679);

    for (MediaMetadata episode : episodes) {
      if (episode.getEpisodeNumber() == 12 && episode.getSeasonNumber() == 2) {
        assertThat(episode.getTitle()).isEqualTo("Επεισόδιο 12");
        assertThat(episode.getPlot())
            .isEqualTo("Η φτηνή τηλεόραση των Σίμσονς χαλάει κι ο Χόμερ με την Μαρτζ διηγούνται στα παιδιά τους πώς γνωρίστηκαν.");
      }
    }

  }

  @Test
  public void testTvEpisodeListDataIntegrityWithFallBackLanguageShouldFallbackAndReturnCorrectData() throws Exception {
    ITvShowMetadataProvider mp = new TmdbTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("titleFallback", true);
    mp.getProviderInfo().getConfig().setValue("titleFallbackLanguage", MediaLanguages.en.name());

    TvShowSearchAndScrapeOptions options = new TvShowSearchAndScrapeOptions();
    options.setLanguage(MediaLanguages.el);
    options.setCertificationCountry(CountryCode.US);
    options.setReleaseDateCountry("US");
    options.setId(mp.getId(), "456"); // Simpsons

    List<MediaMetadata> episodes = mp.getEpisodeList(options);

    assertThat(episodes).isNotNull();
    assertThat(episodes.size()).isGreaterThanOrEqualTo(679);

    for (MediaMetadata episode : episodes) {
      if (episode.getEpisodeNumber() == 12 && episode.getSeasonNumber() == 2) {
        // https://www.themoviedb.org/tv/456-the-simpsons/season/2/episode/12?language=el-GR
        assertThat(episode.getTitle()).isEqualTo("Επεισόδιο 12"); // NOT translated on HP but in API?!???
        assertThat(episode.getPlot())
            .isEqualTo("Η φτηνή τηλεόραση των Σίμσονς χαλάει κι ο Χόμερ με την Μαρτζ διηγούνται στα παιδιά τους πώς γνωρίστηκαν.");
      }
    }
  }

  @Test
  public void testScrapeTvEpisodeWithFallBackLanguageShouldFallbackAndReturnCorrectData() throws Exception {
    ITvShowMetadataProvider mp = new TmdbTvShowMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("titleFallback", true);
    mp.getProviderInfo().getConfig().setValue("titleFallbackLanguage", MediaLanguages.en.toString());

    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
    options.setLanguage(MediaLanguages.el);
    options.getTvShowIds().put(MediaMetadata.TMDB, 456);
    options.setId(MediaMetadata.SEASON_NR, "2");
    options.setId(MediaMetadata.EPISODE_NR, "12");

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata).isNotNull();
    assertThat(mediaMetadata.getEpisodeNumber()).isEqualTo(12);
    assertThat(mediaMetadata.getSeasonNumber()).isEqualTo(2);
    assertThat(mediaMetadata.getTitle()).isEqualTo("The Way We Was"); // no greek translation here
    assertThat(mediaMetadata.getOriginalTitle()).isEqualTo("The Way We Was");
    assertThat(mediaMetadata.getPlot())
        .isEqualTo("Η φτηνή τηλεόραση των Σίμσονς χαλάει κι ο Χόμερ με την Μαρτζ διηγούνται στα παιδιά τους πώς γνωρίστηκαν.");
  }

  @Test
  public void testScrapeTvEpisodeWithoutFallBackLanguageAndReturnIncorrectData() throws Exception {
    ITvShowMetadataProvider mp = new TmdbTvShowMetadataProvider();

    TvShowEpisodeSearchAndScrapeOptions options = new TvShowEpisodeSearchAndScrapeOptions();
    options.setLanguage(MediaLanguages.el);
    options.getTvShowIds().put(MediaMetadata.TMDB, 456);
    options.setId(MediaMetadata.SEASON_NR, "2");
    options.setId(MediaMetadata.EPISODE_NR, "12");

    MediaMetadata mediaMetadata = mp.getMetadata(options);

    assertThat(mediaMetadata).isNotNull();
    assertThat(mediaMetadata.getEpisodeNumber()).isEqualTo(12);
    assertThat(mediaMetadata.getSeasonNumber()).isEqualTo(2);
    assertThat(mediaMetadata.getTitle()).isEqualTo("Επεισόδιο 12");
    assertThat(mediaMetadata.getPlot())
        .isEqualTo("Η φτηνή τηλεόραση των Σίμσονς χαλάει κι ο Χόμερ με την Μαρτζ διηγούνται στα παιδιά τους πώς γνωρίστηκαν.");

  }

}
