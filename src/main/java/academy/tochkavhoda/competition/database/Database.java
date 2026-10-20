package academy.tochkavhoda.competition.database;
import academy.tochkavhoda.competition.model.*;
import academy.tochkavhoda.competition.exception.CompetitionException;
import java.util.*;
/** One in-memory database for one competition. Access is serialized by the service. */
public final class Database {
    private final Map<String,User> users=new LinkedHashMap<>();
    private final Map<String,String> sessions=new HashMap<>();
    private final Map<String,Application> applications=new LinkedHashMap<>();
    private final Map<String,Map<String,Rating>> ratings=new HashMap<>();
    private Outcome outcome;
    public List<User> users() { return List.copyOf(users.values()); }
    public void insert(User user) {
        for(User old: users.values()) {
            if(old.login().equals(user.login())) throw new CompetitionException("Логин уже используется");
            if(!old.expert() && !user.expert() && old.company().equalsIgnoreCase(user.company())) throw new CompetitionException("Фирма уже зарегистрирована");
            if(old.expert() && user.expert() && old.firstName().equalsIgnoreCase(user.firstName()) && old.lastName().equalsIgnoreCase(user.lastName())) throw new CompetitionException("Эксперт уже зарегистрирован");
        }
        users.put(user.id(),user);
    }
    public void session(String token,String userId) { sessions.put(token,userId); }
    public User user(String token) { return users.get(sessions.get(token)); }
    public void logout(String token) { sessions.remove(token); }
    public void deleteUser(User user) {
        users.remove(user.id()); sessions.values().removeIf(user.id()::equals);
        new ArrayList<>(applications.values()).stream().filter(a->a.participantId().equals(user.id())).forEach(a->deleteApplication(a.id()));
        ratings.values().forEach(m->m.remove(user.id()));
    }
    public List<Application> applications() { return List.copyOf(applications.values()); }
    public Application application(String id) { return applications.get(id); }
    public void insertApplication(Application a) { applications.put(a.id(),a); }
    public void deleteApplication(String id) { applications.remove(id); ratings.remove(id); }
    public List<Rating> ratings(String id) { return List.copyOf(ratings.getOrDefault(id,Map.of()).values()); }
    public void rate(Rating rating) { ratings.computeIfAbsent(rating.applicationId(),k->new HashMap<>()).put(rating.expertId(),rating); }
    public void deleteRating(String applicationId,String expertId) { ratings.getOrDefault(applicationId,new HashMap<>()).remove(expertId); }
    public Outcome outcome() { return outcome; }
    public void outcome(Outcome value) { outcome=value; }
}
