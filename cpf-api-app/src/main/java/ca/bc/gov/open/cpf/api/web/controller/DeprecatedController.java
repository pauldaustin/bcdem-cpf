/*
 * Copyright © 2008-2015, Province of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.bc.gov.open.cpf.api.web.controller;

import java.util.Collections;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ca.bc.gov.open.cpf.api.web.builder.BatchJobResultUiBuilder;
import ca.bc.gov.open.cpf.api.web.builder.BatchJobUiBuilder;
import ca.bc.gov.open.cpf.api.web.builder.BusinessApplicationUiBuilder;

import com.revolsys.ui.web.rest.interceptor.MediaTypeUtil;
import com.revolsys.ui.web.utils.HttpServletUtils;
import com.revolsys.util.Property;
import com.revolsys.util.UrlUtil;

@Controller
@Deprecated
public class DeprecatedController {

  private BusinessApplicationUiBuilder businessApplicationUiBuilder;

  private BatchJobUiBuilder batchJobUiBuilder;

  private BatchJobResultUiBuilder batchJobResultUiBuilder;

  @RequestMapping("/ws/apps/{businessApplicationName}/{businessApplicationVersion}/instant")
  public Object getBusinessApplicationsInstant(
    @PathVariable("businessApplicationName") final String businessApplicationName) {
    return this.businessApplicationUiBuilder.redirectPage("clientInstant");
  }

  @RequestMapping("/ws/apps/{businessApplicationName}/{businessApplicationVersion}/multiple")
  public Object getBusinessApplicationsMultiple(
    @PathVariable("businessApplicationName") final String businessApplicationName) {
    return this.businessApplicationUiBuilder.redirectPage("clientMultiple");
  }

  @RequestMapping(value = {
    "/ws/apps/{businessApplicationName}/{businessApplicationVersion}",
    "/ws/users/{consumerKey}/apps/{businessApplicationName}"
  })
  public Object getBusinessApplicationsResources(
    @PathVariable("businessApplicationName") final String businessApplicationName) {
    return this.businessApplicationUiBuilder.redirectPage("clientView");
  }

  @RequestMapping("/ws/apps/{businessApplicationName}/{businessApplicationVersion}/single")
  public Object getBusinessApplicationsSingle(
    @PathVariable("businessApplicationName") final String businessApplicationName) {
    return this.businessApplicationUiBuilder.redirectPage("clientSingle");
  }

  @RequestMapping(value = {
    "/ws/users", "/ws/users/{userId}"
  })
  public void getUsers() {
    if (MediaTypeUtil.isHtmlPage()) {
      final String url = HttpServletUtils.getFullUrl("/ws/");
      HttpServletUtils.sendRedirect(url);
    } else {
      sendRedirectWithExtension("/ws/");
    }
  }

  @RequestMapping("/ws/users/{consumerKey}/apps")
  public Object getUsersBusinessApplications() {
    return this.businessApplicationUiBuilder.redirectPage("clientList");
  }

  @RequestMapping(
    value = "/ws/users/{consumerKey}/apps/{businessApplicationName}/jobs/{jobId}")
  public Object getUsersBusinessApplicationsJobs() {
    return this.batchJobUiBuilder.redirectPage("clientAppList");
  }

  @RequestMapping("/ws/users/{consumerKey}/apps/{businessApplicationName}")
  public Object getUsersBusinessApplicationsView() {
    return this.businessApplicationUiBuilder.redirectPage("clientView");
  }

  @RequestMapping(value = "/ws/users/{consumerKey}/jobs/{jobId}")
  public Object getUsersJob() {
    return this.batchJobUiBuilder.redirectPage("clientView");
  }

  @RequestMapping(value = "/ws/users/{consumerKey}/jobs")
  public Object getUsersJobs() {
    return this.batchJobUiBuilder.redirectPage("clientList");
  }

  @RequestMapping(value = "/ws/users/{consumerKey}/jobs/{jobId}/cancel")
  public Object getUsersJobsCancel() {
    return this.batchJobUiBuilder.redirectPage("clientCancel");
  }

  @RequestMapping(value = "/ws/users/{consumerKey}/jobs/{batchJobId}/results")
  public Object getUsersJobsResults() {
    return this.batchJobResultUiBuilder.redirectPage("clientList");
  }

  @RequestMapping(
    value = "/ws/users/{consumerKey}/jobs/{batchJobId}/results/{resultId}")
  public Object getUsersJobsResultsView() {
    return this.batchJobResultUiBuilder.redirectPage("clientView");
  }

  @RequestMapping(value = {
    "/ws/users/{consumerKey}/jobs/{batchJobId}/cancel"
  }, method = RequestMethod.POST)
  public void postClientCancel(@PathVariable("batchJobId") final Long batchJobId) {
    this.batchJobUiBuilder.postClientCancel(batchJobId);
  }

  private Void sendRedirectWithExtension(final String path) {
    String url = MediaTypeUtil.getUrlWithExtension(path);
    final String callback = HttpServletUtils.getParameter("callback");
    if (Property.hasValue(callback)) {
      url = UrlUtil.getUrl(url, Collections.singletonMap("callback", callback));
    }
    HttpServletUtils.sendRedirect(url);
    return null;
  }

  public void setBatchJobResultUiBuilder(
    final BatchJobResultUiBuilder resultBuilder) {
    this.batchJobResultUiBuilder = resultBuilder;
  }

  public void setBatchJobUiBuilder(final BatchJobUiBuilder jobBuilder) {
    this.batchJobUiBuilder = jobBuilder;
  }

  public void setBusinessApplicationUiBuilder(
    final BusinessApplicationUiBuilder appBuilder) {
    this.businessApplicationUiBuilder = appBuilder;
  }
}
