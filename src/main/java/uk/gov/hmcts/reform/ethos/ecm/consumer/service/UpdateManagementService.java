package uk.gov.hmcts.reform.ethos.ecm.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ethos.ecm.consumer.domain.MultipleErrors;
import uk.gov.hmcts.reform.ethos.ecm.consumer.domain.repository.MultipleCounterRepository;
import uk.gov.hmcts.reform.ethos.ecm.consumer.domain.repository.MultipleErrorsRepository;
import uk.gov.hmcts.reform.ethos.ecm.consumer.model.servicebus.UpdateCaseMsg;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class UpdateManagementService {

    private final MultipleCounterRepository multipleCounterRepository;
    private final MultipleErrorsRepository multipleErrorsRepository;
    private final MultipleUpdateService multipleUpdateService;
    private final SingleUpdateService singleUpdateService;

    @Autowired
    public UpdateManagementService(MultipleCounterRepository multipleCounterRepository,
                                   MultipleErrorsRepository multipleErrorsRepository,
                                   MultipleUpdateService multipleUpdateService,
                                   SingleUpdateService singleUpdateService) {
        this.multipleCounterRepository = multipleCounterRepository;
        this.multipleErrorsRepository = multipleErrorsRepository;
        this.multipleUpdateService = multipleUpdateService;
        this.singleUpdateService = singleUpdateService;
    }

    public void updateLogic(UpdateCaseMsg updateCaseMsg) throws IOException {

        singleUpdateService.sendUpdateToSingleLogic(updateCaseMsg);

        checkIfFinish(updateCaseMsg);

    }

    public void checkIfFinish(UpdateCaseMsg updateCaseMsg) throws IOException {

        log.info("Deleting all by multiple ref");
        multipleCounterRepository.deleteAllByMultipleRef(updateCaseMsg.getMultipleRef());

        log.info("Checking next multiple count");
        int counter = multipleCounterRepository.persistentQGetNextMultipleCountVal(updateCaseMsg.getMultipleRef());
        log.info("COUNTER: " + counter);
        log.info("TOTAL CASES: " + updateCaseMsg.getTotalCases());
        if (counter == Integer.parseInt(updateCaseMsg.getTotalCases())) {

            multipleUpdateService.sendUpdateToMultipleLogic(updateCaseMsg);

            sendEmailToUser(updateCaseMsg);

            deleteMultipleRefDatabase(updateCaseMsg.getMultipleRef());
        }

    }

    public void sendEmailToUser(UpdateCaseMsg updateCaseMsg) {

        log.info("Checking errors");
        List<MultipleErrors> multipleErrorsList = multipleErrorsRepository.findByMultipleRef(updateCaseMsg.getMultipleRef());

        if (multipleErrorsList != null && !multipleErrorsList.isEmpty()) {

            multipleErrorsList.forEach(multipleRef -> log.info("Case with error: " + multipleRef));

        } else {

            log.info("Sending email to user: No errors");

        }

        //TODO SEND EMAIL TO updateCaseMsg.getUsername()
    }

    public void deleteMultipleRefDatabase(String multipleRef) {
        log.info("Clearing all multipleRef");
        multipleCounterRepository.deleteAllByMultipleRef(multipleRef);
        multipleErrorsRepository.deleteAllByMultipleRef(multipleRef);
    }

}