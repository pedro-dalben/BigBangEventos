package com.pedrodalben.bigbangeventos.lifecycle;

import com.pedrodalben.bigbangeventos.api.OperationResult;
import com.pedrodalben.bigbangeventos.session.*;
import java.time.Clock;
import java.util.*;

/** The only state transition gate for sessions. */
public final class SessionLifecycle {
    private static final Map<SessionState, Set<SessionState>> NEXT = Map.of(
        SessionState.CREATED, Set.of(SessionState.REGISTRATION_OPEN, SessionState.CANCELLED),
        SessionState.REGISTRATION_OPEN, Set.of(SessionState.REGISTRATION_CLOSED, SessionState.COUNTDOWN, SessionState.CANCELLED),
        SessionState.REGISTRATION_CLOSED, Set.of(SessionState.COUNTDOWN, SessionState.CANCELLED),
        SessionState.COUNTDOWN, Set.of(SessionState.RUNNING, SessionState.REGISTRATION_OPEN, SessionState.CANCELLED),
        SessionState.RUNNING, Set.of(SessionState.PAUSED, SessionState.FINISHING, SessionState.CANCELLED, SessionState.FAILED),
        SessionState.PAUSED, Set.of(SessionState.RUNNING, SessionState.FINISHING, SessionState.CANCELLED),
        SessionState.FINISHING, Set.of(SessionState.FINISHED, SessionState.FAILED),
        SessionState.FINISHED, Set.of(), SessionState.CANCELLED, Set.of(), SessionState.FAILED, Set.of());
    private final Clock clock;
    public SessionLifecycle(Clock clock) { this.clock=clock; }
    public synchronized OperationResult transition(EventSession session, SessionState target) {
        if (!NEXT.get(session.state()).contains(target)) return OperationResult.fail("invalid_transition", session.state()+" -> "+target+" não é permitido");
        session.state(target, clock.instant()); return OperationResult.ok("Sessão alterada para "+target);
    }
    public OperationResult cancel(EventSession session, String reason) { OperationResult result=transition(session, SessionState.CANCELLED); if(result.success())session.cancelReason(reason); return result; }
}
