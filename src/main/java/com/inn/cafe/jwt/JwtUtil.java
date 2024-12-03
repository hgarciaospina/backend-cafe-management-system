package com.inn.cafe.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.security.SignatureException;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // Eliminamos la clave secreta débil y utilizamos una generada de manera segura
    // La clave secreta se genera automáticamente para el algoritmo HS256
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    /**
     * Método para extraer el nombre de usuario (subject) del token
     * @param token El token JWT
     * @return El nombre de usuario (email)
     */
    public String extractUserName(String token) {
        try {
            return extractClaims(token, Claims::getSubject);
        } catch (SignatureException e) {
            log.error("Token signature validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT signature");
        }
    }

    /**
     * Método para extraer la fecha de expiración del token
     * @param token El token JWT
     * @return La fecha de expiración del token
     */
    public Date extractExpiration(String token) {
        try {
            return extractClaims(token, Claims::getExpiration);
        } catch (SignatureException e) {
            log.error("Token signature validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT signature");
        }
    }

    /**
     * Método genérico para extraer diferentes claims del token
     * @param token El token JWT
     * @param claimsResolver La función que especifica el tipo de claim que deseas extraer
     * @param <T> El tipo del claim que esperas
     * @return El claim extraído
     */
    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);  // Extrae todos los claims
        return claimsResolver.apply(claims);  // Aplica la función para extraer el claim
    }

    /**
     * Método para obtener la clave secreta para la firma del token
     * @return La clave secreta para firmar el token
     */
    private Key getSigningKey() {
        return SECRET_KEY;  // Usamos la clave secreta generada para HS256
    }

    /**
     * Método para extraer todos los claims del token
     * @param token El token JWT
     * @return Los claims extraídos del token
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())  // Usamos la clave secreta para verificar la firma
                    .build()
                    .parseClaimsJws(token)  // Parseamos el token JWT
                    .getBody();  // Extraemos los claims
        } catch (SignatureException e) {
            log.error("Token signature validation failed: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT signature");
        }
    }

    /**
     * Método para verificar si el token ha expirado
     * @param token El token JWT
     * @return true si el token ha expirado, false si no
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());  // Compara la fecha de expiración con la fecha actual
    }

    /**
     * Método para generar un token JWT
     * @param userName El nombre de usuario (generalmente el correo electrónico)
     * @param role El rol del usuario (ejemplo: "admin" o "user")
     * @return El token JWT generado
     */
    public String generateToken(String userName, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);  // Agrega el rol como un claim
        return createToken(claims, userName);  // Crea el token con los claims y el nombre de usuario
    }

    /**
     * Método para crear un token JWT con los claims proporcionados
     * @param claims Los claims que deseas incluir en el token
     * @param subject El sujeto del token (generalmente el correo electrónico del usuario)
     * @return El token JWT creado
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)  // Asigna los claims
                .setSubject(subject)  // Asigna el sujeto (nombre de usuario)
                .setIssuedAt(new Date(System.currentTimeMillis()))  // Fecha de emisión
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))  // 10 horas de expiración
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)  // Firma con la clave secreta
                .compact();  // Genera el token
    }

    /**
     * Método para validar el token JWT
     * @param token El token JWT
     * @param userDetails Los detalles del usuario autenticado
     * @return true si el token es válido, false si no lo es
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUserName(token);  // Extrae el nombre de usuario del token
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));  // Verifica el nombre de usuario y si el token ha expirado
    }
}