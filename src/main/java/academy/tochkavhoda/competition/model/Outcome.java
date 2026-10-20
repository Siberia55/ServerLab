package academy.tochkavhoda.competition.model;
import java.util.*;
import java.math.BigDecimal;
public record Outcome(List<Award> awards, BigDecimal remaining) {}
