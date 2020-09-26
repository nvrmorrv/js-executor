package impl.controllers.utils;

import static impl.controllers.utils.ResponseMapper.getCommonStatusResp;

import impl.controllers.dto.CommonStatusResp;
import impl.shared.ScriptInfo;
import java.util.function.Function;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class CommonStatusRespAssembler implements RepresentationModelAssembler<ScriptInfo, CommonStatusResp> {
  private final Function<String, Link> selfLinkProvider;

  public CommonStatusRespAssembler(Function<String, Link> selfLinkProvider) {
    this.selfLinkProvider = selfLinkProvider;
  }

  @Override
  public CommonStatusResp toModel(ScriptInfo info) {
    CommonStatusResp resp = getCommonStatusResp(info);
    resp.add(selfLinkProvider.apply(info.getId()));
    return resp;
  }
}
