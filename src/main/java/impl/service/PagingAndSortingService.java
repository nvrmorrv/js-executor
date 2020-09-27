package impl.service;

import impl.service.exceptions.PaginationException;
import impl.shared.ScriptInfo;
import impl.service.exceptions.SortParametersException;
import impl.shared.ScriptStatus;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PagingAndSortingService {

  public static Page<ScriptInfo> getSortedPage(List<ScriptInfo> list, Pageable pageable, String filterStatus) {
    List<ScriptInfo> sorted = sort(list, pageable.getSort(), filterStatus);
    return getPage(sorted, pageable);
  }

  private static Page<ScriptInfo> getPage(List<ScriptInfo> list, Pageable pageable) {
    int pageNumber = pageable.getPageNumber();
    int pageSize = (pageable.getPageSize() > 0 && pageable.getPageSize() < list.size())
          ? pageable.getPageSize() : list.size();
    List<List<ScriptInfo>> pages = getPageLists(list, pageSize);
    if(pageNumber > pages.size() - 1) {
      throw new PaginationException("Requested page does not exist");
    }
    return new PageImpl<>(pages.get(pageNumber), pageable, list.size());
  }

  public static List<List<ScriptInfo>> getPageLists(List<ScriptInfo> list, int pageSize) {
    int numPages = (int) Math.ceil((double)list.size() / (double)pageSize);
    return IntStream.range(0, numPages)
          .mapToObj(pageNum ->
                list.subList(pageNum * pageSize, Math.min((pageNum + 1) * pageSize, list.size())))
          .collect(Collectors.toList());
  }

  private static List<ScriptInfo> sort(List<ScriptInfo> list, Sort sort, String filterStatus) {
    Predicate<ScriptInfo> statusFilter = getStatusPredicate(filterStatus);
    Optional<Comparator<ScriptInfo>> comparator = sort.stream()
          .map(PagingAndSortingService::getComparator)
          .reduce(Comparator::thenComparing);
    Stream<ScriptInfo> sorted = comparator.map(comp -> list.stream().sorted(comp)).orElseGet(list::stream);
    return  sorted.filter(statusFilter).collect(Collectors.toList());
  }

  private static Comparator<ScriptInfo> getComparator(Sort.Order order) {
    ZonedDateTime latestTime = ZonedDateTime.now(ZoneOffset.of("+14:00"));
    String property = order.getProperty();
    Comparator<ScriptInfo> comparator;
    switch (property) {
      case "id":
        comparator = Comparator.comparing(ScriptInfo::getId);
        break;
      case "status":
        comparator = Comparator.comparing(ScriptInfo::getStatus);
        break;
      case "createTime":
        comparator = Comparator.comparing(ScriptInfo::getCreateTime);
        break;
      case "startTime":
        comparator = Comparator.comparing(info -> info.getStartTime().orElse(latestTime));
        break;
      case "finishTime":
        comparator = Comparator.comparing(info -> info.getFinishTime().orElse(latestTime));
        break;
      default:
        throw new SortParametersException("Unexpected sorting field was passed, field: " + property);
    }
    return order.isAscending() ? comparator : comparator.reversed();
  }

  private static Predicate<ScriptInfo> getStatusPredicate(String filterStatus) {
    switch (filterStatus) {
      case "QUEUE":
        return i -> i.getStatus() == ScriptStatus.QUEUE;
      case "RUNNING":
        return i -> i.getStatus() == ScriptStatus.RUNNING;
      case "CANCELLED":
        return i -> i.getStatus() == ScriptStatus.CANCELLED;
      case "DONE":
        return i -> i.getStatus() == ScriptStatus.DONE;
      case "DONE_WITH_EXCEPTION":
        return i -> i.getStatus() == ScriptStatus.DONE_WITH_EXCEPTION;
      case "FINISHED" :
        return i -> ScriptStatus.FINISHED.contains(i.getStatus());
      case "NOT_FINISHED" :
        return i -> !ScriptStatus.FINISHED.contains(i.getStatus());
      case "ANY":
        return i -> true;
      default:
        throw new SortParametersException("Unexpected filtering status was passed, status: " + filterStatus);
    }
  }
}
