package uk.gov.hmcts.reform.ethos.ecm.consumer.idam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenResponseTest {

    private TokenResponse getTokenResponse() {
        return new TokenResponse("accessToken", "expiresIn",
                                 "idToken", "refreshToken", "scope", "tokenType");
    }

    @Test
    void tokenResponseTest() {
        TokenResponse tokenResponse = getTokenResponse();
        assertEquals("accessToken", tokenResponse.accessToken);
        assertEquals("expiresIn", tokenResponse.expiresIn);
        assertEquals("idToken", tokenResponse.idToken);
        assertEquals("refreshToken", tokenResponse.refreshToken);
        assertEquals("scope", tokenResponse.scope);
        assertEquals("tokenType", tokenResponse.tokenType);
    }
}
