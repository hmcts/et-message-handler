package uk.gov.hmcts.reform.ethos.ecm.consumer.helpers.transfertoecm;

public final class SingleCreationServiceHelper {

    private SingleCreationServiceHelper() {
        // Access through static methods
    }

    public static String getTransferredCaseLink(String ccdGatewayBaseUrl, String caseId, String ethosCaseReference) {
        return TransferToEcmCaseDataHelper.generateMarkUp(ccdGatewayBaseUrl, caseId, ethosCaseReference);
    }
}
