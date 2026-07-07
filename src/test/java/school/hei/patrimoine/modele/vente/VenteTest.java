package school.hei.patrimoine.modele.vente;

import static java.time.Month.*;
import static org.junit.jupiter.api.Assertions.*;
import static school.hei.patrimoine.modele.Argent.euro;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import school.hei.patrimoine.modele.Argent;
import school.hei.patrimoine.modele.Devise;
import school.hei.patrimoine.modele.possession.Compte;
import school.hei.patrimoine.modele.possession.Materiel;
import school.hei.patrimoine.modele.possession.Vente;

class VenteTest {
  private static final LocalDate T0 = LocalDate.of(2024, JANUARY, 1);
  private static final LocalDate T1 = LocalDate.of(2024, JUNE, 1);
  private static final LocalDate T2 = LocalDate.of(2024, SEPTEMBER, 1);

  private static final Argent VALEUR_COMPTABLE = new Argent(10_000, Devise.EUR);
  private static final Argent PRIX_VENTE = new Argent(11_000, Devise.EUR);

  private Compte compte;
  private Materiel materiel;

  @BeforeEach
  void setUp() {
    compte = new Compte("Compte courant", T0, new Argent(0, Devise.EUR));
    materiel = new Materiel("Voiture", T0, T0, VALEUR_COMPTABLE, 0.0);
  }

  @Test
  void vente_possessionNonVendueAvantDateVente() {
    assertFalse(materiel.estVendu(T0));
    assertFalse(materiel.estVendu(T1.minusDays(1)));
  }

  @Test
  void vente_possessionEstVendueAPartirDeDateVente() {
    new Vente(T1, materiel, compte, PRIX_VENTE);

    assertTrue(materiel.estVendu(T1));
    assertTrue(materiel.estVendu(T2));
  }

  @Test
  void vente_possessionNonVendueAvantDateVenteApresCreationVente() {
    new Vente(T1, materiel, compte, PRIX_VENTE);

    assertFalse(materiel.estVendu(T0));
    assertFalse(materiel.estVendu(T1.minusDays(1)));
  }

  @Test
  void vente_prixVenteEnregistre() {
    new Vente(T1, materiel, compte, PRIX_VENTE);

    assertTrue(materiel.getPrixVente().isPresent());
    assertEquals(PRIX_VENTE, materiel.getPrixVente().get());
  }

  @Test
  void vente_dateVenteEnregistree() {
    new Vente(T1, materiel, compte, PRIX_VENTE);

    assertTrue(materiel.getDateVente().isPresent());
    assertEquals(T1, materiel.getDateVente().get());
  }

  @Test
  void vente_compteCrediteApresDatVente() {
    new Vente(T1, materiel, compte, PRIX_VENTE);

    var solde = compte.projectionFuture(T2).valeurComptable();
    assertEquals(PRIX_VENTE, solde);
  }

  @Test
  void vente_compteNonCrediteAvantDateVente() {
    new Vente(T1, materiel, compte, PRIX_VENTE);

    var solde = compte.projectionFuture(T0).valeurComptable();
    assertEquals(new Argent(0, Devise.EUR), solde);
  }

  @Test
  void vente_doubleVente_leveIllegalStateException() {
    new Vente(T1, materiel, compte, PRIX_VENTE);

    assertThrows(IllegalStateException.class, () -> new Vente(T2, materiel, compte, PRIX_VENTE));
  }

  @Test
  void vente_valeurComptableEstZeroApresVente() {
    new Vente(T1, materiel, compte, PRIX_VENTE);

    assertEquals(new Argent(0, Devise.EUR), materiel.valeurComptable());
  }

  @Test
  void sansVente_getPrixVenteEtGetDateVenteSontVides() {
    assertTrue(materiel.getPrixVente().isEmpty());
    assertTrue(materiel.getDateVente().isEmpty());
  }

  @Test
  void getValeurMarche_materielVendu_sansVmExplicite_retourneZero() {
    var materiel = new Materiel("Voiture", T0, T0, euro(20_000), 0.0);
    var beneficiaire = new Compte("CompteBenef", T0, euro(0));

    materiel.vendre(T1, euro(18_000), beneficiaire);

    assertEquals(euro(0), materiel.getValeurMarche(T2));
  }

  @Test
  void getValeurMarche_compteNonVendu_sansVm_retourneToujoursLaValeurComptable() {
    var compte = new Compte("CompteCourant", T0, euro(5_000));

    assertEquals(euro(5_000), compte.getValeurMarche(T2));
  }

  @Test
  void getValeurMarche_avecVmExplicite_resteInchangeeMemeApresVente() {
    var materiel = new Materiel("Voiture", T0, T0, euro(20_000), 0.0);
    new ValeurMarche(materiel, T0, euro(22_000));
    var beneficiaire = new Compte("CompteBenef", T0, euro(0));

    materiel.vendre(T1, euro(18_000), beneficiaire);

    assertEquals(euro(22_000), materiel.getValeurMarche(T1));
  }
}
