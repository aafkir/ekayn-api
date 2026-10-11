package com.ekayn.api.security;
import com.ekayn.api.repo.*; import jakarta.servlet.*; import jakarta.servlet.http.*; import org.slf4j.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.boot.web.servlet.FilterRegistrationBean; import org.springframework.context.annotation.*; import org.springframework.http.HttpMethod; import org.springframework.security.authentication.*; import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.config.http.SessionCreationPolicy; import org.springframework.security.core.authority.SimpleGrantedAuthority; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.*; import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; import org.springframework.web.filter.OncePerRequestFilter; import org.springframework.stereotype.Component; import java.io.IOException; import java.time.Instant; import java.util.*;
@Configuration @EnableMethodSecurity public class SecurityConfig {
  static void jsonError(HttpServletResponse response,int status,String code,String message)throws IOException{response.setStatus(status);response.setContentType("application/json");response.getWriter().write("{\"code\":\""+code+"\",\"error\":\""+message+"\"}");}
  @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
  @Bean JwtFilter jwtFilter(JwtService jwt, UserRepository users, RevokedTokenRepository revoked){return new JwtFilter(jwt,users,revoked);}
  @Bean CsrfCookieFilter csrfCookieFilter(){return new CsrfCookieFilter();}
  @Bean FilterRegistrationBean<JwtFilter> jwtFilterServletRegistration(JwtFilter filter){var registration=new FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;}
  @Bean FilterRegistrationBean<CsrfCookieFilter> csrfFilterServletRegistration(CsrfCookieFilter filter){var registration=new FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;}
  @Bean SecurityFilterChain filter(HttpSecurity http, JwtFilter jwt, CsrfCookieFilter csrf, @org.springframework.beans.factory.annotation.Qualifier("cors") org.springframework.web.cors.CorsConfigurationSource corsSource) throws Exception { return http.csrf(c->c.disable()).cors(c->c.configurationSource(corsSource)).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->jsonError(res,401,"AUTHENTICATION_REQUIRED","Authentification requise.")).accessDeniedHandler((req,res,ex)->jsonError(res,403,"ACCESS_DENIED","Accès refusé : session invalide ou droits insuffisants."))).authorizeHttpRequests(a->a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll().requestMatchers("/api/auth/**","/api/stripe/webhook","/actuator/health","/swagger-ui/**","/v3/api-docs/**","/swagger-ui.html").permitAll().anyRequest().authenticated()).addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).addFilterAfter(csrf, JwtFilter.class).build(); }
  @Bean org.springframework.web.cors.CorsConfigurationSource cors(@Value("${app.frontend-urls}") String frontendUrls){var c=new org.springframework.web.cors.CorsConfiguration();var origins=new ArrayList<>(Arrays.stream(frontendUrls.split(",")).map(String::trim).filter(s->!s.isBlank()).toList());origins.add("http://localhost:[*]");origins.add("http://127.0.0.1:[*]");c.setAllowedOriginPatterns(origins);c.setAllowedMethods(List.of("GET","POST","PATCH","DELETE","PUT","OPTIONS"));c.setAllowedHeaders(List.of("Content-Type","Authorization","Idempotency-Key","X-CSRF-Token"));c.setAllowCredentials(true);var s=new org.springframework.web.cors.UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;}
}
class JwtFilter extends OncePerRequestFilter {
  private static final Logger log=LoggerFactory.getLogger(JwtFilter.class);
  private final JwtService jwt; private final UserRepository users; private final RevokedTokenRepository revoked; JwtFilter(JwtService j, UserRepository u, RevokedTokenRepository r){jwt=j;users=u;revoked=r;}
  protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
    String h=req.getHeader("Authorization");
    if(h==null){var cookie=req.getCookies();if(cookie!=null)for(var c:cookie)if(c.getName().equals("ekayn_session")){h="Bearer "+c.getValue();break;}}
    String state="missing";
    if(h!=null&&h.startsWith("Bearer ")) try {
      var c=jwt.parse(h.substring(7));
      if(revoked.existsByTokenId(c.getPayload().getId())) state="revoked";
      else {
        var u=users.findById(UUID.fromString(c.getPayload().getSubject()));
        var claim=c.getPayload().get("sessionVersion");
        long tokenVersion=claim instanceof Number n?n.longValue():Long.parseLong(String.valueOf(claim));
        if(u.isEmpty()) state="user-not-found";
        else if(tokenVersion!=u.get().getSessionVersion()) state="session-version-mismatch";
        else { var context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(new UsernamePasswordAuthenticationToken(u.get(),null,List.of(new SimpleGrantedAuthority("ROLE_"+u.get().getRole())))); SecurityContextHolder.setContext(context); state="authenticated"; }
      }
    } catch(Exception ignored){state="invalid";}
    if(req.getRequestURI().equals("/api/checkout/session")){res.setHeader("X-Ekayn-Auth-State",state);log.debug("JWT checkout auth state={}, cookiePresent={}, authorizationPresent={}",state,req.getCookies()!=null&&Arrays.stream(req.getCookies()).anyMatch(c->c.getName().equals("ekayn_session")),req.getHeader("Authorization")!=null);}
    chain.doFilter(req,res);
  }
}
class CsrfCookieFilter extends OncePerRequestFilter { protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {if(Set.of("POST","PATCH","DELETE","PUT").contains(req.getMethod())&&!req.getRequestURI().equals("/api/stripe/webhook")){String session=null,csrf=null;for(var c:Optional.ofNullable(req.getCookies()).orElse(new Cookie[0])){if(c.getName().equals("ekayn_session"))session=c.getValue();if(c.getName().equals("ekayn_csrf"))csrf=c.getValue();}String header=req.getHeader("X-CSRF-Token");if(session!=null){if(Objects.requireNonNullElse(header,"").isBlank()){SecurityConfig.jsonError(res,403,"CSRF_TOKEN_INVALID","CSRF token invalide.");return;}if(!Objects.equals(csrf,header))res.addHeader("Set-Cookie","ekayn_csrf="+header+"; Path=/; Max-Age=86400; SameSite=Lax");}}chain.doFilter(req,res);} }
