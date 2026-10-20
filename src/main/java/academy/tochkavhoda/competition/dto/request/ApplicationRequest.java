package academy.tochkavhoda.competition.dto.request;
import java.util.*;
import java.math.BigDecimal;
public record ApplicationRequest(String title, String description, Set<String> directions, BigDecimal amount) {}
