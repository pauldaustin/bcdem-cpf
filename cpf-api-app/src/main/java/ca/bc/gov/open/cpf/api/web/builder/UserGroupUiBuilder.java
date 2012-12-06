package ca.bc.gov.open.cpf.api.web.builder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import ca.bc.gov.open.cpf.api.domain.CpfDataAccessObject;
import ca.bc.gov.open.cpf.api.domain.UserAccount;
import ca.bc.gov.open.cpf.api.domain.UserGroup;
import ca.bc.gov.open.cpf.api.domain.UserGroupAccountXref;
import ca.bc.gov.open.cpf.api.domain.UserGroupPermission;
import ca.bc.gov.open.cpf.plugin.api.module.Module;

import com.revolsys.gis.data.model.DataObject;
import com.revolsys.gis.data.model.DataObjectUtil;
import com.revolsys.io.xml.XmlWriter;
import com.revolsys.ui.html.fields.Field;
import com.revolsys.ui.html.form.Form;
import com.revolsys.ui.html.view.Element;
import com.revolsys.ui.html.view.TabElementContainer;
import com.revolsys.ui.web.utils.HttpServletUtils;

@Controller
public class UserGroupUiBuilder extends CpfUiBuilder {

  public UserGroupUiBuilder() {
    super("userGroup", UserGroup.USER_GROUP, UserGroup.USER_GROUP_NAME,
      "User Group", "User Groups");
    setIdParameterName("userGroupName");
  }

  public void userGroupName(final XmlWriter out, final Object object) {
    final DataObject dataObject = (DataObject)object;
    final long userGroupId = DataObjectUtil.getLong(dataObject,
      UserGroup.USER_GROUP_ID);

    CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
    DataObject userGroup = dataAccessObject.getUserGroup(userGroupId);

    String name = userGroup.getValue(UserGroup.USER_GROUP_NAME);
    out.text(name);
  }

  private static final List<String> GLOBAL_GROUP_NAMES = Arrays.asList("ADMIN",
    "USER_TYPE", "GLOBAL", "WORKER");

  public void adminUserGroupLink(final XmlWriter out, final Object object) {
    final DataObject userGroup = (DataObject)object;

    Map<String, String> parameterNames = new HashMap<String, String>();
    parameterNames.put("userGroupName", "userGroupName");

    Map<String, Object> linkObject = new HashMap<String, Object>();
    Object userGroupName = userGroup.getValue(UserGroup.USER_GROUP_NAME);
    linkObject.put(UserGroup.USER_GROUP_NAME, userGroupName);
    linkObject.put("userGroupName", userGroupName);

    String moduleName = userGroup.getValue(UserGroup.MODULE_NAME);
    String pageName;
    if (GLOBAL_GROUP_NAMES.contains(moduleName)) {
      pageName = "groupView";
    } else if (moduleName.startsWith("ADMIN_MODULE_")) {
      pageName = "moduleAdminView";
      linkObject.put("moduleName", moduleName.substring(13));
      parameterNames.put("moduleName", "moduleName");
    } else {
      pageName = "moduleView";
      linkObject.put("moduleName", moduleName);
      parameterNames.put("moduleName", "moduleName");
    }

    serializeLink(out, linkObject, UserGroup.USER_GROUP_NAME, pageName,
      parameterNames);
  }

