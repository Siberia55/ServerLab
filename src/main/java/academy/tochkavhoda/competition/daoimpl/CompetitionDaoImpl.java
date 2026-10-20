package academy.tochkavhoda.competition.daoimpl;
import academy.tochkavhoda.competition.model.*;
import java.util.*;
import academy.tochkavhoda.competition.dao.CompetitionDao;
import academy.tochkavhoda.competition.database.Database;
public final class CompetitionDaoImpl implements CompetitionDao {
    private final Database database=new Database();
    public List<User> users() { return database.users(); }
    public void insert(User user) { database.insert(user); }
    public void session(String token,String userId) { database.session(token,userId); }
    public User user(String token) { return database.user(token); }
    public void logout(String token) { database.logout(token); }
    public void deleteUser(User user) { database.deleteUser(user); }
    public List<Application> applications() { return database.applications(); }
    public Application application(String id) { return database.application(id); }
    public void insertApplication(Application a) { database.insertApplication(a); }
    public void deleteApplication(String id) { database.deleteApplication(id); }
    public List<Rating> ratings(String id) { return database.ratings(id); }
    public void rate(Rating rating) { database.rate(rating); }
    public void deleteRating(String applicationId,String expertId) { database.deleteRating(applicationId,expertId); }
    public Outcome outcome() { return database.outcome(); }
    public void outcome(Outcome value) { database.outcome(value); }
}
