package academy.tochkavhoda.competition.dto.request;
import java.util.*;
import java.math.BigDecimal;
public record RegisterExpertRequest(String firstName, String lastName, Set<String> directions, String login, String password) {}
