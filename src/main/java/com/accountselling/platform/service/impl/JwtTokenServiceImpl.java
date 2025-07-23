package com.accountselling.platform.service.impl;

import com.accountselling.platform.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Implementation of JwtTokenService for handling JWT token operations.
 * Provides secure token generation, validation, and extraction functionality.
 * 
 * การใช้งาน JWT token service สำหรับการจัดการ token อย่างปลอดภัย
 */
@Slf4j
@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(new HashMap<>(), userDetails);
    }

    @Override
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        log.debug("Generating access token for user: {}", userDetails.getUsername());
        return buildToken(extraClaims, userDetails, accessTokenExpiration);
    }

    @Override
    public String generateRefreshToken(UserDetails userDetails) {
        log.debug("Generating refresh token for user: {}", userDetails.getUsername());
        return buildToken(new HashMap<>(), userDetails, refreshTokenExpiration);
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        log.debug("Token validation for user {}: {}", userDetails.getUsername(), isValid);
        return isValid;
    }

    @Override
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    @Override
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    @Override
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * Extract expiration date from token.
     * 
     * @param token the JWT token
     * @return the expiration date
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from token.
     * 
     * @param token the JWT token
     * @param claimsResolver function to extract the claim
     * @param <T> the type of the claim
     * @return the extracted claim
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Build JWT token with claims and expiration.
     * 
     * @param extraClaims additional claims to include
     * @param userDetails user details to encode
     * @param expiration token expiration time in milliseconds
     * @return the built JWT token
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        long currentTimeMillis = System.currentTimeMillis();
        
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(currentTimeMillis))
                .setExpiration(new Date(currentTimeMillis + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract all claims from token.
     * 
     * @param token the JWT token
     * @return all claims
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error extracting claims from token", e);
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /**
     * Get the signing key for JWT operations.
     * 
     * @return the signing key
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}