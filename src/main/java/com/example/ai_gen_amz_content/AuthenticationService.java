package com.example.ai_gen_amz_content;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Date;

@Slf4j
@Service
public class AuthenticationService {
    @Value("${app.security.jwt-secret:1bacddc4e4e0ae4ed91481908f82722d401ead234f699b37fed6bfbfbf83a9e0}")
    private String secretKey;

    private String generateToken() {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject("LIGON_EMPLOYEE")
                .issuer("ligon_team")
                .issueTime(new Date())
                .expirationTime(Date.from(ZonedDateTime.now()
                        .plusMonths(1)          // thêm 1 tháng
                        .toInstant()))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(secretKey));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean introspectToken(String token) {
        try {
            JWSVerifier verifier = new MACVerifier(secretKey.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);
            var verified = signedJWT.verify(verifier);

            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();

            return verified && expiration.after(new Date());

        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    @PostConstruct
    private void getKey() {
        log.info("Ligon Key: {}", generateToken());
    }
}
