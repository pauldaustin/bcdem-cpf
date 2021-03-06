/*
 * Copyright © 2008-2016, Province of British Columbia
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
package ca.bc.gov.open.cpf.plugin.impl.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

public class ModuleEvent extends EventObject {
  private static final long serialVersionUID = -3976150772050177003L;

  public static final String STOP = "moduleStop";

  public static final String START_FAILED = "moduleStartFailed";

  public static final String START = "moduleStart";

  public static final String SECURITY_CHANGED = "moduleSecurityChanged";

  private final String action;

  private final long moduleTime;

  private List<String> businessApplicationNames = Collections.emptyList();

  public ModuleEvent(final Module module, final String action) {
    super(module);
    if (module.isStarted()) {
      this.moduleTime = module.getStartedTime();
    } else {
      this.moduleTime = module.getLastStartTime();
    }
    this.action = action;
  }

  public String getAction() {
    return this.action;
  }

  public List<String> getBusinessApplicationNames() {
    return this.businessApplicationNames;
  }

  public Module getModule() {
    return (Module)getSource();
  }

  public String getModuleName() {
    return getModule().getName();
  }

  public long getModuleTime() {
    return this.moduleTime;
  }

  public void setBusinessApplicationNames(final List<String> businessApplicationNames) {
    this.businessApplicationNames = new ArrayList<>(businessApplicationNames);
  }

  @Override
  public String toString() {
    return super.toString() + this.action;
  }
}
