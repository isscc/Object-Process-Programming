/*******************************************************************************
 * Copyright (c) 2015 Arieh "Vainolo" Bibliowicz and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.vainolo.phd.opp.editor;

import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.DeleteRetargetAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.RedoRetargetAction;
import org.eclipse.gef.ui.actions.UndoRetargetAction;
import org.eclipse.gef.ui.actions.ZoomComboContributionItem;
import org.eclipse.gef.ui.actions.ZoomInRetargetAction;
import org.eclipse.gef.ui.actions.ZoomOutRetargetAction;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.RetargetAction;

import com.vainolo.phd.opp.editor.action.OPPInterpretAction;
import com.vainolo.phd.opp.editor.action.OPPStopInterpreterAction;

@SuppressWarnings("restriction")
public class OPPGraphicalEditorActionBarContributor extends ActionBarContributor {

  @Override
  protected void buildActions() {
    addRetargetAction(new UndoRetargetAction());
    addRetargetAction(new RedoRetargetAction());
    addRetargetAction(new DeleteRetargetAction());

    addRetargetAction(new RetargetAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY, GEFMessages.ToggleGrid_Label, IAction.AS_CHECK_BOX) {
      @Override
      public ImageDescriptor getImageDescriptor() {
        return ImageDescriptor.createFromFile(OPPEditorPlugin.class, "icons/opm_grid.gif");
      }
    });
    addRetargetAction(new RetargetAction(GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY, GEFMessages.ToggleSnapToGeometry_Label, IAction.AS_CHECK_BOX) {
      @Override
      public ImageDescriptor getImageDescriptor() {
        return ImageDescriptor.createFromFile(OPPEditorPlugin.class, "icons/opm_snap_to_grid.gif");
      }
    });

    addAction(new OPPInterpretAction());
    addAction(new OPPStopInterpreterAction());
    addRetargetAction(new ZoomInRetargetAction());
    addRetargetAction(new ZoomOutRetargetAction());
  }

  @Override
  public void contributeToToolBar(IToolBarManager toolBarManager) {
    super.contributeToToolBar(toolBarManager);
    toolBarManager.add(getAction(ActionFactory.UNDO.getId()));
    toolBarManager.add(getAction(ActionFactory.REDO.getId()));
    toolBarManager.add(getAction(ActionFactory.DELETE.getId()));
    toolBarManager.add(getAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY));
    toolBarManager.add(getAction(GEFActionConstants.TOGGLE_SNAP_TO_GEOMETRY));
    toolBarManager.add(getAction(OPPInterpretAction.INTERPRET_ID));
    toolBarManager.add(getAction(OPPStopInterpreterAction.STOP_INTERPRETER_ID));
    toolBarManager.add(getAction(GEFActionConstants.ZOOM_IN));
    toolBarManager.add(getAction(GEFActionConstants.ZOOM_OUT));

    toolBarManager.add(new ZoomComboContributionItem(getPage()));
  }

  @Override
  protected void declareGlobalActionKeys() {
    addGlobalActionKey(ActionFactory.SELECT_ALL.getId());
  }

  @Override
  public void contributeToStatusLine(IStatusLineManager statusLineManager) {
    super.contributeToStatusLine(statusLineManager);
  }
}
