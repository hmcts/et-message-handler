package uk.gov.hmcts.reform.ethos.ecm.consumer.helpers;

import uk.gov.hmcts.ecm.common.model.helper.TribunalOffice;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CloseDataModel;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationDataModel;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.CreationSingleDataModel;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.PreAcceptDataModel;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.TransferToEcmDataModel;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static uk.gov.hmcts.ecm.common.model.helper.Constants.ENGLANDWALES_CASE_TYPE_ID;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.NO;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SCOTLAND_BULK_CASE_TYPE_ID;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.SINGLE_CASE_TYPE;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.YES;

@SuppressWarnings("PMD")
public final class Helper {

    private static final String CASE_NUMBER1 = "4150002/2020";
    private static final String MULTIPLE_CASE = "4150001";
    private static final String JURISDICTION = "EMPLOYMENT";
    private static final String USERNAME = "eric.ccdcooper@gmail.com";
    private static final PreAcceptDataModel PRE_ACCEPT_DATA_MODEL = PreAcceptDataModel.builder().build();
    private static final CloseDataModel CLOSE_DATA_MODEL = CloseDataModel.builder().build();

    private Helper() {
    }

    private static final CreationDataModel CREATION_DATA_MODEL = CreationDataModel.builder()
        .lead(CASE_NUMBER1)
        .multipleRef(MULTIPLE_CASE)
        .build();

    private static final CreationSingleDataModel CREATION_SINGLE_DATA_MODEL = CreationSingleDataModel.builder()
        .positionTypeCT("PositionTypeCT")
        .officeCT("Manchester")
        .ccdGatewayBaseUrl("ccdGatewayBaseUrl")
        .build();

    private static final TransferToEcmDataModel TRANSFER_TO_ECM_DATA_MODEL = TransferToEcmDataModel.builder()
        .positionTypeCT("PositionTypeCT")
        .officeCT(TribunalOffice.LEEDS.getOfficeName())
        .ccdGatewayBaseUrl("ccdGatewayBaseUrl")
        .sourceEthosCaseReference("4150001/2020")
        .build();

    public static UpdateCaseMsg generateUpdateCaseMsg() {
        return UpdateCaseMsg.builder()
            .msgId("1")
            .jurisdiction(JURISDICTION)
            .caseTypeId(SCOTLAND_BULK_CASE_TYPE_ID)
            .multipleRef(MULTIPLE_CASE)
            .ethosCaseReference(CASE_NUMBER1)
            .totalCases("1")
            .username(USERNAME)
            .confirmation(YES)
            .dataModelParent(CREATION_DATA_MODEL)
            .build();
    }

    public static UpdateCaseMsg generatePreAcceptCaseMsg() {
        return UpdateCaseMsg.builder()
            .msgId("1")
            .jurisdiction(JURISDICTION)
            .caseTypeId(SCOTLAND_BULK_CASE_TYPE_ID)
            .multipleRef(MULTIPLE_CASE)
            .ethosCaseReference(CASE_NUMBER1)
            .totalCases("1")
            .username(USERNAME)
            .confirmation(YES)
            .dataModelParent(PRE_ACCEPT_DATA_MODEL)
            .build();
    }

    public static UpdateCaseMsg generateCloseCaseMsg() {
        return UpdateCaseMsg.builder()
            .msgId("1")
            .jurisdiction(JURISDICTION)
            .caseTypeId(SCOTLAND_BULK_CASE_TYPE_ID)
            .multipleRef(MULTIPLE_CASE)
            .ethosCaseReference(CASE_NUMBER1)
            .totalCases("1")
            .username(USERNAME)
            .confirmation(NO)
            .dataModelParent(CLOSE_DATA_MODEL)
            .build();
    }

    public static CreateUpdatesMsg generateCreateUpdatesMsg() {
        return CreateUpdatesMsg.builder()
            .msgId("1")
            .jurisdiction(JURISDICTION)
            .caseTypeId(SCOTLAND_BULK_CASE_TYPE_ID)
            .multipleRef(MULTIPLE_CASE)
            .ethosCaseRefCollection(Arrays.asList("4150001/2020", CASE_NUMBER1, "4150003/2020"))
            .totalCases("3")
            .username(USERNAME)
            .confirmation(YES)
            .dataModelParent(CREATION_DATA_MODEL)
            .build();
    }

    public static UpdateCaseMsg generateCreationSingleCaseMsg() {
        return UpdateCaseMsg.builder()
            .msgId("1")
            .jurisdiction(JURISDICTION)
            .caseTypeId(SCOTLAND_BULK_CASE_TYPE_ID)
            .multipleRef(MULTIPLE_CASE)
            .ethosCaseReference(CASE_NUMBER1)
            .totalCases("1")
            .username(USERNAME)
            .confirmation(NO)
            .dataModelParent(CREATION_SINGLE_DATA_MODEL)
            .build();
    }

    public static CompletableFuture<Void> getCompletableFuture() {
        return CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public static CreateUpdatesMsg transferToEcmMessage() {
        return CreateUpdatesMsg.builder()
            .msgId("1")
            .jurisdiction(JURISDICTION)
            .caseTypeId(ENGLANDWALES_CASE_TYPE_ID)
            .multipleRef(SINGLE_CASE_TYPE)
            .ethosCaseRefCollection(Arrays.asList("4150001/2020"))
            .totalCases("1")
            .username(USERNAME)
            .confirmation(YES)
            .dataModelParent(TRANSFER_TO_ECM_DATA_MODEL)
            .build();
    }
}
