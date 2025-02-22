// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.explorer.dialogs;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.wizards.NewProjectWizard.NewProjectCommand;
import com.google.appinventor.client.wizards.TemplateUploadWizard;
import com.google.appinventor.client.wizards.youngandroid.NewYoungAndroidProjectWizard;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A dialog containing options to begin 3 different tutorials or being a new
 * project from scratch. Should appear when the user currently has no projects
 * in their projects list.
 */
public class NoProjectDialogBox extends DialogBox {

  private static NoProjectDialogBoxUiBinder uiBinder =
      GWT.create(NoProjectDialogBoxUiBinder.class);
  private static NoProjectDialogBox lastDialog = null;

  interface NoProjectDialogBoxUiBinder extends UiBinder<Widget, NoProjectDialogBox> {
  }

  /**
   * Class to open a new project with the tutorial's contents when the user
   * clicks on the "Go to Tutorial" button.
   */
  private class NewTutorialProject implements NewProjectCommand {
    public void execute(Project project) {
      Ode.getInstance().openYoungAndroidProjectInDesigner(project);
    }
  }

  @UiField
  Button closeDialogBox;
  @UiField
  Button goToApp1;
  @UiField
  Button goToApp2;
  @UiField
  Button goToApp3;
  @UiField
  Button noDialogNewProject;

  /**
   * Creates a new dialog box when the user has no current projects in their
   * projects list. This will give them an option to open a tutorial project or
   * create their own project.
   */
  public NoProjectDialogBox() {
    this.setStylePrimaryName("ode-noDialogDiv");
    add(uiBinder.createAndBindUi(this));
    this.center();
    this.setAnimationEnabled(true);
    this.setAutoHideEnabled(true);
    lastDialog = this;
  }

  @UiHandler("closeDialogBox")
  void handleClose(ClickEvent e) {
    this.hide();
  }

  @UiHandler("goToApp1")
  void handleGoToPurr(ClickEvent e) {
    this.hide();
    new TemplateUploadWizard().createProjectFromExistingZip("RdfNotepad", new NewTutorialProject());
  }

  @UiHandler("goToApp2")
  void handleGoToTalk(ClickEvent e) {
    this.hide();
    new TemplateUploadWizard().createProjectFromExistingZip("SleepApnea", new NewTutorialProject());
  }

  @UiHandler("goToApp3")
  void handleGoToYR(ClickEvent e) {
    this.hide();
    new TemplateUploadWizard().createProjectFromExistingZip("LdpCoapTutorial", new NewTutorialProject());
  }

  @UiHandler("noDialogNewProject")
  void handleNewProject(ClickEvent e) {
    this.hide();
    new NewYoungAndroidProjectWizard(null).show();
  }

  public static void closeIfOpen() {
    if (lastDialog != null) {
      lastDialog.removeFromParent();;
      lastDialog = null;
    }
  }
}
