package com.vainolo.phd.opm.interpreter.inzoomedprocessinstance;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.vainolo.phd.opm.interpreter.OPMAbstractProcessInstance;
import com.vainolo.phd.opm.interpreter.OPMObjectInstance;
import com.vainolo.phd.opm.interpreter.OPMObjectInstanceValueAnalyzer;
import com.vainolo.phd.opm.interpreter.OPMProcessInstance;
import com.vainolo.phd.opm.interpreter.OPMProcessInstanceFactory;
import com.vainolo.phd.opm.interpreter.OPMProcessInstanceHeap;
import com.vainolo.phd.opm.interpreter.utils.OPDExecutionAnalyzer;
import com.vainolo.phd.opm.model.OPMLink;
import com.vainolo.phd.opm.model.OPMObject;
import com.vainolo.phd.opm.model.OPMObjectProcessDiagram;
import com.vainolo.phd.opm.model.OPMProceduralLink;
import com.vainolo.phd.opm.model.OPMProcess;
import com.vainolo.phd.opm.model.OPMState;
import com.vainolo.phd.opm.utilities.OPMConstants;
import com.vainolo.phd.opm.utilities.analysis.OPDAnalyzer;
import com.vainolo.utils.SimpleLoggerFactory;

/**
 * Executable instance used for a {@link OPMObjectProcessDiagram} containing an
 * in-zoomed process.
 * 
 * @author Arieh "Vainolo" Bibliowicz
 * 
 */
public class OPMInZoomedProcessExecutableInstance extends OPMAbstractProcessInstance implements OPMProcessInstance {

  private static final Logger logger = SimpleLoggerFactory.createLogger(OPMInZoomedProcessExecutableInstance.class
      .getName());

  private final OPMObjectProcessDiagram opd;
  private DirectedAcyclicGraph<OPMProcess, DefaultEdge> opdDag;
  private OPDAnalyzer analyzer;
  private OPMInZoomedProcessExecutionState executionState = new OPMInZoomedProcessExecutionState();
  private OPMInZoomedProcessExecutionHelper executionHelper = new OPMInZoomedProcessExecutionHelper();
  private OPMInZoomedProcessInstanceHeap heap = new OPMInZoomedProcessInstanceHeap();
  private OPMObjectInstanceValueAnalyzer valueAnalyzer = new OPMObjectInstanceValueAnalyzer();
  private OPMProcess inZoomedProcess;
  private OPDExecutionAnalyzer executionAnalyzer;

  /**
   * Create a new instance.
   * 
   * @param opd
   *          the {@link OPMObjectProcessDiagram} for this instance.
   */
  public OPMInZoomedProcessExecutableInstance(OPMObjectProcessDiagram opd, OPDAnalyzer analyzer) {
    this.opd = opd;
    this.analyzer = analyzer;
    this.executionAnalyzer = new OPDExecutionAnalyzer();
  }

  @Override
  protected void preExecution() {
    super.preExecution();
    inZoomedProcess = analyzer.getInZoomedProcess(getOpd());
    opdDag = executionAnalyzer.createExecutionDAG(getInZoomedProcess());
    heap.initializeVariables(getOpd());
  }

  @Override
  protected void postExecution() {
    super.postExecution();
    heap.exportVariableValuesToArguments(getOpd());
  }

  @Override
  public String getName() {
    return getOpd().getName();
  }

  /**
   * @deprecated This function should not be stopeed for this kind of instance.
   *             It will throw an exception.
   */
  @Override
  public void setName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected OPMProcessInstanceHeap getHeap() {
    return heap;
  }

  private void createInitialSetOfExecutableInstances() {
    Set<OPMProcess> initialProcesses = executionAnalyzer.findInitialProcesses(opdDag);
    for(OPMProcess process : initialProcesses) {
      OPMProcessInstance instance = OPMProcessInstanceFactory.createExecutableInstance(process);
      executionState.addWaitingInstance(process, instance);
    }
  }

  public Set<OPMProcessInstance> findWaitingInstanceThatCanBeMadeReady() {
    Set<OPMProcessInstance> newReadyInstances = Sets.newHashSet();
    for(OPMProcessInstance waitingInstance : executionState.getWaitingInstances()) {
      loadInstanceArguments(waitingInstance);
      if(waitingInstance.isReady())
        newReadyInstances.add(waitingInstance);
    }
    return newReadyInstances;
  }

  public Set<OPMProcessInstance> findWaitingInstancesThatMustBeSkipped() {
    Set<OPMProcessInstance> instancesThatMustBeSkipped = Sets.newHashSet();
    for(OPMProcessInstance waitingInstance : executionState.getWaitingInstances()) {
      loadInstanceArguments(waitingInstance);
      if(instanceMustBeSkipped(waitingInstance)) {
        instancesThatMustBeSkipped.add(waitingInstance);
      }
    }
    return instancesThatMustBeSkipped;
  }

