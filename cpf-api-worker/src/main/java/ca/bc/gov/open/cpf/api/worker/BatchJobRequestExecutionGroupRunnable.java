package ca.bc.gov.open.cpf.api.worker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.springframework.util.StopWatch;

import ca.bc.gov.open.cpf.client.httpclient.DigestHttpClient;
import ca.bc.gov.open.cpf.plugin.api.RecoverableException;
import ca.bc.gov.open.cpf.plugin.api.log.AppLog;
import ca.bc.gov.open.cpf.plugin.api.security.SecurityService;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.impl.BusinessApplicationRegistry;
import ca.bc.gov.open.cpf.plugin.impl.PluginAdaptor;
import ca.bc.gov.open.cpf.plugin.impl.module.Module;
import ca.bc.gov.open.cpf.plugin.impl.security.SecurityServiceFactory;

import com.revolsys.converter.string.StringConverter;
import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.data.model.Attribute;
import com.revolsys.gis.data.model.DataObjectMetaData;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.io.FileUtil;
import com.revolsys.io.NamedLinkedHashMap;
import com.revolsys.io.json.JsonMapIoFactory;
import com.revolsys.parallel.ThreadUtil;
import com.revolsys.util.Compress;

public class BatchJobRequestExecutionGroupRunnable implements Runnable {
  private final Map<String, Object> groupIdMap;

  private final DigestHttpClient httpClient;

  private final SecurityServiceFactory securityServiceFactory;

  private final String workerId;

  private final CpfWorkerScheduler executor;

  private final String groupId;

  private final String moduleName;

  private final String logLevel;

  private final String businessApplicationName;

  private final Number batchJobId;

  private final String userId;

  private final AppLog log;

  private int errorCount = 0;

  private int successCount = 0;

  private long applicationExecutionTime = 0;

  private BusinessApplication businessApplication;

  private Module module;

  public BatchJobRequestExecutionGroupRunnable(
    final CpfWorkerScheduler executor,
    final BusinessApplicationRegistry businessApplicationRegistry,
    final DigestHttpClient httpClient,
    final SecurityServiceFactory securityServiceFactory, final String workerId,
    final Map<String, Object> groupIdMap) {
    this.executor = executor;
    this.httpClient = httpClient;
    this.securityServiceFactory = securityServiceFactory;
    this.workerId = workerId;
    this.groupIdMap = groupIdMap;
    groupId = (String)groupIdMap.get("groupId");
    moduleName = (String)groupIdMap.get("moduleName");
    businessApplicationName = (String)groupIdMap.get("businessApplicationName");
    batchJobId = (Number)groupIdMap.get("batchJobId");
    userId = (String)groupIdMap.get("consumerKey");
    logLevel = (String)groupIdMap.get("logLevel");
    log = new AppLog(businessApplicationName, groupId, logLevel);
  }

  public void addError(final Map<String, Object> result,
    final String logPrefix, final String errorCode, final Throwable e) {
    log.error(logPrefix + errorCode, e);
    if (result.get("errorCode") == null) {
      result.put("errorCode", errorCode);
      if (e == null) {
        result.put("errorMessage", logPrefix);
      } else {
        final StringWriter errorOut = new StringWriter();
        e.printStackTrace(new PrintWriter(errorOut));
        result.put("errorMessage", e.getMessage());
        result.put("errorTrace", errorOut);
      }
    }
  }

