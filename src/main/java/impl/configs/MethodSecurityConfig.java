package impl.configs;

import impl.repositories.ScriptRepository;
import impl.security.ScriptAccessSecurityExpressionHandler;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {
  private final Set<String> adminEmails;
  private final ScriptRepository repo;

  public MethodSecurityConfig( @Value("${executor.admin-emails}") Set<String> adminEmails, ScriptRepository repo) {
    this.adminEmails = adminEmails;
    this.repo = repo;
  }

  @Override
  public MethodSecurityExpressionHandler createExpressionHandler() {
    return new ScriptAccessSecurityExpressionHandler(adminEmails, repo);
  }
}
