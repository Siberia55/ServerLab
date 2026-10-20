package academy.tochkavhoda.competition.dto.response;
import java.util.*;
import java.math.BigDecimal;
public record ResultsResponse(List<AwardResponse> awards, BigDecimal remaining) {}
