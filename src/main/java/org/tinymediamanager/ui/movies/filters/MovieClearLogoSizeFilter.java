package org.tinymediamanager.ui.movies.filters;

import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;

import org.tinymediamanager.core.MediaFileType;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;

public class MovieClearLogoSizeFilter extends AbstractNumberMovieFilter {

  public MovieClearLogoSizeFilter() {
    super();

    // display the size with px at the end
    spinnerLow.setEditor(prepareNumberEditor(spinnerLow, "####0 px"));
    spinnerHigh.setEditor(prepareNumberEditor(spinnerHigh, "####0 px"));

  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(TmmResourceBundle.getString("filter.clearlogo.width"));
  }

  @Override
  public String getId() {
    return "movieClearLogoSize";
  }

  @Override
  public boolean accept(Movie movie) {
    return matchInt(movie.getArtworkDimension(MediaFileType.CLEARLOGO).width);
  }

  @Override
  protected SpinnerNumberModel getNumberModel() {
    return new SpinnerNumberModel(0, 0, 99999, 1);
  }
}