  public Element createUserGroupView(final HttpServletRequest request,
    final HttpServletResponse response, final String prefix,
    final String membersPrefix, final String moduleName,
    final String userGroupName, final List<String> moduleNames)
    throws NoSuchRequestHandlingMethodException {
    if (moduleName != null) {
      hasModule(request, moduleName);
    }

    final DataObject userGroup = getUserGroup(userGroupName);
    if (userGroup != null
      && (moduleName == null || moduleNames.contains(userGroup.getValue(UserGroup.MODULE_NAME)))) {
      final TabElementContainer tabs = new TabElementContainer();
      addObjectViewPage(tabs, userGroup, prefix);

      final Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("serverSide", Boolean.TRUE);

      if (!userGroup.getValue(UserGroup.MODULE_NAME).equals("USER_TYPE")) {
        UserAccountUiBuilder userAccountUiBuilder = getBuilder(UserAccount.USER_ACCOUNT);
        userAccountUiBuilder.addMembersDataTable(tabs, membersPrefix);
      }

      addTabDataTable(tabs, UserGroupPermission.USER_GROUP_PERMISSION, prefix
        + "List", parameters);

      return tabs;
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  public List<String> getUserGroupModuleNames(final String moduleName) {
    return Arrays.asList(moduleName, "USER_TYPE", "GLOBAL");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/adminUserGroups"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  public Object pageModuleAdminList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    hasModule(request, moduleName);

    final Map<String, Object> parameters = new HashMap<String, Object>();

    final Map<String, Object> filter = new HashMap<String, Object>();
    filter.put(UserGroup.MODULE_NAME, "ADMIN_MODULE_" + moduleName);

    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response,
      "moduleAdminList", Module.class, "view", parameters);

  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/adminUserGroups/{userGroupName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_FOR_MODULE)
  public Element pageModuleAdminView(final HttpServletRequest request,
    final HttpServletResponse response,
    final @PathVariable String userGroupName,
    @PathVariable final String moduleName) throws IOException, ServletException {
    return createUserGroupView(request, response, "moduleAdmin",
      "moduleAdminGroup", moduleName, userGroupName,
      Arrays.asList("ADMIN_MODULE_" + moduleName));

  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @PreAuthorize(ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Element pageModuleUserGroupAdd(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    hasModule(request, moduleName);

    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put(UserGroup.ACTIVE_IND, 1);
    parameters.put(UserGroup.MODULE_NAME, moduleName);
    parameters.put(UserGroup.USER_GROUP_NAME, moduleName + "_");
    return createObjectAddPage(parameters, "module", "preInsert");
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}/delete"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void pageModuleUserGroupDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName,
    @PathVariable final String userGroupName) throws IOException,
    ServletException {
    hasModule(request, moduleName);

    final DataObject userGroup = getUserGroup(userGroupName);
    if (userGroup != null
      && userGroup.getValue(UserGroup.MODULE_NAME).equals(moduleName)) {
      final CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
      dataAccessObject.deleteUserGroup(userGroup);
      redirectPage("moduleList");
    }
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @PreAuthorize(ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  public Element pageModuleUserGroupEdit(final HttpServletRequest request,
    final HttpServletResponse response,
    final @PathVariable String userGroupName,
    @PathVariable final String moduleName) throws IOException, ServletException {
    hasModule(request, moduleName);

    final Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("moduleName", moduleName);

    final DataObject userGroup = getUserGroup(userGroupName);
    if (userGroup != null
      && userGroup.getValue(UserGroup.MODULE_NAME).equals(moduleName)) {
      return createObjectEditPage(userGroup, "module");
    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  public Object pageModuleUserGroupList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String moduleName)
    throws IOException, ServletException {
    hasModule(request, moduleName);

    final Map<String, Object> parameters = new HashMap<String, Object>();

    final Map<String, Object> filter = new HashMap<String, Object>();
    final List<String> moduleNames = getUserGroupModuleNames(moduleName);
    filter.put("MODULE_NAME", moduleNames);

    parameters.put("filter", filter);

    return createDataTableHandlerOrRedirect(request, response, "moduleList",
      Module.class, "view", parameters);
  }

  @RequestMapping(value = {
    "/admin/modules/{moduleName}/userGroups/{userGroupName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_MODULE_ADMIN_OR_SECURITY_ADMINS)
  public Element pageModuleUserGroupView(final HttpServletRequest request,
    final HttpServletResponse response,
    final @PathVariable String userGroupName,
    @PathVariable final String moduleName) throws IOException, ServletException {
    return createUserGroupView(request, response, "module", "moduleGroup",
      moduleName, userGroupName, getUserGroupModuleNames(moduleName));
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}/userGroups"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  public Object pageUserAccountList(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String consumerKey)
    throws IOException, NoSuchRequestHandlingMethodException {
    final DataObject userAccount = getUserAccount(consumerKey);
    if (userAccount != null) {

      final Map<String, Object> parameters = new HashMap<String, Object>();

      parameters.put(
        "fromClause",
        "CPF.CPF_USER_GROUPS T"
          + " JOIN CPF.CPF_USER_GROUP_ACCOUNT_XREF X ON T.USER_GROUP_ID = X.USER_GROUP_ID");

      final Map<String, Object> filter = new HashMap<String, Object>();
      filter.put(UserGroupAccountXref.USER_ACCOUNT_ID, userAccount.getIdValue());

      parameters.put("filter", filter);

      return createDataTableHandlerOrRedirect(request, response,
        "userAccountList", UserAccount.USER_ACCOUNT, "view", parameters);

    }
    throw new NoSuchRequestHandlingMethodException(request);
  }

  @RequestMapping(value = {
    "/admin/userGroups/add"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Element pageUserGroupAdd(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException, ServletException {
    final Map<String, Object> defaultValues = new HashMap<String, Object>();
    defaultValues.put(UserGroup.ACTIVE_IND, 1);
    defaultValues.put(UserGroup.MODULE_NAME, "GLOBAL");
    defaultValues.put(UserGroup.USER_GROUP_NAME, "GLOBAL_");
    return super.createObjectAddPage(defaultValues, "group", "preInsert");
  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/delete"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void pageUserGroupDelete(final HttpServletRequest request,
    final HttpServletResponse response, @PathVariable final String userGroupName)
    throws IOException, ServletException {

    final DataObject userGroup = getUserGroup(userGroupName);
    if (userGroup != null
      && userGroup.getValue(UserGroup.MODULE_NAME).equals("GLOBAL")) {
      final CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
      dataAccessObject.deleteUserGroup(userGroup);
      redirectPage("groupList");
    }
  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}/edit"
  }, method = {
    RequestMethod.GET, RequestMethod.POST
  })
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Element pageUserGroupEdit(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable String userGroupName)
    throws IOException, ServletException {
    final DataObject userGroup = getUserGroup(userGroupName);
    return super.createObjectEditPage(userGroup, "group");
  }

  @RequestMapping(value = {
    "/admin/userGroups"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  public Object pageUserGroupList(final HttpServletRequest request,
    final HttpServletResponse response) throws IOException {
    return createDataTableHandler(request, "groupList");
  }

  @RequestMapping(value = {
    "/admin/userAccounts/{consumerKey}/userGroups/{userGroupName}/delete"
  }, method = RequestMethod.POST)
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void pageUserGroupMemberDelete(final HttpServletRequest request,
    final HttpServletResponse response,
    @PathVariable final String userGroupName,
    @PathVariable final String consumerKey, @RequestParam final Boolean confirm)
    throws ServletException {
    final DataObject userGroup = getUserGroup(userGroupName);
    if (userGroup != null) {

      final DataObject userAccount = getUserAccount(consumerKey);
      if (userAccount != null) {
        if (confirm == Boolean.TRUE) {
          final CpfDataAccessObject dataAccessObject = getCpfDataAccessObject();
          dataAccessObject.deleteUserGroupAccountXref(userGroup, userAccount);
        }
        redirectToTab( UserAccount.USER_ACCOUNT, "view",
          "userAccountList");
        return;
      }
    }

  }

  @RequestMapping(value = {
    "/admin/userGroups/{userGroupName}"
  }, method = RequestMethod.GET)
  @ResponseBody
  @PreAuthorize(ADMIN_OR_ADMIN_SECURITY)
  public Element pageUserGroupView(final HttpServletRequest request,
    final HttpServletResponse response, final @PathVariable String userGroupName)
    throws IOException, ServletException {
    return createUserGroupView(request, response, "group", "group", null,
      userGroupName, null);
  }

  @Override
  public boolean preInsert(final Form form, final DataObject userGroup) {
    final Field nameField = form.getField(UserGroup.USER_GROUP_NAME);
    String groupName = nameField.getValue();
    groupName = groupName.toUpperCase();
    nameField.setValue(groupName);
    userGroup.setValue(UserGroup.USER_GROUP_NAME, groupName);

    final DataObject group = getUserGroup(groupName);
    if (group == null) {
      final String moduleName = userGroup.getValue(UserGroup.MODULE_NAME);
      if (!groupName.startsWith(moduleName + "_")) {
        nameField.addValidationError("Group name must start with " + moduleName
          + "_");
        return false;
      } else {
        final int groupNameLength = groupName.length();
        final int minLength = moduleName.length() + 2;
        if (groupNameLength < minLength) {

          nameField.addValidationError("Group name must have a suffix");
          return false;
        }
      }
      return true;
    } else {
      nameField.addValidationError("Group name is already used");
      return false;
    }
  }

  @Override
  public boolean preUpdate(final Form form, final DataObject userGroup) {
    Long userGroupId = DataObjectUtil.getLong(userGroup,
      UserGroup.USER_GROUP_ID);
    final Field nameField = form.getField(UserGroup.USER_GROUP_NAME);
    String groupName = nameField.getValue();
    groupName = groupName.toUpperCase();
    nameField.setValue(groupName);
    userGroup.setValue(UserGroup.USER_GROUP_NAME, groupName);

    final DataObject group = getUserGroup(groupName);
    if (group == null
      || DataObjectUtil.getLong(group, UserGroup.USER_GROUP_ID) == userGroupId) {
      final String moduleName = userGroup.getValue(UserGroup.MODULE_NAME);
      if (!groupName.startsWith(moduleName + "_")) {
        nameField.addValidationError("Group name must start with " + moduleName
          + "_");
        return false;
      } else {
        final int groupNameLength = groupName.length();
        final int minLength = moduleName.length() + 2;
        if (groupNameLength < minLength) {

          nameField.addValidationError("Group name must have a suffix");
          return false;
        }
      }
      HttpServletUtils.setPathVariable("userGroupName", groupName);
      return true;
    } else {
      nameField.addValidationError("Group name is already used");
      return false;
    }
  }
}
