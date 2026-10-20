package academy.tochkavhoda.competition.model;
import java.util.*;
import java.math.BigDecimal;
public record User(String id, String firstName, String lastName, String company, Set<String> directions, String login, String password, boolean expert) {}
