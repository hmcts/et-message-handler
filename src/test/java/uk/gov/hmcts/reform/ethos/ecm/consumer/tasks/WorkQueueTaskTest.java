package uk.gov.hmcts.reform.ethos.ecm.consumer.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ecm.common.model.servicebus.CreateUpdatesMsg;
import uk.gov.hmcts.ecm.common.model.servicebus.UpdateCaseMsg;
import uk.gov.hmcts.reform.ethos.ecm.consumer.service.UpdateManagementService;
import uk.gov.hmcts.reform.ethos.ecm.consumer.service.transfertoecm.TransferToEcmService;

import java.io.IOException;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.EMPLOYMENT;
import static uk.gov.hmcts.ecm.common.model.helper.Constants.NO;

@ExtendWith(SpringExtension.class)
@SuppressWarnings({"PMD.CloseResource", "PMD.CheckResultSet"})
class WorkQueueTaskTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private TransferToEcmService transferToEcmService;

    @Mock
    private UpdateManagementService updateManagementService;

    private WorkQueueTask workQueueTask;

    @BeforeEach
    public void setUp() {
        workQueueTask = new WorkQueueTask(dataSource, transferToEcmService, updateManagementService);
        ReflectionTestUtils.setField(workQueueTask, "batchSize", "50");
    }

    @Test
    void pollDatabaseForWorkMultipleCreateUpdateMessage() throws Exception {
        Connection mockConn = mockConnection();
        when(dataSource.getConnection()).thenReturn(mockConn);

        CallableStatement mockAddWork = mock(CallableStatement.class);
        CallableStatement mockCompleteWork = mock(CallableStatement.class);

        when(mockConn.prepareCall("{ call add_work(?, ?) }")).thenReturn(mockAddWork);
        when(mockConn.prepareCall("{ call complete_work(?) }")).thenReturn(mockCompleteWork);

        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.next()).thenAnswer(new Answer<Boolean>() {
            private boolean isFirstInvocation = true;

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                if (isFirstInvocation) {
                    isFirstInvocation = false;
                    return true; // Return true for the first invocation
                } else {
                    return false; // Return false for the second and subsequent invocations
                }
            }
        });

        CreateUpdatesMsg msg = new CreateUpdatesMsg();
        msg.setMsgId("123");
        msg.setEthosCaseRefCollection(List.of("6000001/2024"));
        msg.setConfirmation(NO);
        msg.setJurisdiction(EMPLOYMENT);
        msg.setMultipleRef("6000003");
        msg.setUsername("admin@hmcts.net");

        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("case_id")).thenReturn("6000003");
        when(resultSet.getString("json_data")).thenReturn(new ObjectMapper().writeValueAsString(msg));

        Statement statement = mock(Statement.class);
        when(mockConn.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT * FROM pick_up_work(50)")).thenReturn(resultSet);

        workQueueTask.pollDatabaseForWorkMultiple();

        verify(mockAddWork, times(1)).execute();
        verify(mockCompleteWork, times(1)).execute();
    }

    @Test
    void pollDatabaseForWorkMultipleUpdateCaseMessage() throws Exception {
        Connection mockConn = mockConnection();
        when(dataSource.getConnection()).thenReturn(mockConn);

        CallableStatement mockCompleteWork = mock(CallableStatement.class);

        when(mockConn.prepareCall("{ call complete_work(?) }")).thenReturn(mockCompleteWork);

        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.next()).thenAnswer(new Answer<Boolean>() {
            private boolean isFirstInvocation = true;

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                if (isFirstInvocation) {
                    isFirstInvocation = false;
                    return true; // Return true for the first invocation
                } else {
                    return false; // Return false for the second and subsequent invocations
                }
            }
        });

        UpdateCaseMsg msg = new UpdateCaseMsg();
        msg.setMsgId("123");
        msg.setConfirmation(NO);
        msg.setJurisdiction(EMPLOYMENT);
        msg.setMultipleRef("6000001");
        msg.setUsername("admin@hmcts.net");

        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("case_id")).thenReturn("6000001");
        when(resultSet.getString("json_data")).thenReturn(new ObjectMapper().writeValueAsString(msg));

        Statement statement = mock(Statement.class);
        when(mockConn.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT * FROM pick_up_work(50)")).thenReturn(resultSet);

        workQueueTask.pollDatabaseForWorkMultiple();

        verify(updateManagementService, times(1)).updateLogic(any());
        verify(mockCompleteWork, times(1)).execute();
    }

    @Test
    void pollDatabaseForWorkMultipleUpdateCaseMessageErrored() throws Exception {
        Connection mockConn = mockConnection();
        when(dataSource.getConnection()).thenReturn(mockConn);

        CallableStatement mockErroredWork = mock(CallableStatement.class);

        when(mockConn.prepareCall("{ call errored_work(?) }")).thenReturn(mockErroredWork);

        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.next()).thenAnswer(new Answer<Boolean>() {
            private boolean isFirstInvocation = true;

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                if (isFirstInvocation) {
                    isFirstInvocation = false;
                    return true; // Return true for the first invocation
                } else {
                    return false; // Return false for the second and subsequent invocations
                }
            }
        });

        UpdateCaseMsg msg = new UpdateCaseMsg();
        msg.setMsgId("123");
        msg.setConfirmation(NO);
        msg.setJurisdiction(EMPLOYMENT);
        msg.setMultipleRef("6000001");
        msg.setUsername("admin@hmcts.net");

        when(resultSet.getInt("id")).thenReturn(1);
        when(resultSet.getString("case_id")).thenReturn("6000002");
        when(resultSet.getString("json_data")).thenReturn(new ObjectMapper().writeValueAsString(msg));

        Statement statement = mock(Statement.class);
        when(mockConn.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT * FROM pick_up_work(50)")).thenReturn(resultSet);

        doThrow(new IOException()).when(updateManagementService).updateLogic(any());

        workQueueTask.pollDatabaseForWorkMultiple();

        verify(mockErroredWork, times(1)).execute();
    }

    private Connection mockConnection() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(mock(Statement.class));
        when(connection.prepareCall(anyString())).thenReturn(mock(CallableStatement.class));
        when(connection.createArrayOf(anyString(), any(Integer[].class))).thenReturn(mock(Array.class));
        return connection;
    }
}
