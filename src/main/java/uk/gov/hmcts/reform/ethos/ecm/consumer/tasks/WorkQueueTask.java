package uk.gov.hmcts.reform.ethos.ecm.consumer.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.datamodel.TransferToEcmDataModel;
import uk.gov.hmcts.reform.ethos.ecm.consumer.model.WorkItem;
import uk.gov.hmcts.reform.ethos.ecm.consumer.service.UpdateManagementService;
import uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm.TransferToEcmService;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkQueueTask {
    @Value("${queue.batchSize:50}")
    private String batchSize;

    @Autowired
    @Qualifier("etcos")
    private final DataSource dataSource;
    private final TransferToEcmService transferToEcmService;
    private final UpdateManagementService updateManagementService;

    @Scheduled(cron = "${cron.pickupWorkTask:0 * * * * *}")
    public void pollDatabaseForWorkMultiple() throws JsonProcessingException {
        try (Connection conn = dataSource.getConnection()) {

            List<WorkItem> workTasks = pickUpWork(conn, Integer.parseInt(batchSize));

            if (CollectionUtils.isEmpty(workTasks)) {
                return;
            }

            for (WorkItem work : workTasks) {
                processWorkItem(conn, work);
            }
        } catch (SQLException e) {
            log.error("Error picking up work", e);
        }
    }

    private void processWorkItem(Connection conn, WorkItem work) throws JsonProcessingException {
        CreateUpdatesMsg message = new ObjectMapper().readValue(work.getJsonData(), CreateUpdatesMsg.class);

        if (message.getEthosCaseRefCollection() == null) {
            // Must be an Update Message
            try {
                UpdateCaseMsg updateMsg = new ObjectMapper().readValue(work.getJsonData(), UpdateCaseMsg.class);
                log.info("RECEIVED 'Update Case' ------> ethosCaseRef {} - multipleRef {} - multipleRefLinkMarkUp {}",
                        updateMsg.getEthosCaseReference(),
                        updateMsg.getMultipleRef(),
                        updateMsg.getMultipleReferenceLinkMarkUp());

                updateManagementService.updateLogic(updateMsg);
                setComplete(conn, work.getId());
            } catch (Exception e) {
                log.error("Error processing Update Message", e);
                setErrored(conn, work.getId());
            }
            return;
        }

        log.info("RECEIVED 'Create Updates' ------>  message with ID {}", message);

        try {
            if (message.getDataModelParent() instanceof TransferToEcmDataModel) {
                transferToEcmService.transferToEcm(message);
            } else {
                sendUpdateCaseMessages(message);
            }
            setComplete(conn, work.getId());
        } catch (Exception e) {
            setErrored(conn, work.getId());
            log.error("Error processing work item", e);
        }
    }

    private void sendUpdateCaseMessages(CreateUpdatesMsg createUpdatesMsg)
            throws SQLException {
        if (createUpdatesMsg.getEthosCaseRefCollection() == null) {
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            for (String ethosCaseReference : createUpdatesMsg.getEthosCaseRefCollection()) {
                UpdateCaseMsg updateCaseMsg = UpdateCaseMsg.builder()
                        .msgId(UUID.randomUUID().toString())
                        .multipleRef(createUpdatesMsg.getMultipleRef())
                        .ethosCaseReference(ethosCaseReference)
                        .totalCases(createUpdatesMsg.getTotalCases())
                        .multipleReferenceLinkMarkUp(createUpdatesMsg.getMultipleReferenceLinkMarkUp())
                        .jurisdiction(createUpdatesMsg.getJurisdiction())
                        .caseTypeId(createUpdatesMsg.getCaseTypeId())
                        .username(createUpdatesMsg.getUsername())
                        .confirmation(createUpdatesMsg.getConfirmation())
                        .dataModelParent(createUpdatesMsg.getDataModelParent())
                        .build();

                sendMessage(conn, updateCaseMsg);
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
    }

    private void sendMessage(Connection conn, UpdateCaseMsg msg) {
        try (CallableStatement addWork = conn.prepareCall("{ call add_work(?, ?) }")) {
            addWork.setString(1, msg.getMultipleRef());

            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            jsonObject.setValue(new ObjectMapper().writeValueAsString(msg));
            addWork.setObject(2, jsonObject);

            addWork.execute();
        } catch (SQLException | JsonProcessingException ex) {
            log.error("Failed to add work", ex);
        }
    }

    private void setComplete(Connection conn, int id) {
        try (CallableStatement call = conn.prepareCall("{ call complete_work(?) }")) {
            Array sqlArray = conn.createArrayOf("int4", new Integer[] { id });
            call.setArray(1, sqlArray);
            call.execute();
        } catch (SQLException ex) {
            log.error("Failed marking work as errored", ex);
        }
    }

    private void setErrored(Connection conn, int id) {
        try (CallableStatement call = conn.prepareCall("{ call errored_work(?) }")) {
            Array sqlArray = conn.createArrayOf("int4", new Integer[] { id });
            call.setArray(1, sqlArray);
            call.execute();
        } catch (SQLException ex) {
            log.error("Failed marking work as errored", ex);
        }
    }

    public List<WorkItem> pickUpWork(Connection conn, int batchSize) throws SQLException {
        List<WorkItem> workItems = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM pick_up_work(" + batchSize + ")")) {
                while (rs.next()) {
                    workItems.add(WorkItem.builder()
                            .id(rs.getInt("id"))
                            .caseId(rs.getString("case_id"))
                            .jsonData(rs.getString("json_data"))
                            .build());
                }
            } catch (SQLException ex) {
                log.error("Failed picking up work", ex);
            }
        }
        return workItems;
    }
}
