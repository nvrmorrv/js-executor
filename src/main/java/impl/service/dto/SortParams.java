package impl.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SortParams {
  private final String sortField;
  private final String sortOrder;
  private final String status;
}
