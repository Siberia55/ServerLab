package academy.tochkavhoda.competition.model;
import java.util.*;
import java.math.BigDecimal;
public record Application(String id, String participantId, String title, String description, Set<String> directions, BigDecimal amount) {}
