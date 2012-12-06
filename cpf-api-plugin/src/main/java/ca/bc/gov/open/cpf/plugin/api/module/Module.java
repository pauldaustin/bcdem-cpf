package ca.bc.gov.open.cpf.plugin.api.module;

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplication;
import ca.bc.gov.open.cpf.plugin.api.PluginAdaptor;

public interface Module {

  Set<String> RESERVED_MODULE_NAMES = new LinkedHashSet<String>(Arrays.asList(
    "CPF", "VIEW", "EDIT", "ADD", "DELETE", "APP", "ADMIN", "DEFAULT", "COPY",
    "CLONE", "MODULE", "GROUP"));

  void addModuleError(String error);

  void clearModuleError();

  void destroy();

  void disable();

  void enable();

  BusinessApplication getBusinessApplication(String businessApplicationName);

  List<String> getBusinessApplicationNames();

  PluginAdaptor getBusinessApplicationPlugin(String businessApplicationName,
    String logLevel);

  PluginAdaptor getBusinessApplicationPlugin(BusinessApplication application,
    String logLevel);

  List<BusinessApplication> getBusinessApplications();

  ClassLoader getClassLoader();

  URL getConfigUrl();

  List<URL> getJarUrls();

  String getModuleDescriptor();

  String getModuleError();

  String getModuleType();

  String getName();

  Map<String, Set<ResourcePermission>> getPermissionsByGroupName();

  Date getStartedDate();

  boolean hasBusinessApplication(String businessApplicationName);

  boolean isApplicationsLoaded();

  boolean isEnabled();

  boolean isRelaodable();

  boolean isRemoteable();

  boolean isStarted();

  void loadApplications();

  void restart();

  void start();

  void stop();
}
