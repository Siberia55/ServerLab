package academy.tochkavhoda.competition.service;

import academy.tochkavhoda.competition.dao.CompetitionDao;
import academy.tochkavhoda.competition.daoimpl.CompetitionDaoImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Composition root: both services share the database of this competition. */
public final class Services {
    private final UserService users;
    private final GrantService grants;

    public Services(BigDecimal fund, BigDecimal threshold, Instant deadline, Clock clock) {
        Objects.requireNonNull(fund, "fund");
        Objects.requireNonNull(threshold, "threshold");
        Objects.requireNonNull(deadline, "deadline");
        Objects.requireNonNull(clock, "clock");
        if (fund.signum() < 0 || fund.stripTrailingZeros().scale() > 2
                || threshold.signum() < 0 || threshold.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("Неверный фонд или порог");
        }
        CompetitionDao dao = new CompetitionDaoImpl();
        users = new UserService(dao, clock, deadline, fund, threshold);
        grants = new GrantService(dao, clock, deadline, fund, threshold);
    }

    public UserService users() { return users; }
    public GrantService grants() { return grants; }
}
