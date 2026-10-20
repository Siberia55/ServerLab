package academy.tochkavhoda.competition.service;
import academy.tochkavhoda.competition.dao.*;
import academy.tochkavhoda.competition.model.*;
import academy.tochkavhoda.competition.dto.request.*;
import academy.tochkavhoda.competition.dto.response.*;
import academy.tochkavhoda.competition.server.ServerResponse;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;
public final class UserService extends ServiceSupport {
    public UserService(CompetitionDao dao,Clock clock,Instant deadline,BigDecimal fund,BigDecimal threshold) { super(dao,clock,deadline,fund,threshold); }
    private User create(String first,String last,String company,Set<String> dirs,String login,String password,boolean expert) {
        first=text(first,"имя"); last=text(last,"фамилия"); login=text(login,"логин");
        require(login.length()>=3,"Логин должен содержать не менее 3 символов");
        require(password!=null && !password.isBlank() && password.length()>=6,"Пароль должен содержать не менее 6 символов");
        return new User(UUID.randomUUID().toString(),first,last,company,dirs,login,password,expert);
    }
    public ServerResponse registerParticipant(String input) { return execute(()-> {
        RegisterParticipantRequest r=read(input,RegisterParticipantRequest.class);
        dao.insert(create(r.firstName(),r.lastName(),text(r.company(),"фирма"),Set.of(),r.login(),r.password(),false)); return Map.of();
    }); }
    public ServerResponse registerExpert(String input) { return execute(()-> {
        RegisterExpertRequest r=read(input,RegisterExpertRequest.class);
        dao.insert(create(r.firstName(),r.lastName(),null,directions(r.directions()),r.login(),r.password(),true)); return Map.of();
    }); }
    public ServerResponse login(String input) { return execute(()-> {
        LoginRequest r=read(input,LoginRequest.class);
        String login=text(r.login(),"логин"); text(r.password(),"пароль");
        User u=dao.users().stream().filter(v->v.login().equals(login)&&v.password().equals(r.password())).findFirst().orElse(null);
        require(u!=null,"Неверный логин или пароль");
        String token=UUID.randomUUID().toString(); dao.session(token,u.id()); return new TokenResponse(token);
    }); }
    public ServerResponse logout(String token) { return execute(()-> { user(token); dao.logout(token); return Map.of(); }); }
    public ServerResponse leave(String token) { return execute(()-> { dao.deleteUser(user(token)); return Map.of(); }); }
}
