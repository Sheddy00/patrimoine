package school.hei.patrimoine.patrilang.visitors.variable;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static school.hei.patrimoine.patrilang.antlr.PatriLangParser.*;
import static school.hei.patrimoine.patrilang.mapper.MonthMapper.stringToMonth;
import static school.hei.patrimoine.patrilang.modele.variable.VariableType.DATE;
import static school.hei.patrimoine.patrilang.visitors.variable.VariableVisitor.extractVariableName;

import java.time.LocalDate;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import school.hei.patrimoine.patrilang.modele.variable.VariableScope;

@RequiredArgsConstructor
public class VariableDateVisitor {
  private final VariableScope variableScope;
  private final Supplier<VariableExpressionVisitor> variableExpressionVisitorSupplier;

  public LocalDate apply(DateContext ctx) {
    var baseDate = visitBaseDate(ctx.dateAtom());

    if (isNull(ctx.dateDelta())) {
      return baseDate;
    }

    return applyDelta(baseDate, ctx.dateDelta());
  }

  private LocalDate visitBaseDate(DateAtomContext ctx) {
    if (nonNull(ctx.MOT_DATE_INDETERMINER())) {
      return LocalDate.MAX;
    }

    if (nonNull(ctx.MOT_DATE_MAXIMUM())) {
      return LocalDate.MAX;
    }

    if (nonNull(ctx.MOT_DATE_MINIMUM())) {
      return LocalDate.MIN;
    }

    if (nonNull(ctx.DATE_VARIABLE())) {
      var name = extractVariableName(ctx.DATE_VARIABLE().getText());
      return (LocalDate) this.variableScope.get(name, DATE).value();
    }

    var jour = this.variableExpressionVisitorSupplier.get().apply(ctx.jour.expression());
    var annee = this.variableExpressionVisitorSupplier.get().apply(ctx.annee.expression());
    if (nonNull(ctx.moisEntier)) {
      var mois = this.variableExpressionVisitorSupplier.get().apply(ctx.moisEntier.expression());
      return LocalDate.of(annee.intValue(), mois.intValue(), jour.intValue());
    }

    return LocalDate.of(
        annee.intValue(), stringToMonth(ctx.moisTextuel.getText()), jour.intValue());
  }

  private LocalDate applyDelta(LocalDate baseValue, DateDeltaContext ctx) {
    var isMinus = nonNull(ctx.MOINS());
    var anneePart = visitAnneePart(ctx.anneePart());
    var moisPart = visitMoisPart(ctx.moisPart());
    var joursPart = visitJours(ctx.jourPart());
    var semainesPart = visitSemaines(ctx.semainePart());
    LocalDate newValue;

    if (isMinus) {
      newValue =
          baseValue
              .minusYears(anneePart)
              .minusMonths(moisPart)
              .minusDays(semainesPart)
              .minusDays(joursPart);
    } else {
      newValue =
          baseValue
              .plusYears(anneePart)
              .plusMonths(moisPart)
              .plusDays(semainesPart)
              .plusDays(joursPart);
    }

    return newValue;
  }

  private int visitAnneePart(AnneePartContext ctx) {
    return isNull(ctx)
        ? 0
        : this.variableExpressionVisitorSupplier
            .get()
            .apply(ctx.variable().expression())
            .intValue();
  }

  private int visitMoisPart(MoisPartContext ctx) {
    return isNull(ctx)
        ? 0
        : this.variableExpressionVisitorSupplier
            .get()
            .apply(ctx.variable().expression())
            .intValue();
  }

  private int visitJours(JourPartContext ctx) {
    return isNull(ctx)
        ? 0
        : this.variableExpressionVisitorSupplier
            .get()
            .apply(ctx.variable().expression())
            .intValue();
  }

  private int visitSemaines(SemainePartContext ctx) {
    return isNull(ctx)
        ? 0
        : this.variableExpressionVisitorSupplier.get().apply(ctx.variable().expression()).intValue()
            * 7;
  }
}
