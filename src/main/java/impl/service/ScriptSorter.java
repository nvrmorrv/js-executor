package impl.service;

import impl.shared.ScriptInfo;
import impl.service.dto.SortParams;
import impl.service.exceptions.SortParametersException;
import impl.shared.ExecStatus;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ScriptSorter {

  public static List<ScriptInfo> sort(List<ScriptInfo> list, SortParams sortParams) {
    Predicate<ScriptInfo> filter = getPredicate(sortParams.getStatus());
    Comparator<ScriptInfo> comparator = getComparator(sortParams.getSortField());
    List<ScriptInfo> sorted = list.stream()
          .filter(filter)
          .sorted(comparator)
          .collect(Collectors.toList());
    changeOrder(sorted, sortParams.getSortOrder());
    return sorted;
  }

  private static Comparator<ScriptInfo> getComparator(String sortField) {
    ZonedDateTime latestTime = ZonedDateTime.now(ZoneOffset.of("+14:00"));
    switch (sortField) {
      case "id":
        return Comparator.comparing(ScriptInfo::getId);
      case "create-time":
        return Comparator.comparing(ScriptInfo::getCreateTime);
      case "start-time":
        return Comparator.comparing(info -> info.getStartTime().orElse(latestTime));
      case "finish-time":
        return Comparator.comparing(info -> info.getFinishTime().orElse(latestTime));
      default:
        throw new SortParametersException("Unexpected sorting field was passed, field: " + sortField);
    }
  }

  private static Predicate<ScriptInfo> getPredicate(String filterStatus) {
    switch (filterStatus) {
      case "queue":
        return i -> i.getStatus() == ExecStatus.QUEUE;
      case "running":
        return i -> i.getStatus() == ExecStatus.RUNNING;
      case "cancelled":
        return i -> i.getStatus() == ExecStatus.CANCELLED;
      case "done":
        return i -> i.getStatus() == ExecStatus.DONE;
      case "done-with-exception":
        return i -> i.getStatus() == ExecStatus.DONE_WITH_EXCEPTION;
      case "finished" :
        return i -> ExecStatus.FINISHED.contains(i.getStatus());
      case "any":
        return i -> true;
      default:
        throw new SortParametersException("Unexpected filtering status was passed, status: " + filterStatus);
    }
  }

  private static void changeOrder(List<ScriptInfo> list, String order) {
    switch (order) {
      case "asc":
        break;
      case "desc":
        Collections.reverse(list);
        break;
      default:
        throw new SortParametersException("Unexpected sorting order was passed, order: " + order);
    }
  }
}
