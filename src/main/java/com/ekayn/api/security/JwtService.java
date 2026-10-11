package com.ekayn.api.security;
import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.util.*;
@Service public class JwtService {
  private final SecretKey key; public JwtService(@Value("${app.jwt-secret}") String secret) { if(secret==null||secret.length()<32||secret.startsWith("change-me")||secret.startsWith("replace"))throw new IllegalStateException("JWT_SECRET doit être défini avec au moins 32 caractères aléatoires."); key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }
  public String create(UUID id, String email, String role, long sessionVersion) { return Jwts.builder().id(UUID.randomUUID().toString()).subject(id.toString()).claim("email",email).claim("role",role).claim("sessionVersion",sessionVersion).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+86400000)).signWith(key).compact(); }
  public Jws<Claims> parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token); }
}
