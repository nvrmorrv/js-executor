package impl.security;

import impl.repositories.ScriptRepository;
import impl.repositories.exceptions.UnknownIdException;
import java.util.Set;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class ScriptAccessSecurityExpressionRoot
  extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {
  private Object filterObject;
  private Object returnObject;
  private Object target;
  private final ScriptRepository repo;
  private final Set<String> adminEmails;

  public ScriptAccessSecurityExpressionRoot(Authentication auth, ScriptRepository repo, Set<String> adminEmails) {
    super(auth);
    this.adminEmails = adminEmails;
    this.repo = repo;
  }

  public void setFilterObject(Object filterObject) {
    this.filterObject = filterObject;
  }

  public Object getFilterObject() {
    return filterObject;
  }

  public void setReturnObject(Object returnObject) {
    this.returnObject = returnObject;
  }

  public Object getReturnObject() {
    return returnObject;
  }

  void setThis(Object target) {
    this.target = target;
  }

  public Object getThis() {
    return target;
  }

  public boolean isAdmin() {
    return adminEmails.contains(getPrincipalEmail());
  }

  public boolean isOwner(String scriptId) {
    try {
      return repo.getScript(scriptId).getOwner().equals(getPrincipalEmail());
    } catch (UnknownIdException ex) {
      return true;
    }
  }

  private String getPrincipalEmail() {
    return (String) ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getTokenAttributes().get("email");
  }
}
