package academy.tochkavhoda.competition.dto.request;
import java.util.*;
import java.math.BigDecimal;
public record RegisterParticipantRequest(String firstName, String lastName, String company, String login, String password) {}
