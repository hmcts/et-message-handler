package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.ethos.ecm.consumer.config.OAuth2Configuration;
import uk.gov.hmcts.reform.ethos.ecm.consumer.idam.TokenResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.service.AccessTokenService.BEARER_AUTH_TYPE;

@ExtendWith(SpringExtension.class)
class AccessTokenServiceTest {

    @InjectMocks
    private transient AccessTokenService accessTokenService;
    @Mock
    private transient RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        OAuth2Configuration oauth2Configuration = new OAuth2Configuration("redirectUri", "id", "secret");
        accessTokenService = new AccessTokenService(oauth2Configuration, restTemplate);
    }

    @Test
    void testGetAccessTokenTest() {
        String url = "http://sidam-api:5000/o/token";
        ReflectionTestUtils.setField(accessTokenService, "idamApiOidcUrl", url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<TokenResponse> responseEntity = new ResponseEntity<>(getTokenResponse(), HttpStatus.OK);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(getTokenRequestMap(), headers);
        when(restTemplate.postForEntity(eq(url), eq(httpEntity), eq(TokenResponse.class))).thenReturn(responseEntity);
        String token = accessTokenService.getAccessToken("Username", "Password");
        assertEquals(BEARER_AUTH_TYPE + " accessToken", token);
    }

    @Test
    void testGetAccessTokenEmptyBody() {
        String url = "http://sidam-api:5000/o/token";
        ReflectionTestUtils.setField(accessTokenService, "idamApiOidcUrl", url);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<TokenResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(getTokenRequestMap(), headers);
        when(restTemplate.postForEntity(eq(url), eq(httpEntity), eq(TokenResponse.class))).thenReturn(responseEntity);
        String token = accessTokenService.getAccessToken("Username", "Password");
        assertEquals("", token);
    }

    private TokenResponse getTokenResponse() {
        return new TokenResponse("accessToken", "expiresIn", "idToken",
                                 "refreshToken", "scope", "tokenType");
    }

    private MultiValueMap<String, String> getTokenRequestMap() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", "id");
        map.add("client_secret", "secret");
        map.add("grant_type", "password");
        map.add("redirect_uri", "redirectUri");
        map.add("username", "Username");
        map.add("password", "Password");
        map.add("scope", "openid profile roles");
        map.add("refresh_token", null);
        map.add("code", null);
        return map;
    }

}
