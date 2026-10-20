package academy.tochkavhoda.competition.service;
import academy.tochkavhoda.competition.dao.*;
import academy.tochkavhoda.competition.model.*;
import academy.tochkavhoda.competition.dto.request.*;
import academy.tochkavhoda.competition.dto.response.*;
import academy.tochkavhoda.competition.server.ServerResponse;
import java.util.*;
import java.math.*;
import java.time.*;
public final class GrantService extends ServiceSupport {
    public GrantService(CompetitionDao dao,Clock clock,Instant deadline,BigDecimal fund,BigDecimal threshold) { super(dao,clock,deadline,fund,threshold); }
    public ServerResponse addApplications(String token,String input) { return execute(()-> {
        User u=role(token,false); open(); ApplicationsRequest r=read(input,ApplicationsRequest.class);
        require(r.applications()!=null && !r.applications().isEmpty(),"Список заявок пуст");
        List<Application> applications=new ArrayList<>();
        for(ApplicationRequest a:r.applications()) {
            require(a!=null,"Заявка не должна быть null");
            require(a.amount()!=null && a.amount().signum()>0 && a.amount().stripTrailingZeros().scale()<=2,"Сумма должна быть положительной, не более двух знаков после запятой");
            applications.add(new Application(UUID.randomUUID().toString(),u.id(),text(a.title(),"название"),text(a.description(),"описание"),directions(a.directions()),a.amount()));
        }
        applications.forEach(dao::insertApplication);
        return new IdsResponse(applications.stream().map(Application::id).toList());
    }); }
    public ServerResponse cancelApplication(String token,String input) { return execute(()-> {
        User u=role(token,false); open(); Application a=application(read(input,IdRequest.class).applicationId());
        require(a.participantId().equals(u.id()),"Нельзя отменить чужую заявку"); dao.deleteApplication(a.id()); return Map.of();
    }); }
    public ServerResponse rate(String token,String input) { return execute(()-> {
        User u=role(token,true); open(); RatingRequest r=read(input,RatingRequest.class); Application a=application(r.applicationId());
        require(!Collections.disjoint(a.directions(),u.directions()),"Заявка вне направлений эксперта");
        require(r.value()!=null && r.value()>=1 && r.value()<=5,"Оценка должна быть целым числом от 1 до 5");
        dao.rate(new Rating(u.id(),a.id(),r.value())); return Map.of();
    }); }
    public ServerResponse deleteRating(String token,String input) { return execute(()-> {
        User u=role(token,true); open(); Application a=application(read(input,IdRequest.class).applicationId());
        require(dao.ratings(a.id()).stream().anyMatch(r->r.expertId().equals(u.id())),"Ваша оценка не найдена");
        dao.deleteRating(a.id(),u.id()); return Map.of();
    }); }
    private ApplicationResponse view(Application a,User u) {
        Integer own=dao.ratings(a.id()).stream().filter(r->r.expertId().equals(u.id())).map(Rating::value).findFirst().orElse(null);
        return new ApplicationResponse(a.id(),a.participantId(),a.title(),a.description(),a.directions(),a.amount(),average(a),own);
    }
    public ServerResponse findApplications(String token,String input) { return execute(()-> {
        User u=role(token,true); Set<String> dirs=directions(read(input,DirectionsRequest.class).directions());
        require(u.directions().containsAll(dirs),"Запрошены направления вне компетенции эксперта");
        return new ApplicationsResponse(dao.applications().stream().filter(a->!Collections.disjoint(a.directions(),dirs)).map(a->view(a,u)).toList());
    }); }
    public ServerResponse getRatedApplications(String token) { return execute(()-> {
        User u=role(token,true);
        return new ApplicationsResponse(dao.applications().stream().filter(a->dao.ratings(a.id()).stream().anyMatch(r->r.expertId().equals(u.id()))).map(a->view(a,u)).toList());
    }); }
    public ServerResponse getResults(String token) { return execute(()-> {
        user(token); require(!clock.instant().isBefore(deadline),"Конкурс еще не завершен"); Outcome o=dao.outcome();
        return new ResultsResponse(o.awards().stream().map(a->new AwardResponse(a.applicationId(),a.amount(),a.average())).toList(),o.remaining());
    }); }
}