  private void loadInstanceArguments(OPMProcessInstance instance) {
    for(OPMLink incomingDataLink : analyzer.findIncomingDataLinks(executionState.getProcess(instance))) {
      OPMObject argument = analyzer.getObject(incomingDataLink);
      if(incomingDataLink.getCenterDecoration() == null || "".equals(incomingDataLink.getCenterDecoration())) {
        instance.setArgument(argument.getName(), heap.getVariable(argument));
      } else {
        instance.setArgument(incomingDataLink.getCenterDecoration(), heap.getVariable(argument));
      }
    }
  }

  private void skipInstancesAndCreateNewWaitingInstances(Set<OPMProcessInstance> waitingInstancesThatCanBeSkipped) {
    for(OPMProcessInstance instance : waitingInstancesThatCanBeSkipped) {
      OPMProcess process = executionState.getProcess(instance);
      executionState.removeWaitingInstance(instance);
      createFollowingWaitingInstances(process, instance, true);
    }
  }

  @Override
  protected void executing() {
    createInitialSetOfExecutableInstances();
    if(!executionState.areThereWaitingInstances()) {
      logger.info("Nothing to execute in " + getName());
      return;
    }

    while(executionState.areThereWaitingOrReadyInstances()) {
      calculateReadyInstances();
      if(!executionState.areThereReadyInstances()) {
        logger.info("Finished execution with waiting processes. Exiting.");
        return;
      }

      OPMProcessInstance instance = executionState.getReadyInstances().iterator().next();
      if(instanceIsNotReadyAnymore(instance)) {
        executionState.makeReadyInstanceWaiting(instance);
        continue;
      }
      boolean skipped = executeSingleInstance(instance);
      handlePostExecutionOfSingleInstance(instance, skipped);
    }

    logger.info("finished executing " + getName());
  }

  private boolean executeSingleInstance(OPMProcessInstance instance) {
    if(!instanceMustBeSkipped(instance)) {
      instance.execute();
      extractResultsToVariables(instance);
      return false;
    } else {
      logger.info("Skipping " + executionState.getProcess(instance).getName());
      return true;
    }
  }

  private void handlePostExecutionOfSingleInstance(OPMProcessInstance instance, boolean skipped) {
    OPMProcess process = executionState.getProcess(instance);
    executionState.removeReadyInstance(instance);
    createFollowingWaitingInstances(process, instance, skipped);
  }

  private void calculateReadyInstances() {
    if(!executionState.areThereReadyInstances()) {
      Set<OPMProcessInstance> waitingInstancesThatMustBeSkipped = findWaitingInstancesThatMustBeSkipped();
      while(waitingInstancesThatMustBeSkipped.size() > 0) {
        skipInstancesAndCreateNewWaitingInstances(waitingInstancesThatMustBeSkipped);
        waitingInstancesThatMustBeSkipped = findWaitingInstancesThatMustBeSkipped();
      }
      Set<OPMProcessInstance> waitingInstancesThatCanBeMadeReady = findWaitingInstanceThatCanBeMadeReady();
      executionState.makeWaitingInstancesReady(waitingInstancesThatCanBeMadeReady);
    }
  }

  private boolean instanceIsNotReadyAnymore(OPMProcessInstance instance) {
    loadInstanceArguments(instance);
    return !instance.isReady();
  }

  private void extractResultsToVariables(OPMProcessInstance instance) {
    for(OPMLink resultLink : analyzer.findOutgoingDataLinks(executionState.getProcess(instance))) {
      OPMObject object = analyzer.getObject(resultLink);
      OPMObjectInstance value;
      if(resultLink.getCenterDecoration() == null || "".equals(resultLink.getCenterDecoration())) {
        value = instance.getArgument(object.getName());
      } else {
        value = instance.getArgument(resultLink.getCenterDecoration());
      }
      heap.setVariable(object, value);
    }
  }

  private void createFollowingWaitingInstances(OPMProcess process, OPMProcessInstance instance, boolean skipped) {
    boolean createdInstancesFromEventLinks = false;
    if(!skipped)
      createdInstancesFromEventLinks = createFollowingWaitinginstancesFromEventLinks(process, instance);
    if(!createdInstancesFromEventLinks) {
      createFollowingWaitingInstancesFromFollowingProcesses(process, instance);
    }
  }

  private boolean createFollowingWaitinginstancesFromEventLinks(OPMProcess process, OPMProcessInstance instance) {
    Set<OPMProcess> newProcesses = findFollowingProcessesFromEventLinks(process);
    boolean created = false;
    for(OPMProcess newProcess : newProcesses) {
      logger.info("Creating new instance of " + process.getName() + " from event link.");
      OPMProcessInstance newInstance = createNewWaitingInstance(newProcess);
      created = true;
      loadInstanceArguments(newInstance);
      if(!newInstance.isReady()) {
        logger.info("Removing created instance of " + process.getName() + " because it is not ready.");
        executionState.removeWaitingInstance(newInstance);
        created = false;
      }
    }
    return created;
  }

