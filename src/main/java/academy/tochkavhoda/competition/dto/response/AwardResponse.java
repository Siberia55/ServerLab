package academy.tochkavhoda.competition.dto.response;
import java.util.*;
import java.math.BigDecimal;
public record AwardResponse(String applicationId, BigDecimal amount, BigDecimal average) {}
