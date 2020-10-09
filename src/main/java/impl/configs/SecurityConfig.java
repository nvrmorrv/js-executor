package impl.configs;

import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http
         // .httpBasic().disable()
          .antMatcher("/**").authorizeRequests()
          .antMatchers("/", "/logout", "/scripts", "/actuator/**", "/swagger-ui/**").permitAll()
         // .antMatchers("/login").denyAll()
          .anyRequest().authenticated()
          .and()
//          .logout(logout -> logout
//                .permitAll()
//                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_OK)))
//         // .deleteCookies("")
////          .addLogoutHandler((httpServletRequest, httpServletResponse, authentication) ->
////                httpServletResponse.setStatus(HttpServletResponse.SC_OK))
          .oauth2Login();
  }
}
