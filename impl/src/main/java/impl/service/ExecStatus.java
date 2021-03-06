package impl.service;

import java.util.Set;

public enum ExecStatus {
    QUEUE,
    RUNNING,
    CANCELLED,
    DONE,
    DONE_WITH_EXCEPTION,
    DONE_WITH_SYNTAX_ERROR;

    public static final Set<ExecStatus> FINISHED =
          Set.of(ExecStatus.CANCELLED,
                ExecStatus.DONE,
                ExecStatus.DONE_WITH_EXCEPTION,
                ExecStatus.DONE_WITH_SYNTAX_ERROR);
}
