// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.settings.user;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.settings.Settings;
import com.google.appinventor.client.widgets.properties.EditableProperty;
import com.google.appinventor.shared.rpc.user.UserInfoProvider;
import com.google.appinventor.shared.settings.SettingsConstants;

/**
 * General settings.
 *
 */
public final class GeneralSettings extends Settings {
  /**
   * Creates a new instance of user-specific general settings.
   *
   * @param user  user associated with settings
   */
  public GeneralSettings(UserInfoProvider user) {
    super(SettingsConstants.USER_GENERAL_SETTINGS);

    addProperty(new EditableProperty(this, SettingsConstants.GENERAL_SETTINGS_CURRENT_PROJECT_ID,
        "0", null, EditableProperty.TYPE_INVISIBLE));
    addProperty(new EditableProperty(this, SettingsConstants.USER_TEMPLATE_URLS,"",
        null, EditableProperty.TYPE_INVISIBLE));
  }

  @Override
  protected void updateAfterDecoding() {
    Ode.getInstance().openPreviousProject();
  }
}
