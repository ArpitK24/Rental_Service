package com.rentalService.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.net.URL;
import java.util.Date;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token.expiration:3600000}")
    private long accessTtlMs;

    private SecretKey key;

    @PostConstruct
    private void init() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            if (keyBytes.length < 32) throw new IllegalArgumentException("JWT secret must be >= 32 bytes");
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT key initialized ({} bytes)", keyBytes.length);

            try {
                URL loc = io.jsonwebtoken.Jwts.class.getProtectionDomain().getCodeSource().getLocation();
                log.info("Jwts loaded from: {}", loc);
            } catch (Throwable t) {
                log.warn("Could not determine Jwts location", t);
            }
        } catch (Exception e) {
            log.error("Failed to initialize JWT key", e);
            throw new IllegalStateException("Failed to init JWT key: " + e.getMessage(), e);
        }
    }

    public String generateAccessToken(String userId, String role, String mobile) {
        long now = System.currentTimeMillis();
        Date iat = new Date(now);
        Date exp = new Date(now + accessTtlMs);

        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .claim("mobile", mobile)
                .setIssuedAt(iat)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate token in a robust way:
     * 1) Try the simple API: Jwts.parser().setSigningKey(key).parseClaimsJws(token)
     * 2) If a NoSuchMethodError or similar occurs (weird classpath), fallback to parserBuilder via reflection.
     */
    public boolean validateToken(String token) {
        // First, the simple/normal path. This will work for the vast majority of setups.
        try {
            Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token); // throws on invalid/expired
            log.debug("JWT validated via parser()");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Illegal argument parsing JWT: {}", e.getMessage());
            return false;
        } catch (NoSuchMethodError nsme) {
            // This indicates the runtime JJWT implementation differs; fall through to reflection fallback below.
            log.warn("parser() path not available at runtime (NoSuchMethodError). Falling back to reflective parserBuilder path.");
        } catch (Throwable t) {
            // Some other runtime linkage issue — try reflective fallback but log what happened.
            log.warn("JWT validation via parser() failed unexpectedly: {}. Trying reflective fallback.", t.toString());
        }

        // Reflective fallback using parserBuilder (if available)
        try {
            // Use reflection to call: Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)
            Class<?> jwtsClass = io.jsonwebtoken.Jwts.class;
            // parserBuilder()
            java.lang.reflect.Method pb = jwtsClass.getMethod("parserBuilder");
            Object builder = pb.invoke(null);
            // setSigningKey(Key)
            java.lang.reflect.Method setSigningKey = builder.getClass().getMethod("setSigningKey", java.security.Key.class);
            Object afterSet = setSigningKey.invoke(builder, key);
            // build()
            java.lang.reflect.Method buildMethod = afterSet.getClass().getMethod("build");
            Object parser = buildMethod.invoke(afterSet);
            // parseClaimsJws(String)
            java.lang.reflect.Method parse = parser.getClass().getMethod("parseClaimsJws", String.class);
            Object jws = parse.invoke(parser, token); // will throw InvocationTargetException if invalid
            log.debug("JWT validated via reflective parserBuilder()");
            return true;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof ExpiredJwtException) {
                log.warn("JWT expired (reflective): {}", cause.getMessage());
            } else if (cause instanceof SignatureException) {
                log.warn("Invalid JWT signature (reflective): {}", cause.getMessage());
            } else if (cause instanceof MalformedJwtException) {
                log.warn("Malformed JWT (reflective): {}", cause.getMessage());
            } else if (cause instanceof UnsupportedJwtException) {
                log.warn("Unsupported JWT (reflective): {}", cause.getMessage());
            } else {
                log.warn("JWT reflective invocation error: {}", cause == null ? ite.getMessage() : cause.getMessage());
            }
            return false;
        } catch (NoSuchMethodException nsme) {
            log.warn("Reflective parserBuilder path not found: {}", nsme.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("JWT validation failed in reflective fallback: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract Claims: try the simple parser() path and fall back to reflection if needed.
     */
    public Claims extractClaims(String token) {
        // Try simple path
        try {
            return Jwts.parser()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (NoSuchMethodError nsme) {
            log.warn("parser() not available at runtime; trying reflective parserBuilder() for extractClaims.");
        } catch (JwtException e) {
            // rethrow to caller
            throw e;
        } catch (Throwable t) {
            log.warn("extractClaims via parser() failed: {}. Trying reflective fallback.", t.toString());
        }

        // Reflective fallback
        try {
            Class<?> jwtsClass = io.jsonwebtoken.Jwts.class;
            java.lang.reflect.Method pb = jwtsClass.getMethod("parserBuilder");
            Object builder = pb.invoke(null);
            java.lang.reflect.Method setSigningKey = builder.getClass().getMethod("setSigningKey", java.security.Key.class);
            Object afterSet = setSigningKey.invoke(builder, key);
            java.lang.reflect.Method buildMethod = afterSet.getClass().getMethod("build");
            Object parser = buildMethod.invoke(afterSet);
            java.lang.reflect.Method parse = parser.getClass().getMethod("parseClaimsJws", String.class);
            Object jws = parse.invoke(parser, token);
            java.lang.reflect.Method getBody = jws.getClass().getMethod("getBody");
            Object body = getBody.invoke(jws);
            return (Claims) body;
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof JwtException) throw (JwtException) cause;
            throw new JwtException("Failed to extract claims (reflective): " + ite.getMessage(), ite);
        } catch (Exception e) {
            throw new JwtException("Failed to extract claims (reflective): " + e.getMessage(), e);
        }
    }

    public String extractUserId(String token) {
        Claims c = extractClaims(token);
        return c == null ? null : c.getSubject();
    }

    public String extractRole(String token) {
        Claims c = extractClaims(token);
        return c == null ? null : c.get("role", String.class);
    }

    public String extractMobile(String token) {
        Claims c = extractClaims(token);
        return c == null ? null : c.get("mobile", String.class);
    }
}
