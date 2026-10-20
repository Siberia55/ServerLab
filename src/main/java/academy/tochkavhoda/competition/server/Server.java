package academy.tochkavhoda.competition.server;
import academy.tochkavhoda.competition.service.*;
import java.math.BigDecimal;
import java.time.*;
/** Direct-call JSON API; no HTTP or console client. */
public final class Server {
    private final UserService users;
    private final GrantService grants;
    public Server(BigDecimal fund,BigDecimal threshold,Instant deadline) { this(fund,threshold,deadline,Clock.systemUTC()); }
    public Server(BigDecimal fund,BigDecimal threshold,Instant deadline,Clock clock) {
        Services services = new Services(fund, threshold, deadline, clock);
        users = services.users();
        grants = services.grants();
    }
    public ServerResponse registerParticipant(String json) { return users.registerParticipant(json); }
    public ServerResponse registerExpert(String json) { return users.registerExpert(json); }
    public ServerResponse login(String json) { return users.login(json); }
    public ServerResponse logout(String token) { return users.logout(token); }
    public ServerResponse leave(String token) { return users.leave(token); }
    public ServerResponse addApplications(String token,String json) { return grants.addApplications(token,json); }
    public ServerResponse cancelApplication(String token,String json) { return grants.cancelApplication(token,json); }
    public ServerResponse rate(String token,String json) { return grants.rate(token,json); }
    public ServerResponse deleteRating(String token,String json) { return grants.deleteRating(token,json); }
    public ServerResponse findApplications(String token,String json) { return grants.findApplications(token,json); }
    public ServerResponse getRatedApplications(String token) { return grants.getRatedApplications(token); }
    public ServerResponse getResults(String token) { return grants.getResults(token); }
}
