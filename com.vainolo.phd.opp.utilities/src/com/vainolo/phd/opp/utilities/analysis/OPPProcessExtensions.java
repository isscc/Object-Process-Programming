/*******************************************************************************
 * Copyright (c) 2015 Arieh "Vainolo" Bibliowicz and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.vainolo.phd.opp.utilities.analysis;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.vainolo.phd.opp.model.OPPLink;
import com.vainolo.phd.opp.model.OPPProceduralLink;
import com.vainolo.phd.opp.model.OPPProceduralLinkKind;
import com.vainolo.phd.opp.model.OPPProcess;

public class OPPProcessExtensions {

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static Collection<OPPProceduralLink> findIncomingDataLinks(OPPProcess process) {
    return (Collection) Collections2.filter(process.getIncomingLinks(), new IsIncomingDataLink());
  }

  public static Collection<OPPProceduralLink> findIncomingProceduralLinks(OPPProcess process) {
    Set<OPPProceduralLink> dataLinks = Sets.newHashSet(findIncomingDataLinks(process));
    Set<OPPProceduralLink> driverLinks = Sets.newHashSet(findIncomingAgentLinks(process));
    return Sets.union(dataLinks, driverLinks);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static Collection<OPPProceduralLink> findIncomingAgentLinks(OPPProcess process) {
    return (Collection) Collections2.filter(process.getIncomingLinks(), IsAgentLink.INSTANCE);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static Collection<OPPProceduralLink> findOutgoingDataLinks(OPPProcess process) {
    return ((Collection) Collections2.filter(process.getOutgoingLinks(), IsOutgoingDataLink.INSTANCE));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static Collection<OPPProceduralLink> findOutgoingAgentLinks(OPPProcess process) {
    return (Collection) Collections2.filter(process.getOutgoingLinks(), IsAgentLink.INSTANCE);
  }

  public enum IsAgentLink implements Predicate<OPPLink> {
    INSTANCE;

    @Override
    public boolean apply(OPPLink input) {
      if (OPPProceduralLink.class.isInstance(input)) {
        OPPProceduralLink link = OPPProceduralLink.class.cast(input);
        return OPPProceduralLinkKind.AGENT.equals(link.getKind());
      } else {
        return false;
      }
    }
  }

  public static class IsIncomingDataLink implements Predicate<OPPLink> {
    @Override
    public boolean apply(final OPPLink link) {
      if (!OPPProceduralLink.class.isInstance(link))
        return false;
      else {
        OPPProceduralLink localLink = (OPPProceduralLink) link;
        switch (localLink.getKind()) {
        case CONS_RES:
        case INSTRUMENT:
          return true;
        default:
          return false;
        }
      }
    }
  }

  public enum IsOutgoingDataLink implements Predicate<OPPLink> {
    INSTANCE;

    @Override
    public boolean apply(final OPPLink link) {
      if (!OPPProceduralLink.class.isInstance(link))
        return false;
      else {
        OPPProceduralLink localLink = OPPProceduralLink.class.cast(link);
        switch (localLink.getKind()) {
        case CONS_RES:
          return true;
        default:
          return false;
        }
      }
    }
  }

}
