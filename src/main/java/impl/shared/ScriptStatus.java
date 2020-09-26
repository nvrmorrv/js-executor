package impl.shared;

import java.util.Set;

public enum ScriptStatus {
    QUEUE,
    RUNNING,
    CANCELLED,
    DONE,
    DONE_WITH_EXCEPTION;

    public static Set<ScriptStatus> FINISHED = Set.of(CANCELLED, DONE, DONE_WITH_EXCEPTION);
}
