package software.amazon.kendra.datasource;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.kendra.KendraClient;
import software.amazon.awssdk.services.kendra.model.ConflictException;
import software.amazon.awssdk.services.kendra.model.DataSourceStatus;
import software.amazon.awssdk.services.kendra.model.DescribeDataSourceRequest;
import software.amazon.awssdk.services.kendra.model.DescribeDataSourceResponse;
import software.amazon.awssdk.services.kendra.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kendra.model.UpdateDataSourceRequest;
import software.amazon.awssdk.services.kendra.model.UpdateDataSourceResponse;
import software.amazon.awssdk.services.kendra.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

   private static final String TEST_ID = "testId";
   private static final String TEST_DATA_SOURCE_NAME = "testDataSource";
   private static final String TEST_INDEX_ID = "testIndexId";
   private static final String TEST_ROLE_ARN = "testRoleArn";
   private static final String TEST_DESCRIPTION = "testDescription";
   private static final String TEST_SCHEDULE = "testSchedule";
   private static final String TEST_DATA_SOURCE_TYPE = "testDataSourceType";

   TestDataSourceArnBuilder testDataSourceArnBuilder = new TestDataSourceArnBuilder();

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<KendraClient> proxyClient;

    @Mock
    KendraClient awsKendraClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        awsKendraClient = mock(KendraClient.class);
        proxyClient = MOCK_PROXY(proxy, awsKendraClient);
    }

   @AfterEach
   public void post_execute() {
       verify(awsKendraClient, atLeastOnce()).serviceName();
       verifyNoMoreInteractions(awsKendraClient);
   }


   @Test
   public void handleRequest_SimpleSuccess() {
       final UpdateHandler handler = new UpdateHandler(testDataSourceArnBuilder);

       final ResourceModel model = ResourceModel.builder()
           .id(TEST_ID)
           .indexId(TEST_INDEX_ID)
           .name(TEST_DATA_SOURCE_NAME)
           .schedule(TEST_SCHEDULE)
           .roleArn(TEST_ROLE_ARN)
           .description(TEST_DESCRIPTION)
           .build();

       final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
           .desiredResourceState(model)
           .build();

       when(proxyClient.client().updateDataSource(any(UpdateDataSourceRequest.class)))
           .thenReturn(UpdateDataSourceResponse.builder().build());

       when(proxyClient.client().describeDataSource(any(DescribeDataSourceRequest.class)))
           .thenReturn(DescribeDataSourceResponse.builder()
               .id(TEST_ID)
               .indexId(TEST_INDEX_ID)
               .name(TEST_DATA_SOURCE_NAME)
               .schedule(TEST_SCHEDULE)
               .description(TEST_DESCRIPTION)
               .roleArn(TEST_ROLE_ARN)
               .type(TEST_DATA_SOURCE_TYPE)
               .status(DataSourceStatus.ACTIVE)
               .build());
       final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

       ResourceModel expectedResourceModel = ResourceModel.builder()
           .id(TEST_ID)
           .indexId(TEST_INDEX_ID)
           .name(TEST_DATA_SOURCE_NAME)
           .arn(testDataSourceArnBuilder.build(request))
           .description(TEST_DESCRIPTION)
           .roleArn(TEST_ROLE_ARN)
           .schedule(TEST_SCHEDULE)
           .type(TEST_DATA_SOURCE_TYPE)
           .build();

       assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
       assertThat(response.getResourceModels()).isNull();
       assertThat(response.getMessage()).isNull();
       assertThat(response.getErrorCode()).isNull();

       verify(proxyClient.client(), times(1)).updateDataSource(any(UpdateDataSourceRequest.class));
       verify(proxyClient.client(), times(2)).describeDataSource(any(DescribeDataSourceRequest.class));
   }

   @Test
   public void handleRequest_UpdatingToActive() {
      final UpdateHandler handler = new UpdateHandler(testDataSourceArnBuilder);

       final ResourceModel model = ResourceModel.builder()
           .id(TEST_ID)
           .indexId(TEST_INDEX_ID)
           .name(TEST_DATA_SOURCE_NAME)
           .schedule(TEST_SCHEDULE)
           .roleArn(TEST_ROLE_ARN)
           .description(TEST_DESCRIPTION)
           .build();

       final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
           .desiredResourceState(model)
           .build();

       when(proxyClient.client().updateDataSource(any(UpdateDataSourceRequest.class)))
           .thenReturn(UpdateDataSourceResponse.builder().build());

       when(proxyClient.client().describeDataSource(any(DescribeDataSourceRequest.class)))
           .thenReturn(DescribeDataSourceResponse.builder()
               .id(TEST_ID)
               .indexId(TEST_INDEX_ID)
               .name(TEST_DATA_SOURCE_NAME)
               .schedule(TEST_SCHEDULE)
               .description(TEST_DESCRIPTION)
               .roleArn(TEST_ROLE_ARN)
               .type(TEST_DATA_SOURCE_TYPE)
               .status(DataSourceStatus.UPDATING)
               .build(),
               DescribeDataSourceResponse.builder()
               .id(TEST_ID)
               .indexId(TEST_INDEX_ID)
               .name(TEST_DATA_SOURCE_NAME)
               .schedule(TEST_SCHEDULE)
               .description(TEST_DESCRIPTION)
               .roleArn(TEST_ROLE_ARN)
               .type(TEST_DATA_SOURCE_TYPE)
               .status(DataSourceStatus.ACTIVE)
               .build()
               );
       final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

       ResourceModel expectedResourceModel = ResourceModel.builder()
           .id(TEST_ID)
           .indexId(TEST_INDEX_ID)
           .name(TEST_DATA_SOURCE_NAME)
           .arn(testDataSourceArnBuilder.build(request))
           .description(TEST_DESCRIPTION)
           .roleArn(TEST_ROLE_ARN)
           .schedule(TEST_SCHEDULE)
           .type(TEST_DATA_SOURCE_TYPE)
           .build();

       assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
       assertThat(response.getResourceModels()).isNull();
       assertThat(response.getMessage()).isNull();
       assertThat(response.getErrorCode()).isNull();

       verify(proxyClient.client(), times(1)).updateDataSource(any(UpdateDataSourceRequest.class));
       verify(proxyClient.client(), times(3)).describeDataSource(any(DescribeDataSourceRequest.class));
   }

   @Test
   public void handleRequest_throwsCfnInvalidRequestException() {
      final UpdateHandler handler = new UpdateHandler(testDataSourceArnBuilder);

       when(proxyClient.client().updateDataSource(any(UpdateDataSourceRequest.class)))
               .thenThrow(ValidationException.builder().build());

       final ResourceModel model = ResourceModel.builder()
           .name(TEST_DATA_SOURCE_NAME)
           .indexId(TEST_INDEX_ID)
           .type(TEST_DATA_SOURCE_TYPE)
           .dataSourceConfiguration(DataSourceConfiguration.builder().build())
           .description(TEST_DESCRIPTION)
           .roleArn(TEST_ROLE_ARN)
           .schedule(TEST_SCHEDULE)
           .build();

       final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
           .desiredResourceState(model)
           .build();

       assertThrows(CfnInvalidRequestException.class, () -> {
           handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
       });
   }

   @Test
   public void handleRequest_throwsCfnNotFoundException() {
      final UpdateHandler handler = new UpdateHandler(testDataSourceArnBuilder);

       when(proxyClient.client().updateDataSource(any(UpdateDataSourceRequest.class)))
               .thenThrow(ResourceNotFoundException.builder().build());

       final ResourceModel model = ResourceModel.builder()
           .name(TEST_DATA_SOURCE_NAME)
           .indexId(TEST_INDEX_ID)
           .type(TEST_DATA_SOURCE_TYPE)
           .dataSourceConfiguration(DataSourceConfiguration.builder().build())
           .description(TEST_DESCRIPTION)
           .roleArn(TEST_ROLE_ARN)
           .schedule(TEST_SCHEDULE)
           .build();

       final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
           .desiredResourceState(model)
           .build();

       assertThrows(CfnNotFoundException.class, () -> {
           handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
       });
   }

   @Test
   public void handleRequest_throwsCfnResourceConflictException() {
      final UpdateHandler handler = new UpdateHandler(testDataSourceArnBuilder);

       when(proxyClient.client().updateDataSource(any(UpdateDataSourceRequest.class)))
               .thenThrow(ConflictException.builder().build());

       final ResourceModel model = ResourceModel.builder()
           .name(TEST_DATA_SOURCE_NAME)
           .indexId(TEST_INDEX_ID)
           .type(TEST_DATA_SOURCE_TYPE)
           .dataSourceConfiguration(DataSourceConfiguration.builder().build())
           .description(TEST_DESCRIPTION)
           .roleArn(TEST_ROLE_ARN)
           .schedule(TEST_SCHEDULE)
           .build();

       final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
           .desiredResourceState(model)
           .build();

       assertThrows(CfnResourceConflictException.class, () -> {
           handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
       });
   }

   @Test
   public void handleRequest_throwsCfnGeneralServiceException() {
      final UpdateHandler handler = new UpdateHandler(testDataSourceArnBuilder);

       when(proxyClient.client().updateDataSource(any(UpdateDataSourceRequest.class)))
               .thenThrow(AwsServiceException.builder().build());

       final ResourceModel model = ResourceModel.builder()
           .name(TEST_DATA_SOURCE_NAME)
           .indexId(TEST_INDEX_ID)
           .type(TEST_DATA_SOURCE_TYPE)
           .dataSourceConfiguration(DataSourceConfiguration.builder().build())
           .description(TEST_DESCRIPTION)
           .roleArn(TEST_ROLE_ARN)
           .schedule(TEST_SCHEDULE)
           .build();

       final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
           .desiredResourceState(model)
           .build();

       assertThrows(CfnGeneralServiceException.class, () -> {
           handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
       });
   }
}