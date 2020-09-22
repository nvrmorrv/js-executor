package impl.repositories.entities;

import java.util.Set;

public enum ExecStatus {
    QUEUE,
    RUNNING,
    CANCELLED,
    DONE,
    DONE_WITH_EXCEPTION;

    public static Set<ExecStatus> FINISHED = Set.of(CANCELLED, DONE, DONE_WITH_EXCEPTION);
}
