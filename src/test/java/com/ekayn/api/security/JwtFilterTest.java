package com.ekayn.api.security;

import com.ekayn.api.model.User;
import com.ekayn.api.repo.RevokedTokenRepository;
import com.ekayn.api.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtFilterTest {
  private final UserRepository users = mock(UserRepository.class);
  private final RevokedTokenRepository revoked = mock(RevokedTokenRepository.class);
  private final JwtService jwt = new JwtService("01234567890123456789012345678901");

  @AfterEach void clearContext() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }

  @Test void authenticatesValidSessionCookie() throws Exception {
    User user = new User(); user.setId(UUID.randomUUID()); user.setSessionVersion(0);
    when(revoked.existsByTokenId(anyString())).thenReturn(false); when(users.findById(user.getId())).thenReturn(Optional.of(user));
    JwtFilter filter = new JwtFilter(jwt, users, revoked);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/checkout/session"); request.setCookies(new Cookie("ekayn_session", jwt.create(user.getId(), "client@test", "CUSTOMER", 0)));
    MockHttpServletResponse response = new MockHttpServletResponse(); FilterChain chain = mock(FilterChain.class);
    filter.doFilter(request, response, chain);
    assertNotNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
    assertEquals("authenticated", response.getHeader("X-Ekayn-Auth-State")); verify(chain).doFilter(request, response);
  }

  @Test void rejectsAbsentSessionCookieWithoutAuthentication() throws Exception {
    JwtFilter filter = new JwtFilter(jwt, users, revoked); MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/checkout/session"); MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, mock(FilterChain.class));
    assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()); assertEquals("missing", response.getHeader("X-Ekayn-Auth-State"));
  }

  @Test void rejectsInvalidSessionCookieWithoutAuthentication() throws Exception {
    JwtFilter filter = new JwtFilter(jwt, users, revoked); MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/checkout/session"); request.setCookies(new Cookie("ekayn_session", "not-a-jwt")); MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, mock(FilterChain.class));
    assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()); assertEquals("invalid", response.getHeader("X-Ekayn-Auth-State"));
  }
}
