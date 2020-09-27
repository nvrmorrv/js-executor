package impl.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import impl.service.exceptions.SortParametersException;
import impl.shared.ScriptInfo;
import impl.shared.ScriptStatus;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PagingAndSortingServiceTest {
  private final ScriptInfo firstDoneScriptInfo = new ScriptInfo(
        "id1",
        ScriptStatus.DONE,
        ZonedDateTime.now(),
        Optional.of(ZonedDateTime.now().plusMinutes(1)),
        Optional.of(ZonedDateTime.now().plusMinutes(2)),
        Optional.empty(),
        Optional.empty());
  private final ScriptInfo secondDoneScriptInfo = new ScriptInfo(
        "id2",
        ScriptStatus.DONE,
        ZonedDateTime.now(),
        Optional.of(ZonedDateTime.now().plusMinutes(1)),
        Optional.of(ZonedDateTime.now().plusMinutes(2)),
        Optional.empty(),
        Optional.empty());
  private final ScriptInfo runningScriptInfo = new ScriptInfo(
        "id3",
        ScriptStatus.RUNNING,
        ZonedDateTime.now(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());

  List<ScriptInfo> scriptInfos = Arrays.asList(firstDoneScriptInfo, secondDoneScriptInfo, runningScriptInfo);

  @Test
  public void shouldPassOnSortingAsc() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
    Page<ScriptInfo> page = PagingAndSortingService.getSortedPage(scriptInfos, pageable, "ANY");
    List<ScriptInfo> sorted = page.get().collect(Collectors.toList());
    assertEquals(3, sorted.size());
    assertEquals(firstDoneScriptInfo, sorted.get(0));
    assertEquals(secondDoneScriptInfo, sorted.get(1));
    assertEquals(runningScriptInfo, sorted.get(2));
  }

  @Test
  public void shouldPassOnSortingDesc() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());
    Page<ScriptInfo> page = PagingAndSortingService.getSortedPage(scriptInfos, pageable, "ANY");
    List<ScriptInfo> sorted = page.get().collect(Collectors.toList());
    assertEquals(3, sorted.size());
    assertEquals(runningScriptInfo, sorted.get(0));
    assertEquals(secondDoneScriptInfo, sorted.get(1));
    assertEquals(firstDoneScriptInfo, sorted.get(2));
  }

  @Test
  public void shouldPassOnMultipleSorting() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("status"), Sort.Order.desc("id")));
    Page<ScriptInfo> page = PagingAndSortingService.getSortedPage(scriptInfos, pageable, "ANY");
    List<ScriptInfo> sorted = page.get().collect(Collectors.toList());
    assertEquals(3, sorted.size());
    assertEquals(runningScriptInfo, sorted.get(0));
    assertEquals(secondDoneScriptInfo, sorted.get(1));
    assertEquals(firstDoneScriptInfo, sorted.get(2));
  }

  @Test
  public void shouldFailOnSortingWithWrongField() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("field"));
    assertThatThrownBy(() -> PagingAndSortingService.getSortedPage(scriptInfos, pageable, "ANY"))
          .isInstanceOf(SortParametersException.class);
  }

  @Test
  public void shouldFailOnSortingWithWrongDirection() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("status,direction"));
    assertThatThrownBy(() -> PagingAndSortingService.getSortedPage(scriptInfos, pageable, "ANY"))
          .isInstanceOf(SortParametersException.class);
  }

  @Test
  public void shouldPassOnFiltering() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
    Page<ScriptInfo> page = PagingAndSortingService.getSortedPage(scriptInfos, pageable, "RUNNING");
    List<ScriptInfo> sorted = page.get().collect(Collectors.toList());
    assertEquals(1, sorted.size());
    assertEquals(runningScriptInfo, sorted.get(0));
    page = PagingAndSortingService.getSortedPage(scriptInfos, pageable, "NOT_FINISHED");
    sorted = page.get().collect(Collectors.toList());
    assertEquals(1, sorted.size());
    assertEquals(runningScriptInfo, sorted.get(0));
    page = PagingAndSortingService.getSortedPage(scriptInfos, pageable, "FINISHED");
    sorted = page.get().collect(Collectors.toList());
    assertEquals(2, sorted.size());
    assertEquals(firstDoneScriptInfo, sorted.get(0));
    assertEquals(secondDoneScriptInfo, sorted.get(1));
  }

  @Test
  public void shouldFailOnFilteringWithWrongField() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
    assertThatThrownBy(() -> PagingAndSortingService.getSortedPage(scriptInfos, pageable, "status"))
         .isInstanceOf(SortParametersException.class);
  }

  @Test
  public void shouldPassOnPagination() {
    Pageable pageable = PageRequest.of(0, 2, Sort.by("id"));
    Page<ScriptInfo> page = PagingAndSortingService.getSortedPage(scriptInfos, pageable, "ANY");
    List<ScriptInfo> sorted = page.get().collect(Collectors.toList());
    assertEquals(0, page.getNumber());
    assertEquals(2, page.getSize());
    assertEquals(3, page.getTotalElements());
    assertEquals(2, page.getTotalPages());
    assertTrue(page.hasNext());
    assertFalse(page.hasPrevious());
    assertEquals(firstDoneScriptInfo, sorted.get(0));
    assertEquals(secondDoneScriptInfo, sorted.get(1));
  }

  @Test
  public void shouldPassOnPaginationWhenPageSizeGreaterThanListSize() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
    Page<ScriptInfo> page = PagingAndSortingService.getSortedPage(scriptInfos, pageable, "ANY");
    List<ScriptInfo> sorted = page.get().collect(Collectors.toList());
    assertEquals(0, page.getNumber());
    assertEquals(10, page.getSize());
    assertEquals(3, page.getTotalElements());
    assertEquals(1, page.getTotalPages());
    assertFalse(page.hasNext());
    assertFalse(page.hasPrevious());
    assertEquals(firstDoneScriptInfo, sorted.get(0));
    assertEquals(secondDoneScriptInfo, sorted.get(1));
    assertEquals(runningScriptInfo, sorted.get(2));
  }

  @Test
  public void shouldPassOnPaginationWhenPageNumberGreaterThanTotalAmountOfPages() {
    Pageable pageable = PageRequest.of(1, 10, Sort.by("id"));
    Page<ScriptInfo> page = PagingAndSortingService.getSortedPage(scriptInfos, pageable, "ANY");
    List<ScriptInfo> sorted = page.get().collect(Collectors.toList());
    assertEquals(1, page.getNumber());
    assertEquals(10, page.getSize());
    assertEquals(3, page.getTotalElements());
    assertEquals(1, page.getTotalPages());
    assertFalse(page.hasNext());
    assertTrue(page.hasPrevious());
    assertEquals(0, sorted.size());
  }
}
