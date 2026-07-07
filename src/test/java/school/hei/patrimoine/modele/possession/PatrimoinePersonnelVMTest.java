package school.hei.patrimoine.modele.possession;

import static java.time.Month.JANUARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static school.hei.patrimoine.modele.Argent.euro;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import school.hei.patrimoine.modele.Devise;
import school.hei.patrimoine.modele.Patrimoine;
import school.hei.patrimoine.modele.Personne;
import school.hei.patrimoine.modele.vente.ValeurMarche;

class PatrimoinePersonnelVMTest {
  private static final LocalDate T0 = LocalDate.of(2025, JANUARY, 1);
  private static final LocalDate T_FUT = LocalDate.of(2026, JANUARY, 1);

  @Test
  void getValeurMarche_sansVmExplicite_appliqueLeTauxDePossession() {
    var koto = new Personne("Koto");
    var soa = new Personne("Soa");
    var compteCommun = new Compte("CompteCommun", T0, euro(100_000));

    Patrimoine.of("KotoEtSoa", Devise.EUR, T0, Map.of(koto, 0.5, soa, 0.5), Set.of(compteCommun));

    var louPerso = koto.patrimoine(Devise.EUR, T0);

    assertEquals(euro(50_000), louPerso.getValeurMarche());
  }

  @Test
  void getValeurMarche_possesseurUnique_egaleLaValeurComptableIntegrale() {
    var koto = new Personne("Koto");
    var compte = new Compte("CompteKoto", T0, euro(80_000));

    Patrimoine.of("Koto", Devise.EUR, T0, koto, Set.of(compte));

    var kotoPerso = koto.patrimoine(Devise.EUR, T0);

    assertEquals(euro(80_000), kotoPerso.getValeurMarche());
  }

  @Test
  void getValeurMarche_tauxAsymetrique_appliqueLeBonPourcentage() {
    var koto = new Personne("Koto");
    var famille = new Personne("FamilleZafy");
    var maison = new Materiel("Maison", T0, T0, euro(200_000), 0.0);

    Patrimoine.of("FamilleZafy", Devise.EUR, T0, Map.of(koto, 0.3, famille, 0.7), Set.of(maison));

    var louPerso = koto.patrimoine(Devise.EUR, T0);

    assertEquals(euro(60_000), louPerso.getValeurMarche());
  }

  @Test
  void getValeurMarche_coherenteAvecValeurComptable_quandAucuneVmExplicite() {
    var koto = new Personne("Koto");
    var soa = new Personne("Soa");
    var famille = new Personne("FamilleZafy");

    Patrimoine.of(
        "KotoEtSoa",
        Devise.EUR,
        T0,
        Map.of(koto, 0.5, soa, 0.5),
        Set.of(new Compte("CompteCommun", T0, euro(100_000))));
    Patrimoine.of(
        "FamilleZafy",
        Devise.EUR,
        T0,
        Map.of(koto, 0.3, famille, 0.7),
        Set.of(new Materiel("Maison", T0, T0, euro(200_000), 0.0)));
    Patrimoine.of("Lou", Devise.EUR, T0, koto, Set.of(new Compte("CompteKoto", T0, euro(15_000))));

    var louPerso = koto.patrimoine(Devise.EUR, T0);

    assertEquals(louPerso.getValeurComptable(), louPerso.getValeurMarche());
  }

  @Test
  void getValeurMarche_avecVmExplicitesSurLesSousPossessions_estCorrectementProratisee() {
    var koto = new Personne("Koto");
    var famille = new Personne("FamilleZafy");
    var maison = new Materiel("Maison", T0, T0, euro(200_000), 0.0);
    new ValeurMarche(maison, T0, euro(260_000));

    Patrimoine.of("FamilleZafy", Devise.EUR, T0, Map.of(koto, 0.3, famille, 0.7), Set.of(maison));

    var louPerso = koto.patrimoine(Devise.EUR, T0);

    assertEquals(euro(78_000), louPerso.getValeurMarche());
  }

  @Test
  void getValeurMarche_projectionFuture_conserveLeTauxDePossession() {
    var koto = new Personne("Koto");
    var soa = new Personne("Soa");
    var compteCommun = new Compte("CompteCommun", T0, euro(100_000));

    Patrimoine.of("KotoEtSoa", Devise.EUR, T0, Map.of(koto, 0.5, soa, 0.5), Set.of(compteCommun));

    var louPerso = koto.patrimoine(Devise.EUR, T0).projectionFuture(T_FUT);

    assertEquals(euro(50_000), louPerso.getValeurMarche());
    assertEquals(louPerso.getValeurComptable(), louPerso.getValeurMarche());
  }

  @Test
  void getValeurMarche_plusieursSousPossessions_sommeLesPartsPonderees() {
    var koto = new Personne("Koto");
    var soa = new Personne("Soa");
    var compte = new Compte("CompteCommun", T0, euro(40_000));
    var materiel = new Materiel("Voiture", T0, T0, euro(60_000), 0.0);

    Patrimoine.of(
        "KotoEtSoa", Devise.EUR, T0, Map.of(koto, 0.5, soa, 0.5), Set.of(compte, materiel));

    var louPerso = koto.patrimoine(Devise.EUR, T0);

    assertEquals(euro(50_000), louPerso.getValeurMarche());
  }
}
