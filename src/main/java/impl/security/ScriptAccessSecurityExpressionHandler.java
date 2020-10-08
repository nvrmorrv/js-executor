package impl.security;

import impl.repositories.ScriptRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

@RequiredArgsConstructor
public class ScriptAccessSecurityExpressionHandler
      extends DefaultMethodSecurityExpressionHandler {
  private final Set<String> adminEmails;
  private final ScriptRepository repo;
  private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

  @Override
  protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
        Authentication authentication, MethodInvocation invocation) {
    ScriptAccessSecurityExpressionRoot root =
          new ScriptAccessSecurityExpressionRoot(authentication, repo, adminEmails);
    root.setPermissionEvaluator(getPermissionEvaluator());
    root.setTrustResolver(this.trustResolver);
    root.setRoleHierarchy(getRoleHierarchy());
    return root;
  }
}
