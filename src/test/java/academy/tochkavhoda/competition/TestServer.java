package academy.tochkavhoda.competition;
import academy.tochkavhoda.competition.server.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.math.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TestServer {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final Instant END=Instant.parse("2030-01-01T00:00:00Z");
    private static class TestClock extends Clock {
        Instant now=END.minusSeconds(1);
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return now;}
    }
    private TestClock clock;
    private Server server;
    @BeforeEach void setup(){clock=new TestClock();server=new Server(new BigDecimal("100"),new BigDecimal("3"),END,clock);}
    private JsonNode ok(ServerResponse response) throws Exception {assertEquals(200,response.getResponseCode(),response.getResponseData());return JSON.readTree(response.getResponseData());}
    private void bad(ServerResponse response) throws Exception {assertEquals(400,response.getResponseCode());assertEquals(1,JSON.readTree(response.getResponseData()).size());assertFalse(JSON.readTree(response.getResponseData()).get("error").asText().isBlank());}
    private String participant(String login) throws Exception {
        ok(server.registerParticipant(JSON.writeValueAsString(Map.of("firstName","Иван","lastName","Иванов","company",login,"login",login,"password","secret"))));return login(login);
    }
    private String expert(String login,String... dirs) throws Exception {
        ok(server.registerExpert(JSON.writeValueAsString(Map.of("firstName",login,"lastName","Петров","directions",dirs,"login",login,"password","secret"))));return login(login);
    }
    private String login(String login) throws Exception {return ok(server.login(JSON.writeValueAsString(Map.of("login",login,"password","secret")))).get("token").asText();}
    private String add(String token,String amount,String... dirs) throws Exception {
        String json=JSON.writeValueAsString(Map.of("applications",List.of(Map.of("title","Проект","description","Описание","directions",dirs,"amount",new BigDecimal(amount)))));
        return ok(server.addApplications(token,json)).get("applicationIds").get(0).asText();
    }
    private String id(String id) throws Exception {return JSON.writeValueAsString(Map.of("applicationId",id));}
    private ServerResponse rate(String token,String id,int value) throws Exception {return server.rate(token,JSON.writeValueAsString(Map.of("applicationId",id,"value",value)));}
    private JsonNode find(String token,String... dirs) throws Exception {return ok(server.findApplications(token,JSON.writeValueAsString(Map.of("directions",dirs)))).get("applications");}
    @Test void registrationSessionsAndRoles() throws Exception {
        String p=participant("firm");String e=expert("expert","математика");
        bad(server.registerParticipant("{\"firstName\":\"А\",\"lastName\":\"Б\",\"company\":\"firm\",\"login\":\"other\",\"password\":\"secret\"}"));
        bad(server.registerExpert("{\"firstName\":\"expert\",\"lastName\":\"Петров\",\"directions\":[\"физика\"],\"login\":\"other\",\"password\":\"secret\"}"));
        bad(server.login("{\"login\":\"firm\",\"password\":\"wrong!\"}"));
        bad(server.findApplications(p,"{\"directions\":[\"математика\"]}"));
        bad(server.addApplications(e,"{}"));
        ok(server.logout(p));bad(server.logout(p));String next=login("firm");assertNotEquals(p,next);bad(server.leave(p));
        ok(server.leave(next));bad(server.logout(next));bad(server.login("{\"login\":\"firm\",\"password\":\"secret\"}"));
    }
    @Test void validationAndAtomicBatch() throws Exception {
        String p=participant("firm");String e=expert("expert","math");
        for(String input:new String[]{"null","[]","{","{}","{} {}","{\"firstName\":12}"}) bad(server.registerParticipant(input));
        bad(server.registerParticipant(null));
        bad(server.addApplications(p,"{\"applications\":[{\"title\":\"ok\",\"description\":\"ok\",\"directions\":[\"math\"],\"amount\":10},null]}"));
        assertEquals(0,find(e,"math").size());
        for(String amount:new String[]{"0","-1","1.001"}) bad(server.addApplications(p,"{\"applications\":[{\"title\":\"a\",\"description\":\"b\",\"directions\":[\"math\"],\"amount\":"+amount+"}]}"));
        String a=add(p,"10","math");bad(rate(e,a,0));bad(rate(e,a,6));
        bad(server.rate(e,"{\"applicationId\":\""+a+"\",\"value\":4.5}"));bad(server.rate(e,"{\"applicationId\":\""+a+"\",\"value\":\"5\"}"));
    }
    @Test void directionsRatingUpdateDeleteAndOwnership() throws Exception {
        String p=participant("firm");String other=participant("other");String e=expert("expert"," MATH ","physics");String outsider=expert("outsider","art");
        String a=add(p,"10","math","art");ok(rate(e,a,2));ok(rate(e,a,5));
        JsonNode list=find(e,"math","physics");assertEquals(1,list.size());assertEquals(5,list.get(0).get("ownRating").asInt());
        assertEquals(1,ok(server.getRatedApplications(e)).get("applications").size());
        bad(server.findApplications(e,"{\"directions\":[\"art\"]}"));
        String b=add(p,"20","physics");bad(rate(outsider,b,5));bad(server.cancelApplication(other,id(a)));bad(server.deleteRating(outsider,id(b)));
        ok(server.deleteRating(e,id(a)));assertEquals(0,ok(server.getRatedApplications(e)).get("applications").size());
        ok(rate(e,a,4));ok(server.cancelApplication(p,id(a)));assertEquals(0,ok(server.getRatedApplications(e)).get("applications").size());bad(rate(e,a,4));
    }
    @Test void leaveCascadesAndInvalidatesEverySession() throws Exception {
        String p=participant("firm"),e=expert("expert","math"),other=expert("second","math");
        String a=add(p,"50","math");ok(rate(e,a,5));ok(rate(other,a,1));String e2=login("expert");
        ok(server.leave(e));bad(server.getRatedApplications(e2));assertEquals(1,find(other,"math").get(0).get("average").asInt());
        ok(server.leave(p));assertEquals(0,find(other,"math").size());
    }
    @Test void rankingBudgetSkipThresholdAndNoRatings() throws Exception {
        String p=participant("firm"),e=expert("expert","math");
        String expensive=add(p,"101","math"),large=add(p,"80","math"),small=add(p,"30","math"),low=add(p,"1","math"),unrated=add(p,"1","math");
        ok(rate(e,expensive,5));ok(rate(e,large,4));ok(rate(e,small,4));ok(rate(e,low,3));
        bad(server.getResults(e));clock.now=END;
        JsonNode result=ok(server.getResults(e));assertEquals(1,result.get("awards").size());assertEquals(small,result.get("awards").get(0).get("applicationId").asText());assertEquals(70,result.get("remaining").asInt());
        bad(rate(e,unrated,5));bad(server.deleteRating(e,id(small)));bad(server.cancelApplication(p,id(small)));bad(server.addApplications(p,"{}"));
        ok(server.leave(p));assertEquals(result,ok(server.getResults(e)));
    }
    @Test void averagesUseAllExpertsAndConsumeExactBudget() throws Exception {
        String p=participant("firm"),e=expert("expert","math"),e2=expert("second","math");
        String a=add(p,"60","math"),b=add(p,"40","math");ok(rate(e,a,5));ok(rate(e2,a,3));ok(rate(e,b,4));
        clock.now=END;JsonNode result=ok(server.getResults(p));assertEquals(2,result.get("awards").size());assertEquals(b,result.get("awards").get(0).get("applicationId").asText());assertEquals(0,result.get("remaining").asInt());
    }
    @Test void equalTiesRemainStable() throws Exception {
        String p=participant("firm"),e=expert("expert","math");
        for(int i=0;i<5;i++)ok(rate(e,add(p,"60","math"),5));
        clock.now=END;JsonNode result=ok(server.getResults(e));assertEquals(1,result.get("awards").size());for(int i=0;i<10;i++)assertEquals(result,ok(server.getResults(e)));
    }
    @Test void emptyCompetitionAndInvalidToken() throws Exception {
        String p=participant("firm");bad(server.getRatedApplications("unknown"));bad(server.leave(null));clock.now=END;
        JsonNode r=ok(server.getResults(p));assertEquals(0,r.get("awards").size());assertEquals(100,r.get("remaining").asInt());
    }
}
