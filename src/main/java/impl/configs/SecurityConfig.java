package impl.configs;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

@Configuration
@AllArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  private final SecurityProblemSupport unauthorizedResponse;

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http
          .authorizeRequests(authReq ->
                authReq
                      .antMatchers("/", "/logout", "/actuator/**", "/swagger-ui/**").permitAll()
                      .anyRequest().authenticated())
          .oauth2ResourceServer()
            .accessDeniedHandler(unauthorizedResponse)
            .jwt();
  }
}
