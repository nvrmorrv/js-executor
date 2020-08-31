package rest.api;

import rest.api.doc.annotations.CancelExecApiEndPoint;
import rest.api.doc.annotations.DeleteExecApiEndpoint;
import rest.api.doc.annotations.ExecuteScriptApiEndpoint;
import rest.api.doc.annotations.GetAllExecIdsApiEndpoint;
import rest.api.doc.annotations.GetExecStatusApiEndpoint;
import rest.api.doc.annotations.GetFinishedExecIdsApiEndpoint;
import rest.api.dto.BlockingExecResp;
import rest.api.dto.ExecReq;
import rest.api.dto.ScriptId;
import rest.api.dto.ScriptListResp;
import rest.api.dto.StatusResp;

public interface ExecutorRestApi {

  @ExecuteScriptApiEndpoint
  ScriptId executeScriptAsync(ExecReq body);

  BlockingExecResp executeScriptWithBlocking(ExecReq body);

  @GetExecStatusApiEndpoint
  StatusResp getExecutionStatus(String id);

  @CancelExecApiEndPoint
  void cancelExecution(String id);

  @DeleteExecApiEndpoint
  void deleteExecution(String id);

  @GetFinishedExecIdsApiEndpoint
  ScriptListResp getFinishedExecutions();

  @GetAllExecIdsApiEndpoint
  ScriptListResp getAllExecutions();
}
