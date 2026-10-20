package academy.tochkavhoda.competition.dao;
import academy.tochkavhoda.competition.model.*;
import java.util.*;
public interface CompetitionDao {
    List<User> users();
    void insert(User user);
    void session(String token,String userId);
    User user(String token);
    void logout(String token);
    void deleteUser(User user);
    List<Application> applications();
    Application application(String id);
    void insertApplication(Application a);
    void deleteApplication(String id);
    List<Rating> ratings(String id);
    void rate(Rating rating);
    void deleteRating(String applicationId,String expertId);
    Outcome outcome();
    void outcome(Outcome value);
}
