package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ecm.common.idam.models.UserDetails;
import uk.gov.hmcts.reform.ethos.ecm.consumer.idam.IdamApi;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class UserServiceTest {

    @InjectMocks
    private transient UserService userService;
    @Mock
    private transient AccessTokenService accessTokenService;
    private transient UserDetails userDetails;

    private static final String TOKEN = "accessToken";

    @BeforeEach
    public void setUp() {
        userDetails = getUserDetails();
        IdamApi idamApi = new IdamApi() {
            @Override
            public UserDetails retrieveUserDetails(String authorisation) {
                return getUserDetails();
            }

            @Override
            public UserDetails getUserByUserId(String authorisation, String userId) {
                return getUserDetails();
            }
        };
        userService = new UserService(idamApi, accessTokenService);
        ReflectionTestUtils.setField(userService, "caseWorkerUserName", "example@gmail.com");
        ReflectionTestUtils.setField(userService, "caseWorkerPassword", "123456");
    }

    @Test
    void getUserDetailsById() { //NOPMD - suppressed LinguisticNaming
        assertEquals(userDetails, userService.getUserDetailsById("TOKEN", "id"));
    }

    @Test
    void shouldCheckAllUserDetails() {
        assertEquals("mail@mail.com", userService.getUserDetails(TOKEN).getEmail());
        assertEquals("Mike", userService.getUserDetails(TOKEN).getFirstName());
        assertEquals("Jordan", userService.getUserDetails(TOKEN).getLastName());
        assertEquals(Collections.singletonList("role"), userService.getUserDetails(TOKEN).getRoles());
        assertEquals(userDetails.toString(), userService.getUserDetails(TOKEN).toString());
    }

    @Test
    void testGetAccessToken() {
        when(accessTokenService.getAccessToken(anyString(), anyString())).thenReturn(TOKEN);
        assertEquals(TOKEN, userService.getAccessToken());
    }

    private UserDetails getUserDetails() {
        UserDetails userDetails = new UserDetails();
        userDetails.setUid("id");
        userDetails.setEmail("mail@mail.com");
        userDetails.setFirstName("Mike");
        userDetails.setLastName("Jordan");
        userDetails.setRoles(Collections.singletonList("role"));
        return userDetails;
    }

}
