package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.ethos.ecm.consumer.config.EmailClient;
import uk.gov.hmcts.reform.ethos.ecm.consumer.domain.MultipleErrors;
import uk.gov.service.notify.NotificationClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Constants.CONFIRMATION_ERROR_EMAIL;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Constants.CONFIRMATION_OK_EMAIL;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.Constants.UNPROCESSABLE_STATE;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.service.EmailService.MULTIPLE_ERRORS;
import static uk.gov.hmcts.reform.ethos.ecm.consumer.service.EmailService.MULTIPLE_REFERENCE;

@ExtendWith(SpringExtension.class)
class EmailServiceTest {

    @InjectMocks
    private transient EmailService emailService;

    @Mock
    EmailClient emailClient;

    private transient String emailAddress;

    private transient String multipleRef;

    @BeforeEach
    public void setUp() {
        emailAddress = "example@hmcts.net";
        multipleRef = "4150001";
    }

    @Test
    void sendConfirmationEmail() throws NotificationClientException {
        emailService.sendConfirmationEmail(emailAddress, multipleRef);
        ConcurrentHashMap<String, String> personalisation = getPersonalisation(new ArrayList<>(), multipleRef);
        verify(emailClient).sendEmail(eq(CONFIRMATION_OK_EMAIL), eq(emailAddress),
                                      eq(personalisation), isA(String.class));
        verifyNoMoreInteractions(emailClient);
    }

    @Test
    void sendConfirmationEmailException() throws NotificationClientException {
        ConcurrentHashMap<String, String> personalisation = getPersonalisation(new ArrayList<>(), multipleRef);
        when(emailClient.sendEmail(eq(CONFIRMATION_OK_EMAIL), eq(emailAddress),
                                   eq(personalisation), isA(String.class)))
            .thenThrow(new NotificationClientException("Exception"));
        emailService.sendConfirmationEmail(emailAddress, multipleRef);
        verify(emailClient).sendEmail(eq(CONFIRMATION_OK_EMAIL), eq(emailAddress),
                                      eq(personalisation), isA(String.class));
        verifyNoMoreInteractions(emailClient);
    }

    @Test
    void sendConfirmationErrorEmail() throws NotificationClientException {
        List<MultipleErrors> multipleErrorsList = generateMultipleErrorList();
        assertEquals("4150002/2020", multipleErrorsList.get(0).getEthoscaseref());
        assertEquals(UNPROCESSABLE_STATE, multipleErrorsList.get(0).getDescription());
        emailService.sendConfirmationErrorEmail(emailAddress, multipleErrorsList, multipleRef);
        verify(emailClient).sendEmail(eq(CONFIRMATION_ERROR_EMAIL), eq(emailAddress),
                                      eq(getPersonalisation(multipleErrorsList, multipleRef)), isA(String.class));
        verifyNoMoreInteractions(emailClient);
    }

    private List<MultipleErrors> generateMultipleErrorList() {
        MultipleErrors multipleErrors = new MultipleErrors();
        multipleErrors.setEthoscaseref("4150002/2020");
        multipleErrors.setMultipleref(multipleRef);
        multipleErrors.setDescription(UNPROCESSABLE_STATE);
        return new ArrayList<>(Collections.singletonList(multipleErrors));
    }

    private ConcurrentHashMap<String, String> getPersonalisation(
        List<MultipleErrors> multipleErrorsList,
        String multipleRef) {
        ConcurrentHashMap<String, String> personalisation = new ConcurrentHashMap<>();
        String errors = multipleErrorsList.stream()
            .map(MultipleErrors::toString)
            .collect(Collectors.joining(", "));
        personalisation.put(MULTIPLE_ERRORS, errors);
        personalisation.put(MULTIPLE_REFERENCE, multipleRef);
        return personalisation;
    }

}
