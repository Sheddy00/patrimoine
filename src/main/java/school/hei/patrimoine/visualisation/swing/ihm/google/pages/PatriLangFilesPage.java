package school.hei.patrimoine.visualisation.swing.ihm.google.pages;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import static school.hei.patrimoine.patrilang.PatriLangTranspiler.TOUT_CAS_FILE_EXTENSION;
import static school.hei.patrimoine.visualisation.swing.ihm.google.component.appbar.AppBar.*;
import static school.hei.patrimoine.visualisation.swing.ihm.google.modele.MessageDialog.showError;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import school.hei.patrimoine.cas.CasSet;
import school.hei.patrimoine.cas.CasSetAnalyzer;
import school.hei.patrimoine.google.model.Pagination;
import school.hei.patrimoine.modele.recouppement.RecoupeurDeCasSet;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.HtmlViewer;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.app.LazyPage;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.appbar.AppBar;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.appbar.builtin.SaveAndSyncFileButton;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.button.Button;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.comment.CommentSideBar;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.files.FileSideBar;
import school.hei.patrimoine.visualisation.swing.ihm.google.component.recoupement.AddImprevuDialog;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.AsyncTask;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.CasSetSetter;
import school.hei.patrimoine.visualisation.swing.ihm.google.modele.State;

@Getter
@Slf4j
public class PatriLangFilesPage extends LazyPage {
  public static final String PAGE_NAME = "patrilang-files";
  private static final int COMMENT_PAGE_SIZE = 100;

  private final State state;
  private final CasSetSetter casSetSetter;

  private HtmlViewer htmlViewer;
  private Button addImprevuButton;

  public PatriLangFilesPage() {
    super(PAGE_NAME);
    this.casSetSetter = CasSetSetter.getInstance();

    state =
        new State(
            Map.of(
                "viewMode",
                ViewMode.VIEW,
                "fontSize",
                14,
                "commentPagination",
                new Pagination(COMMENT_PAGE_SIZE, null)));

    state.subscribe(
        "selectedFile",
        () -> {
          this.updateCas();
          this.updateAddImprevuButtonVisibility();
        });

    casSetSetter.addObserver(
        () -> {
          if (this.htmlViewer != null) {
            this.htmlViewer.update();
          }

          this.updateCas();

          if (this.addImprevuButton != null) {
            this.updateAddImprevuButtonVisibility();
          }
        });

    setLayout(new BorderLayout());
  }

  @Override
  protected void init() {
    casSetSetter.updatedCasSet();

    addAppBar();
    addMainSplitPane();
  }

  private void addAppBar() {
    this.addImprevuButton = addImprevuButton(state);
    addImprevuButton.setVisible(false);

    var appBar =
        new AppBar(
            List.of(
                builtInViewModeSelect(state),
                new SaveAndSyncFileButton(state, () -> getHtmlViewer().getText()),
                evolutionGraphicButton(),
                recoupementButton(),
                addImprevuButton),
            List.of(builtInFontSizeControllerButton(state), builtInUserInfoPanel()));

    add(appBar, BorderLayout.NORTH);
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

  static Button addImprevuButton(State state) {
    return new Button(
        "Ajouter un imprévu",
        e -> {
          if (state.get("selectedFile") == null) {
            showError("Erreur", "Veuillez sélectionner un fichier avant d'ajouter un imprévu");
            return;
          }
          new AddImprevuDialog(state);
        });
  }

  private void updateAddImprevuButtonVisibility() {
    var optionalSelectedFile = selectedFile();

    if (optionalSelectedFile.isEmpty() || isToutCasFile(optionalSelectedFile.get())) {
      addImprevuButton.setVisible(false);
      return;
    }

    addImprevuButton.setVisible(!isPlannedSelectedFile());
  }

  private void showCasSetAnalyser() {
    AsyncTask.<CasSet>builder()
        .task(
            () ->
                RecoupeurDeCasSet.of(casSetSetter.plannedCasSet(), casSetSetter.doneCasSet())
                    .getRecouped())
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

  private void updateCas() {
    File selectedFile = state.get("selectedFile");
    if (isPlannedSelectedFile() || isToutCasFile(selectedFile)) {
      return;
    }

    var doneCas = casSetSetter.getCas(selectedFile, casSetSetter.doneCasSet());
    var plannedCas = casSetSetter.getCas(selectedFile, casSetSetter.plannedCasSet());
    state.update(Map.of("plannedCas", plannedCas, "doneCas", doneCas));
  }

  private Optional<File> selectedFile() {
    return Optional.ofNullable(state.get("selectedFile"));
  }

  private boolean isToutCasFile(File file) {
    return file.getName().endsWith(TOUT_CAS_FILE_EXTENSION);
  }

  private boolean isPlannedSelectedFile() {
    return state.get("isPlannedSelectedFile") == null
        || (boolean) state.get("isPlannedSelectedFile");
  }
}