  /**
   * <h2>Fields</h2>
   * 
   * batchJobExecutionGroupId long
   * requestSequenceNumber long
   * perRequestResultData boolean
   * 
   * errorCode String
   * errorMessage String
   * errorDebugMessage String
   * 
   * pluginExecutionTime long
   * results List<Map<String, Object>>
   * logRecords List<Map<String,Object>>
   * 
   * @param requestMetaData
   * @param applicationParameters
   * @param requestParameters
   * @return
   */
  protected Map<String, Object> executeRequest(
    final DataObjectMetaData requestMetaData,
    final Map<String, Object> applicationParameters,
    final Map<String, Object> requestParameters) {
    final StopWatch requestStopWatch = new StopWatch("Request");
    requestStopWatch.start();

    final Number requestSequenceNumber = (Number)requestParameters.remove("requestSequenceNumber");
    final boolean perRequestResultData = businessApplication.isPerRequestResultData();

    final Map<String, Object> requestResult = new LinkedHashMap<String, Object>();
    requestResult.put("batchJobExecutionGroupId", groupId);
    requestResult.put("requestSequenceNumber", requestSequenceNumber);
    requestResult.put("perRequestResultData", perRequestResultData);

    boolean hasError = true;
    try {
      final Map<String, Object> parameters = getParameters(businessApplication,
        requestMetaData, applicationParameters, requestParameters);
      final PluginAdaptor plugin = module.getBusinessApplicationPlugin(
        businessApplicationName, groupId, logLevel);
      if (plugin == null) {
        addError(requestResult, "Unable to create plugin "
          + businessApplicationName + " ", "ERROR_PROCESSING_REQUEST", null);
      } else {
        final AppLog appLog = plugin.getAppLog();
        File resultFile = null;
        OutputStream resultData = null;
        if (businessApplication.isPerRequestInputData()) {
          // TODO urls for per request input data
          // final String inputDataUrl = httpClient.getOAuthUrl("GET",
          // "/worker/workers/" + workerId + "/jobs/" + batchJobId
          // + "/groups/" + groupId + "/requests/" + batchJobExecutionGroupId
          // + "/inputData");
          // parameters.put("inputDataUrl", inputDataUrl);
        }
        if (businessApplication.isPerRequestResultData()) {
          resultFile = FileUtil.createTempFile(businessApplicationName, ".bin");
          resultData = new FileOutputStream(resultFile);
          parameters.put("resultData", resultData);
        }
        try {
          if (businessApplication.isSecurityServiceRequired()) {
            final SecurityService securityService = securityServiceFactory.getSecurityService(
              module, userId);
            plugin.setSecurityService(securityService);
          }
          plugin.setParameters(parameters);

          final StopWatch pluginStopWatch = new StopWatch("Plugin execute");
          pluginStopWatch.start();
          if (appLog.isDebugEnabled()) {
            appLog.debug("Request Execution Start " + groupId + " "
              + requestSequenceNumber);
          }
          try {
            plugin.execute();
          } finally {
            try {
              if (pluginStopWatch.isRunning()) {
                pluginStopWatch.stop();
              }
            } catch (final IllegalStateException e) {
            }
            final long pluginTime = pluginStopWatch.getTotalTimeMillis();
            requestResult.put("pluginExecutionTime", pluginTime);
            if (appLog.isDebugEnabled()) {
              appLog.debug("Request Execution End " + groupId + " "
                + requestSequenceNumber);
            }
          }
          final List<Map<String, Object>> results = plugin.getResults();
          requestResult.put("results", results);
          sendResultData(requestSequenceNumber, requestResult, parameters,
            resultFile, resultData);

        } catch (final IllegalArgumentException e) {
          addError(requestResult, "Error processing request ",
            "BAD_INPUT_DATA_VALUE", e);
        } catch (final RecoverableException e) {
          log.error("Error processing request " + requestSequenceNumber, e);
          addError(requestResult, "Error processing request ",
            "RECOVERABLE_EXCEPTION", null);
        } finally {
          if (resultFile != null) {
            FileUtil.closeSilent(resultData);
            resultFile.delete();
          }
        }
        try {
          if (requestStopWatch.isRunning()) {
            requestStopWatch.stop();
          }
        } catch (final IllegalStateException e) {
        }
      }
      hasError = false;
    } catch (final Throwable e) {
      log.error("Error processing request " + requestSequenceNumber, e);
      addError(requestResult, "Error processing request ",
        "ERROR_PROCESSING_REQUEST", null);
    }

    if (hasError) {
      errorCount++;
    } else {
      successCount++;
    }
    applicationExecutionTime = requestStopWatch.getTotalTimeMillis();
    return requestResult;
  }

  public String getGroupId() {
    return groupId;
  }

  @SuppressWarnings("unchecked")
  protected Map<String, Object> getParameters(
    final BusinessApplication businessApplication,
    final DataObjectMetaData requestMetaData,
    final Map<String, Object> applicationParameters,
    final Map<String, Object> requestParameters) {
    final Map<String, Object> parameters = new LinkedHashMap<String, Object>(
      applicationParameters);
    parameters.putAll(requestParameters);
    if (!businessApplication.isPerRequestInputData()) {

      for (final Entry<String, Object> entry : parameters.entrySet()) {
        final String name = entry.getKey();
        final Object value = entry.getValue();
        if (value != null) {
          final Attribute attribute = requestMetaData.getAttribute(name);
          if (attribute != null) {
            final DataType dataType = attribute.getType();
            final Class<Object> dataTypeClass = (Class<Object>)dataType.getJavaClass();
            if (!dataTypeClass.isAssignableFrom(value.getClass())) {
              entry.setValue(value);
              final StringConverter<Object> converter = StringConverterRegistry.getInstance()
                .getConverter(dataTypeClass);
              if (converter != null) {
                final Object convertedValue = converter.toObject(value);
                entry.setValue(convertedValue);
              }
            }
          }
        }
      }
    }
    return parameters;
  }