  private Set<OPMProcess> findFollowingProcessesFromEventLinks(OPMProcess process) {
    Set<OPMProcess> ret = Sets.newHashSet();
    // TODO: there is a bug here, since a change in a variable can cause change
    // in unrelated variables, and this only checks the variables that are
    // output by the process.

    Collection<OPMProceduralLink> outgoingProceduralLinks = analyzer.findOutgoingDataLinks(process);
    for(OPMProceduralLink link : outgoingProceduralLinks) {
      OPMObject object = analyzer.getObject(link);
      if(heap.getVariable(object) != null) {
        ret.addAll(findProcessesToInvokeAfterObjectHasChanged(object, heap.getVariable(object)));
      }
    }
    return ret;
  }

  private Set<OPMProcess> findProcessesToInvokeAfterObjectHasChanged(OPMObject object, OPMObjectInstance objectInstance) {
    Preconditions.checkNotNull(objectInstance, "Object instance cannot be null.");
    Set<OPMProcess> ret = Sets.newHashSet();
    Collection<OPMProceduralLink> outgoingEventLinks = analyzer.findOutgoingEventLinks(object);
    for(OPMProceduralLink eventLink : outgoingEventLinks) {
      if(eventLinkInvokesProcess(eventLink, objectInstance)) {
        ret.add(analyzer.getProcess(eventLink));
      }
    }
    return ret;
  }

  private boolean eventLinkInvokesProcess(OPMProceduralLink link, OPMObjectInstance objectInstance) {
    if(OPMObject.class.isInstance(link.getSource())) {
      return true;
    } else if(OPMState.class.isInstance(link.getSource())) {
      OPMState state = OPMState.class.cast(link.getSource());
      if(valueAnalyzer.isObjectInstanceInState(objectInstance, state)) {
        return true;
      }
    }
    return false;
  }

  private OPMProcessInstance createNewWaitingInstance(OPMProcess process) {
    OPMProcessInstance newInstance = OPMProcessInstanceFactory.createExecutableInstance(process);
    executionState.addWaitingInstance(process, newInstance);
    return newInstance;
  }

  private void createFollowingWaitingInstancesFromFollowingProcesses(OPMProcess process, OPMProcessInstance instance) {
    Set<OPMProcess> followingProcesses = executionHelper.calculateFollowingProcesses(process, opdDag, executionState);
    for(OPMProcess followingProcess : followingProcesses) {
      OPMProcessInstance newInstance = OPMProcessInstanceFactory.createExecutableInstance(followingProcess);
      executionState.addWaitingInstance(followingProcess, newInstance);
    }

  }

  private boolean instanceMustBeSkipped(OPMProcessInstance instance) {
    for(OPMProceduralLink link : analyzer.findIncomingDataLinks(executionState.getProcess(instance))) {
      if(link.getSubKinds().contains(OPMConstants.OPM_CONDITIONAL_LINK_SUBKIND)) {
        OPMObjectInstance variable = heap.getVariable(analyzer.getObject(link));
        if(variable == null) {
          logger.info("Skipping instance of " + executionState.getProcess(instance).getName()
              + " because conditional parameter " + analyzer.getObject(link).getName() + " is empty");
          return true;
        }
        if(OPMState.class.isInstance(link.getSource())) {
          if(!valueAnalyzer.isObjectInstanceInState(variable, OPMState.class.cast(link.getSource()))) {
            logger.info("Skipping instance of " + executionState.getProcess(instance).getName()
                + " because conditional parameter " + analyzer.getObject(link).getName() + " is not in state "
                + OPMState.class.cast(link.getSource()).getName());
            return true;
          }
        }
      }
    }

    return false;
  }

  @Override
  public boolean isReady() {
    Collection<OPMObject> parameters = analyzer.findIncomingParameters(getOpd());
    for(OPMObject object : parameters) {
      if(getArgument(object.getName()) == null) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get the main in-zoomed {@link OPMProcess} of the
   * {@link OPMObjectProcessDiagram}
   * 
   * @return the main {@link OPMProcess} of this {@link OPMObjectProcessDiagram}
   */
  private OPMProcess getInZoomedProcess() {
    return inZoomedProcess;
  }

  /**
   * Get the {@link OPMObjectProcessDiagram} that this class interprets
   * 
   * @return the {@link OPMObjectProcessDiagram} interpreted by this class
   */
  private OPMObjectProcessDiagram getOpd() {
    return opd;
  }
}