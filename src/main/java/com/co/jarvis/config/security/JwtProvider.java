package com.co.jarvis.config.security;

import com.co.jarvis.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtProvider {

    private String SECRET_KEY = "u6Z9Dr5cQWyPm5TVbXsGQJLfzK5fdN8JeVXlD6P7gB8=";

    private HashMap<String, UserDto> listTokens = new HashMap<>();

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token).getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String numberIdentity, UserDto user) {
        Map<String, UserDto> claims = new HashMap<>();
        claims.put(numberIdentity, user);
        return createToken(claims, user);
    }

    private String createToken(Map<String, UserDto> claims, UserDto user) {
        Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10);
        SecretKey secretKey = getSecretKey();
        String tokenCreated = Jwts.builder()
                .claims(claims)
                .subject(user.getNumberIdentity())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
        listTokens.put(tokenCreated, user);
        return tokenCreated;
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public Boolean validateToken(String token, String numberIdentity) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(numberIdentity) && !isTokenExpired(token));
    }

    public UserDto getTokenUser(String token) {
        return listTokens.get(token);
    }
}
