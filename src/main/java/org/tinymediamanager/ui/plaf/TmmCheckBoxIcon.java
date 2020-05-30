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
package org.tinymediamanager.ui.plaf;

import static com.formdev.flatlaf.FlatClientProperties.SELECTED_STATE;
import static com.formdev.flatlaf.FlatClientProperties.SELECTED_STATE_INDETERMINATE;
import static com.formdev.flatlaf.FlatClientProperties.clientPropertyEquals;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.lang.reflect.Method;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;

import com.formdev.flatlaf.icons.FlatCheckBoxIcon;
import com.formdev.flatlaf.ui.FlatButtonUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

public class TmmCheckBoxIcon extends FlatCheckBoxIcon {

  @Override
  protected void paintIcon(Component c, Graphics2D g2) {
    boolean indeterminate = c instanceof JComponent && clientPropertyEquals((JComponent) c, SELECTED_STATE, SELECTED_STATE_INDETERMINATE);
    boolean selected = indeterminate || (c instanceof AbstractButton && ((AbstractButton) c).isSelected());

    // paint focused border
    if (FlatUIUtils.isPermanentFocusOwner(c) && focusWidth > 0) {
      g2.setColor(focusColor);
      paintFocusBorder(g2);
    }

    // paint border
    g2.setColor(FlatButtonUI.buttonStateColor(c, selected ? selectedBorderColor : borderColor, disabledBorderColor,
        selected && selectedFocusedBorderColor != null ? selectedFocusedBorderColor : focusedBorderColor, hoverBorderColor, null));
    paintBorder(g2);

    // paint background
    FlatUIUtils.setColor(g2,
        FlatButtonUI.buttonStateColor(c, selected ? selectedBackground : background, disabledBackground, focusedBackground,
            selected && selectedHoverBackground != null ? selectedHoverBackground : hoverBackground,
            selected && selectedPressedBackground != null ? selectedPressedBackground : pressedBackground),
        background);
    paintBackground(g2);

    g2.setColor(c.isEnabled() ? checkmarkColor : disabledCheckmarkColor);
    // paint tri-state
    if (isTriStateButtonModelStatusMixed(c)) {
      paintTriState(g2);
    }
    else if (selected) {
      // paint checkmark
      paintCheckmark(g2);
    }
    else if (indeterminate) {
      // paint indeterminate
      paintIndeterminate(g2);
    }
  }

  private void paintTriState(Graphics2D g2) {
    Path2D.Float path = new Path2D.Float();
    path.moveTo(4.5f, 7f);
    path.lineTo(11.25f, 7f);

    g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(path);
  }

  private boolean isTriStateButtonModelStatusMixed(Component c) {
    if (!(c instanceof AbstractButton)) {
      return false;
    }

    ButtonModel model = ((AbstractButton) c).getModel();

    if ("TriStateButtonModel".equals(model.getClass().getSimpleName())) {
      // check the model state via reflection
      try {
        Method method = model.getClass().getMethod("isMixed");
        if ((boolean) method.invoke(model)) {
          return true;
        }
      }
      catch (Exception ignored) {
      }
    }
    return false;
  }
}
