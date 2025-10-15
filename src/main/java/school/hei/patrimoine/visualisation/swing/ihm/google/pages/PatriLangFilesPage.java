package school.hei.patrimoine.visualisation.swing.ihm.google.pages;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import static school.hei.patrimoine.patrilang.PatriLangTranspiler.TOUT_CAS_FILE_EXTENSION;
import static school.hei.patrimoine.patrilang.PatriLangTranspiler.transpileToutCas;
import static school.hei.patrimoine.visualisation.swing.ihm.google.component.AppBar.*;
import static school.hei.patrimoine.visualisation.swing.ihm.google.utils.MessageDialog.showError;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import school.hei.patrimoine.cas.Cas;
import school.hei.patrimoine.cas.CasSet;
import school.hei.patrimoine.cas.CasSetAnalyzer;
import school.hei.patrimoine.google.model.Pagination;
import school.hei.patrimoine.modele.recouppement.RecoupeurDeCasSet;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.AppBar;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.HtmlViewer;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.app.LazyPage;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.button.Button;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.comment.CommentSideBar;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.files.FileSideBar;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.recoupement.AddImprevuDialog;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.State;
import school.hei.patrimoine.visualisation.swing.ihm.google.utils.AsyncTask;

@Getter
@Slf4j
public class PatriLangFilesPage extends LazyPage {
  public static final String PAGE_NAME = "patrilang-files";
  private static final Integer COMMENT_PAGE_SIZE = 2;

  private CasSet plannedCasSet;
  private CasSet doneCasSet;
  private Button addImprevuButton;

  private final State state;
  private HtmlViewer htmlViewer;

  public PatriLangFilesPage() {
    super(PAGE_NAME);

    state =
        new State(
            Map.of(
                "viewMode",
                ViewMode.VIEW,
                "fontSize",
                14,
                "commentPagination",
                new Pagination(COMMENT_PAGE_SIZE, null)));

    globalState().subscribe(Set.of("newUpdate"), this::updateCasSet);
    updateCas();

    state.subscribe(
        "selectedFile",
        () -> {
          this.updateCas();
          this.updateAddImprevuButtonVisibility();
        });

    setLayout(new BorderLayout());
  }

  @Override
  protected void init() {
    addAppBar();
    addMainSplitPane();
  }

  private void updateAddImprevuButtonVisibility() {
    assert addImprevuButton != null;

    File selectedFile = state.get("selectedFile");

    SwingUtilities.invokeLater(
        () -> {
          if (selectedFile == null) {
            addImprevuButton.setVisible(false);
            return;
          }

          boolean isToutCasFile = selectedFile.getName().endsWith(TOUT_CAS_FILE_EXTENSION);
          addImprevuButton.setVisible(!isToutCasFile);
        });
  }

  private Button addImprevuButton() {
    return new Button(
        "Ajouter un imprévu",
        e -> {
          if (!ensureReadyToAddImprevu()) {
            return;
          }
          new AddImprevuDialog(state);
        });
  }

  private boolean ensureReadyToAddImprevu() {
    File selectedFile = state.get("selectedFile");
    if (selectedFile == null) {
      showError("Erreur", "Veuillez sélectionner un fichier avant d'ajouter un imprévu");
      return false;
    }

    if (plannedCasSet == null || doneCasSet == null) {
      updateCasSet();
    }

    updateCas();

    if (state.get("plannedCas") == null) {
      showError(
          "Erreur",
          "Le cas planifié n'a pas été trouvé. Veuillez vérifier que le fichier"
              + " sélectionné est correct.");
      return false;
    }

    return true;
  }

  private void addAppBar() {
    this.addImprevuButton = addImprevuButton();

    var appBar =
        new AppBar(
            List.of(
                builtInViewModeSelect(state),
                builtInFileDropdown(state, () -> getHtmlViewer().getText()),
                evolutionGraphicButton(),
                recoupementButton(),
                addImprevuButton),
            List.of(builtInFontSizeControllerButton(state), builtInUserInfoPanel()));

    add(appBar, BorderLayout.NORTH);
    updateAddImprevuButtonVisibility();
  }

  private void addMainSplitPane() {
    var horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    horizontalSplit.setLeftComponent(new FileSideBar(state));

    this.htmlViewer = new HtmlViewer(state);
    var rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    rightSplit.setLeftComponent(htmlViewer.toScrollPane());
    rightSplit.setRightComponent(new CommentSideBar(state));
    rightSplit.setDividerLocation(700);

    horizontalSplit.setRightComponent(rightSplit);
    horizontalSplit.setDividerLocation(200);

    add(horizontalSplit, BorderLayout.CENTER);
  }

  public Button recoupementButton() {
    return new Button(
        "Recoupement",
        e -> {
          state.invalidate(
              Set.of(
                  "selectedFile", "selectedCasSetFile", "isPlannedSelectedFile", "selectedFileId"));
          pageManager().navigate(RecoupementPage.PAGE_NAME);
        });
  }

  public Button evolutionGraphicButton() {
    return new Button("Évolution graphique", e -> showCasSetAnalyser());
  }

  private static void showCasSetAnalyser() {
    AsyncTask.<CasSet>builder()
        .task(
            () -> {
              var plannedCasSet =
                  transpileToutCas(FileSideBar.getPlannedCasSetFile().getAbsolutePath());
              var doneCasSet = transpileToutCas(FileSideBar.getDoneCasSetFile().getAbsolutePath());
              return RecoupeurDeCasSet.of(plannedCasSet, doneCasSet).getRecouped();
            })
        .onSuccess(casSet -> new CasSetAnalyzer(DISPOSE_ON_CLOSE).accept(casSet))
        .onError(
            error -> {
              if (error.getMessage().contains("Objectif")) {
                showError("Erreur", "Certains objectifs ne sont pas atteints");
                return;
              }

              showError(
                  "Erreur",
                  "Une erreur s'est produite lors de la génération de l'évolution graphique");
            })
        .build()
        .execute();
  }

  private void updateCasSet() {
    boolean isNewUpdate =
        globalState().get("newUpdate") == null || (boolean) globalState().get("newUpdate");

    if (!isNewUpdate) return;

    this.plannedCasSet = transpileToutCas(FileSideBar.getPlannedCasSetFile().getAbsolutePath());
    this.doneCasSet = transpileToutCas(FileSideBar.getDoneCasSetFile().getAbsolutePath());

    updateCas();
  }

  private void updateCas() {
    File selectedFile = state.get("selectedFile");

    if (selectedFile == null || plannedCasSet == null || doneCasSet == null) {
      return;
    }

    if (selectedFile.getName().endsWith(TOUT_CAS_FILE_EXTENSION)) {
      return;
    }

    var plannedCas = getCasPatriLangFilesPage(selectedFile, plannedCasSet);
    var doneCas = getCasPatriLangFilesPage(selectedFile, doneCasSet);

    if (plannedCas == null || doneCas == null) {
      log.error("No cases found for " + selectedFile.getName());
      return;
    }

    state.update(Map.of("plannedCas", plannedCas, "doneCas", doneCas));
  }

  private static Cas getCasPatriLangFilesPage(File file, CasSet casSet) {
    if (file == null || casSet == null) {
      return null;
    }

    var fileName = file.getName();
    var baseName = fileName.contains(".") ? fileName.substring(0, fileName.indexOf('.')) : fileName;

    return casSet.set().stream()
        .filter(cas -> cas.patrimoine() != null && cas.patrimoine().nom().equals(baseName))
        .findFirst()
        .orElse(null);
  }
}