  /**
   * <h2>Fields</h2>
   * batchJobId long
   * groupId long

   * errorCode String
   * errorMessage String
   * errorDebugMessage String

   * results List<Map<String,Object>
   * logRecords List<Map<String,Object>
   * groupExecutionTime long
   * applicationExecutionTime long
   * errorCount long
   * successCount long
   */
  @Override
  @SuppressWarnings("unchecked")
  public void run() {
    try {

      log.info("Start\tGroup Execution\tgroupId=" + groupId);
      try {
        final StopWatch groupStopWatch = new StopWatch("Group");
        groupStopWatch.start();

        final Map<String, Object> groupResponse = new NamedLinkedHashMap<String, Object>(
          "ExecutionGroupResults");
        groupResponse.put("batchJobId", batchJobId);
        groupResponse.put("groupId", groupId);

        final Long moduleTime = ((Number)groupIdMap.get("moduleTime")).longValue();
        businessApplication = executor.getBusinessApplication(log, moduleName,
          moduleTime, businessApplicationName);
        if (businessApplication == null) {
          executor.addFailedGroup(groupId);
          return;
        } else {
          module = businessApplication.getModule();
          final String groupUrl = httpClient.getUrl("/worker/workers/"
            + workerId + "/jobs/" + batchJobId + "/groups/" + groupId);
          final Map<String, Object> group = httpClient.getJsonResource(groupUrl);

          final Map<String, Object> globalError = new LinkedHashMap<String, Object>();

          final DataObjectMetaData requestMetaData = businessApplication.getRequestMetaData();
          final Map<String, Object> applicationParameters = new HashMap<String, Object>(
            (Map<String, Object>)group.get("applicationParameters"));
          for (final String name : requestMetaData.getAttributeNames()) {
            final Object value = applicationParameters.get(name);
            if (value != null) {
              try {
                final DataType dataType = requestMetaData.getAttributeType(name);
                final Object convertedValue = StringConverterRegistry.toObject(
                  dataType, value);
                applicationParameters.put(name, convertedValue);
              } catch (final Throwable e) {
                log.error("Error processing group", e);
                addError(globalError, "Error processing group ",
                  "BAD_INPUT_DATA_VALUE", null);
              }
            }
          }
          if (globalError.isEmpty()) {
            final Object requestsValue = group.get("requests");
            final Map<String, Object> requestWrapper;
            if (requestsValue instanceof Map) {
              requestWrapper = (Map<String, Object>)requestsValue;
            } else {
              String requestsString = requestsValue.toString();
              if (requestsString.charAt(0) != '{') {
                requestsString = Compress.inflateBase64(requestsString);
              }
              requestWrapper = JsonMapIoFactory.toObjectMap(requestsString);
            }

            final List<Map<String, Object>> requests = (List<Map<String, Object>>)requestWrapper.get("items");

            final List<Map<String, Object>> groupResults = new ArrayList<Map<String, Object>>();

            for (final Map<String, Object> requestParameters : requests) {
              if (ThreadUtil.isInterrupted() || !module.isStarted()) {
                executor.addFailedGroup(groupId);
                return;
              }
              final Map<String, Object> requestResult = executeRequest(
                requestMetaData, applicationParameters, requestParameters);
              groupResults.add(requestResult);
            }
            final String groupResultsString = JsonMapIoFactory.toString(groupResults);
            // groupResultsString = Compress.deflateBase64(groupResultsString);
            groupResponse.put("results", groupResultsString);
          } else {
            groupResponse.putAll(globalError);
          }
        }
        if (ThreadUtil.isInterrupted() || !module.isStarted()) {
          executor.addFailedGroup(groupId);
          return;
        }
        try {
          if (groupStopWatch.isRunning()) {
            groupStopWatch.stop();
          }
        } catch (final IllegalStateException e) {
        }
        final long groupExecutionTime = groupStopWatch.getTotalTimeMillis();
        groupResponse.put("groupExecutedTime", groupExecutionTime);
        groupResponse.put("applicationExecutedTime", applicationExecutionTime);
        groupResponse.put("errorCount", errorCount);
        groupResponse.put("successCount", successCount);

        final String path = "/worker/workers/" + workerId + "/jobs/"
          + batchJobId + "/groups/" + groupId + "/results";
        @SuppressWarnings("unused")
        final Map<String, Object> submitResponse = httpClient.postJsonResource(
          httpClient.getUrl(path), groupResponse);

      } catch (final Throwable e) {
        log.error("Unable to process group " + groupId, e);
        executor.addFailedGroup(groupId);
      } finally {
        log.info("End\tGroup execution\tgroupId=" + groupId);
      }
    } finally {
      executor.removeExecutingGroupId(groupId);
    }
  }

  protected void sendResultData(final Number requestSequenceNumber,
    final Map<String, Object> requestResult,
    final Map<String, Object> parameters, final File resultFile,
    final OutputStream resultData) {
    if (resultData != null) {
      try {
        resultData.flush();
        FileUtil.closeSilent(resultData);
        final String resultDataContentType = (String)parameters.get("resultDataContentType");
        final String resultDataUrl = httpClient.getUrl("/worker/workers/"
          + workerId + "/jobs/" + batchJobId + "/groups/" + groupId
          + "/requests/" + requestSequenceNumber + "/resultData");

        final HttpResponse response = httpClient.postResource(resultDataUrl,
          resultDataContentType, resultFile);
        try {

          final StatusLine statusLine = response.getStatusLine();
          if (statusLine.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new RecoverableException(
              "Result data not accepted by server "
                + statusLine.getStatusCode() + " "
                + statusLine.getReasonPhrase());
          }
        } finally {
          FileUtil.closeSilent(response.getEntity().getContent());
        }
      } catch (final Throwable e) {
        log.error("Error sending result data", e);
        addError(requestResult, "Error processing request",
          "RECOVERABLE_EXCEPTION", null);
      }
    }
  }
}
