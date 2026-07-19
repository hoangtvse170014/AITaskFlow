package com.taskflow.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.dto.response.GoogleUserInfo;
import com.taskflow.exception.OAuth2AuthenticationException;
import com.taskflow.service.GoogleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleServiceImpl implements GoogleService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public GoogleUserInfo verifyIdToken(String idToken, String clientId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://oauth2.googleapis.com/tokeninfo")
                    .queryParam("id_token", idToken)
                    .queryParam("client_id", clientId)
                    .toUriString();

            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new OAuth2AuthenticationException("Invalid Google ID token");
            }

            JsonNode body = response.getBody();
            String email = body.get("email").asText();
            boolean emailVerified = body.has("email_verified") && body.get("email_verified").asBoolean();
            String sub = body.get("sub").asText();

            if (!emailVerified) {
                throw new OAuth2AuthenticationException("Google email is not verified");
            }

            return GoogleUserInfo.builder()
                    .sub(sub)
                    .email(email)
                    .emailVerified(emailVerified)
                    .name(body.has("name") ? body.get("name").asText() : email)
                    .picture(body.has("picture") ? body.get("picture").asText() : null)
                    .givenName(body.has("given_name") ? body.get("given_name").asText() : null)
                    .familyName(body.has("family_name") ? body.get("family_name").asText() : null)
                    .locale(body.has("locale") ? body.get("locale").asText() : null)
                    .build();
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to verify Google ID token", ex);
            throw new OAuth2AuthenticationException("Failed to verify Google ID token");
        }
    }

    @Override
    public GoogleUserInfo fetchUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    GoogleUserInfo.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new OAuth2AuthenticationException("Failed to fetch Google user info");
            }

            return response.getBody();
        } catch (OAuth2AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch Google user info", ex);
            throw new OAuth2AuthenticationException("Failed to fetch Google user info");
        }
    }
}
