package school.hei.patrimoine.visualisation.swing.ihm.google.component.recoupement;

import java.awt.*;
import java.util.Collection;
import java.util.Map;
import javax.swing.*;
import lombok.extern.slf4j.Slf4j;
import school.hei.patrimoine.modele.possession.Possession;
import school.hei.patrimoine.modele.possession.pj.PieceJustificative;
import school.hei.patrimoine.modele.recouppement.model.PossessionRecoupee;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.State;
import school.hei.patrimoine.visualisation.swing.ihm.google.providers.model.Pagination;

@Slf4j
public class PossessionRecoupeeListPanel extends JPanel {
  private final State state;
  private boolean isLoading = false;

  public PossessionRecoupeeListPanel(State state) {
    super();
    this.state = state;

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setOpaque(true);
  }

  public void update(
      Collection<PossessionRecoupee<Possession>> recoupees, Map<String, PieceJustificative> pjs) {
    this.isLoading = true;
    removeAll();
    appendData(recoupees, pjs);

    SwingUtilities.invokeLater(
        () -> {
          Container parent = getParent();
          if (parent instanceof JViewport viewport) {
            viewport.setViewPosition(new Point(0, 0));
          }
          this.isLoading = false;
        });
  }

  public void appendData(
      Collection<PossessionRecoupee<Possession>> recoupees, Map<String, PieceJustificative> pjs) {
    recoupees.forEach(
        possession -> {
          var pj = pjs.getOrDefault(possession.possession().nom(), null);
          add(new PossessionRecoupeeItem(state, possession, pj));
          add(Box.createVerticalStrut(10));
        });

    this.isLoading = false;
    revalidate();
    repaint();
  }

  public JScrollPane toScrollPane() {
    var scroll =
        new JScrollPane(
            this,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scroll.getVerticalScrollBar().setUnitIncrement(20);
    scroll
        .getVerticalScrollBar()
        .addAdjustmentListener(
            e -> {
              var isPaged = state.get("isPaged");
              if (Boolean.TRUE.equals(isPaged)) {
                return;
              }

              if (e.getValueIsAdjusting() || isLoading) {
                return;
              }

              var scrollBar = (JScrollBar) e.getSource();
              int extent = scrollBar.getModel().getExtent();
              int maximum = scrollBar.getModel().getMaximum();
              int value = scrollBar.getValue();

              if ((value + extent) >= (maximum * 0.9)) {
                Pagination currentPagination = state.get("pagination");
                int totalPages = state.get("totalPages") != null ? state.get("totalPages") : 1;

                if (currentPagination != null && currentPagination.page() < totalPages) {
                  this.isLoading = true;
                  var nextPage = currentPagination.page() + 1;
                  state.update(
                      Map.of(
                          "isInfiniteScroll",
                          true,
                          "pagination",
                          new Pagination(nextPage, currentPagination.size())));
                }
              }
            });

    return scroll;
  }
}
