package impl.controllers.utils;

import static impl.controllers.utils.ResponseMapper.getCommonStatusResp;

import impl.controllers.dto.CommonScriptResp;
import impl.shared.ScriptInfo;
import java.util.function.Function;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class CommonStatusRespAssembler implements RepresentationModelAssembler<ScriptInfo, CommonScriptResp> {
  private final Function<String, Link> selfLinkProvider;

  public CommonStatusRespAssembler(Function<String, Link> selfLinkProvider) {
    this.selfLinkProvider = selfLinkProvider;
  }

  @Override
  public CommonScriptResp toModel(ScriptInfo info) {
    CommonScriptResp resp = getCommonStatusResp(info);
    resp.add(selfLinkProvider.apply(info.getId()));
    return resp;
  }
}
