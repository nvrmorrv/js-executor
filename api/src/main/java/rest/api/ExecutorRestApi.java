package rest.api;

import rest.api.doc.annotations.CancelExecApiEndPoint;
import rest.api.doc.annotations.DeleteExecApiEndpoint;
import rest.api.doc.annotations.ExecuteScriptApiEndpoint;
import rest.api.doc.annotations.GetExecIdsApiEndpoint;
import rest.api.doc.annotations.GetExecStatusApiEndpoint;
import rest.api.dto.ExecReq;
import rest.api.dto.ExecResp;
import rest.api.dto.ScriptId;
import rest.api.dto.ScriptListResp;

public interface ExecutorRestApi {

  @ExecuteScriptApiEndpoint
  ScriptId executeScriptAsync(ExecReq body);

  ExecResp executeScriptWithBlocking(ExecReq body);

  @GetExecStatusApiEndpoint
  ExecResp getExecutionStatus(String id);

  @CancelExecApiEndPoint
  void cancelExecution(String id);

  @DeleteExecApiEndpoint
  void deleteExecution(String id);

  @GetExecIdsApiEndpoint
  ScriptListResp getAllExecutions();
}
