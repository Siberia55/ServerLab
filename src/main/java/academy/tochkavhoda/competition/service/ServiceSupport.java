package academy.tochkavhoda.competition.service;
import academy.tochkavhoda.competition.dao.*;
import academy.tochkavhoda.competition.model.*;
import academy.tochkavhoda.competition.dto.response.*;
import academy.tochkavhoda.competition.server.ServerResponse;
import academy.tochkavhoda.competition.exception.CompetitionException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
abstract class ServiceSupport {
    protected final CompetitionDao dao;
    protected final Clock clock;
    protected final Instant deadline;
    protected final BigDecimal fund, threshold;
    private final ObjectMapper json=JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
    ServiceSupport(CompetitionDao dao,Clock clock,Instant deadline,BigDecimal fund,BigDecimal threshold) {
        this.dao=dao; this.clock=clock; this.deadline=deadline; this.fund=fund; this.threshold=threshold;
    }
    protected ServerResponse execute(Supplier<Object> action) {
        synchronized(dao) {
            try {
                freeze();
                return new ServerResponse(200,json.writeValueAsString(action.get()));
            } catch(CompetitionException e) { return error(e.getMessage()); }
              catch(com.fasterxml.jackson.core.JsonProcessingException e) { return error("Ошибка формирования ответа"); }
        }
    }
    private ServerResponse error(String message) {
        try { return new ServerResponse(400,json.writeValueAsString(new ErrorResponse(message))); }
        catch(Exception e) { throw new IllegalStateException(e); }
    }
    protected <T> T read(String input,Class<T> type) {
        try { T value=json.readValue(input,type); require(value!=null,"Запрос не должен быть null"); return value; }
        catch(com.fasterxml.jackson.core.JsonProcessingException|IllegalArgumentException e) { throw new CompetitionException("Некорректный JSON или тип поля"); }
    }
    protected static void require(boolean condition,String error) { if(!condition) throw new CompetitionException(error); }
    protected static String text(String value,String field) {
        require(value!=null && !value.isBlank(),"Поле «"+field+"» обязательно"); return value.strip();
    }
    protected static Set<String> directions(Set<String> input) {
        require(input!=null && !input.isEmpty(),"Нужен хотя бы один пункт направлений");
        Set<String> result=new LinkedHashSet<>();
        input.forEach(s->result.add(text(s,"направление").toLowerCase(Locale.ROOT)));
        return Collections.unmodifiableSet(result);
    }
    protected User user(String token) { User u=token==null?null:dao.user(token); require(u!=null,"Недействительный токен"); return u; }
    protected User role(String token,boolean expert) { User u=user(token); require(u.expert()==expert,"Операция недоступна для этой роли"); return u; }
    protected void open() { require(clock.instant().isBefore(deadline),"Прием заявок и оценок завершен"); }
    protected Application application(String id) { Application a=dao.application(text(id,"applicationId")); require(a!=null,"Заявка не найдена"); return a; }
    protected BigDecimal average(Application a) {
        List<Rating> ratings=dao.ratings(a.id());
        return ratings.isEmpty()?BigDecimal.ZERO:BigDecimal.valueOf(ratings.stream().mapToLong(Rating::value).sum()).divide(BigDecimal.valueOf(ratings.size()),12,RoundingMode.HALF_UP).stripTrailingZeros();
    }
    // Compare rational averages exactly, including comparison against the threshold.
    private long sum(Application a) { return dao.ratings(a.id()).stream().mapToLong(Rating::value).sum(); }
    private int compareAverage(Application a,Application b) {
        return BigInteger.valueOf(sum(a)).multiply(BigInteger.valueOf(dao.ratings(b.id()).size()))
            .compareTo(BigInteger.valueOf(sum(b)).multiply(BigInteger.valueOf(dao.ratings(a.id()).size())));
    }
    private void freeze() {
        if(clock.instant().isBefore(deadline) || dao.outcome()!=null) return;
        List<Application> ranked=new ArrayList<>(dao.applications().stream().filter(a->!dao.ratings(a.id()).isEmpty()).toList());
        Collections.shuffle(ranked);
        ranked.sort((a,b)-> { int comparison=compareAverage(b,a); return comparison!=0?comparison:a.amount().compareTo(b.amount()); });
        BigDecimal remaining=fund;
        List<Award> awards=new ArrayList<>();
        for(Application a:ranked) {
            if(BigDecimal.valueOf(sum(a)).compareTo(threshold.multiply(BigDecimal.valueOf(dao.ratings(a.id()).size())))<=0) break;
            if(a.amount().compareTo(remaining)<=0) { awards.add(new Award(a.id(),a.amount(),average(a))); remaining=remaining.subtract(a.amount()); }
        }
        dao.outcome(new Outcome(List.copyOf(awards),remaining));
    }
}
