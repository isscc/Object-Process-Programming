/*******************************************************************************
 * Copyright (c) 2012 Arieh 'Vainolo' Bibliowicz
 * You can use this code for educational purposes. For any other uses
 * please contact me: vainolo@gmail.com
 *******************************************************************************/
package com.vainolo.phd.opp.editor.command;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;

import com.vainolo.phd.opp.model.OPPFactory;
import com.vainolo.phd.opp.model.OPPProceduralLink;
import com.vainolo.phd.opp.model.OPPPoint;

/**
 * Move a link bendpoint. This class is declared final since it has a very
 * specific functionality.
 * 
 * @author vainolo
 */
public final class OPPLinkMoveBendpointCommand extends Command {

  /** Old location of the moved bendpoint. */
  private OPPPoint oldLocation;
  /** New location of the moved bendpoint. */
  private OPPPoint newLocation;
  /** Index of the bendpoint in the link's bendpoint list. */
  private int index;
  /** Link that contains the bendpoint. */
  private OPPProceduralLink link;

  /** Move the bendpoint to the new location. */
  @Override
  public void execute() {
    if(oldLocation == null) {
      oldLocation = link.getBendpoints().get(index);
    }
    link.getBendpoints().set(index, newLocation);
  }

  /** Restore the old location of the bendpoint. */
  @Override
  public void undo() {
    link.getBendpoints().set(index, oldLocation);
  }

  /**
   * Set the index where the bendpoint is located in the bendpoint list.
   * 
   * @param index
   *          the index where the bendpoint is located.
   */
  public void setIndex(final int index) {
    this.index = index;
  }

  /**
   * Set the link where the bendpoint is located.
   * 
   * @param link
   *          the link where the bendpoint is located.
   */
  public void setOPMLink(final OPPProceduralLink link) {
    this.link = link;
  }

  /**
   * Set the new location of the bendpoint.
   * 
   * @param newLocation
   *          the new location of the bendpoint.
   */
  public void setLocation(final Point newLocation) {
    OPPPoint point = OPPFactory.eINSTANCE.createOPPPoint();
    point.setX(newLocation.x);
    point.setY(newLocation.y);
    this.newLocation = point;
  }
}