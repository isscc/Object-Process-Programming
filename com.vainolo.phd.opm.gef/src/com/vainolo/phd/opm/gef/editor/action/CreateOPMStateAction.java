package com.vainolo.phd.opm.gef.editor.action;

import org.eclipse.gef.Request;
import org.eclipse.gef.tools.CreationTool;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.ui.IWorkbenchPart;

import com.vainolo.phd.opm.gef.editor.OPMGraphicalEditor;
import com.vainolo.phd.opm.gef.editor.factory.OPMStateFactory;

public class CreateOPMStateAction extends WorkbenchPartAction {

  public static final String ID = "CreateState";
  public static final String REQUEST = "CreateState";

  private final Request request;

  public CreateOPMStateAction(IWorkbenchPart part) {
    super(part);
    request = new Request(REQUEST);
    setId(ID);
    setText("Create State");
  }

  public Request getRequest() {
    return request;
  }

  @Override
  public void run() {
    OPMGraphicalEditor editor = (OPMGraphicalEditor) getWorkbenchPart();
    editor.getEditDomain().setActiveTool(new CreationTool(new OPMStateFactory(editor.getIdManager())));
  }

  @Override
  protected boolean calculateEnabled() {
    return true;
  }
}
