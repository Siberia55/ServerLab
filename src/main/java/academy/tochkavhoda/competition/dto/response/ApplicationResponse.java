package academy.tochkavhoda.competition.dto.response;
import java.util.*;
import java.math.BigDecimal;
public record ApplicationResponse(String id, String participantId, String title, String description, Set<String> directions, BigDecimal amount, BigDecimal average, Integer ownRating) {}
