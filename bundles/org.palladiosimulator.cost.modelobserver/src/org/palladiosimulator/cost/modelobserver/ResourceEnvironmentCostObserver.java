package org.palladiosimulator.cost.modelobserver;

import static org.palladiosimulator.metricspec.constants.MetricDescriptionConstants.AGGREGATED_COST_OVER_TIME;
import static org.palladiosimulator.metricspec.constants.MetricDescriptionConstants.COST_OVER_TIME;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.emf.common.notify.Notification;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.cost.modelobserver.PeriodicallyTriggeredCostModelEntity.Factory;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.mdsdprofiles.notifier.MDSDProfilesNotifier;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentPackage;
import org.palladiosimulator.probeframework.calculator.DefaultCalculatorProbeSets;
import org.palladiosimulator.probeframework.calculator.IGenericCalculatorFactory;
import org.palladiosimulator.probeframework.probes.EventProbeList;
import org.palladiosimulator.probeframework.probes.Probe;
import org.palladiosimulator.probeframework.probes.TriggeredProbe;
import org.palladiosimulator.simulizar.modelobserver.AbstractResourceEnvironmentObserver;
import org.palladiosimulator.simulizar.utils.MonitorRepositoryUtil;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager.Global;

import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.probes.TakeCurrentSimulationTimeProbe;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationControl;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationTimeProvider;

public class ResourceEnvironmentCostObserver extends AbstractResourceEnvironmentObserver {

    
    private final HashMap<String, PeriodicallyTriggeredContainerEntity> periodicallyTriggeredContainerEntities = new HashMap<String, PeriodicallyTriggeredContainerEntity>();
    private final PeriodicallyTriggeredCostModelEntity.Factory costModelEntityFactory;
    private final PeriodicallyTriggeredContainerEntity.Factory containerEntityFactory;
    private final IGenericCalculatorFactory calculatorFactory;
    private final ISimulationControl simControl;
    
    @Inject
    public ResourceEnvironmentCostObserver(@Global PCMResourceSetPartition globalPCMInstance,
            PeriodicallyTriggeredContainerEntity.Factory containerEntityFactory, 
            PeriodicallyTriggeredCostModelEntity.Factory costModelEntityFactory,
            IGenericCalculatorFactory calculatorFactory,
            ISimulationControl simControl) {
        super(globalPCMInstance);
        this.containerEntityFactory = containerEntityFactory;
        this.costModelEntityFactory = costModelEntityFactory;
        this.calculatorFactory = calculatorFactory;
        this.simControl = simControl;
 
    }

    private void removeSimulatedResource(final ResourceContainer resourceContainer) {
        final PeriodicallyTriggeredContainerEntity triggeredEntity = this.periodicallyTriggeredContainerEntities
                .get(resourceContainer.getId());
        if (triggeredEntity == null) {
        	return;        	
        }
        triggeredEntity.unschedule();
        this.periodicallyTriggeredContainerEntities.remove(resourceContainer.getId());
    }

    private void initPeriodicCostCalculator(final ResourceContainer resourceContainer) {
        if (!StereotypeAPI.isStereotypeApplied(resourceContainer, "Price")) {
            return;
        }

        this.periodicallyTriggeredContainerEntities.put(resourceContainer.getId(),
                containerEntityFactory.create(resourceContainer));
    }

    private void initPeriodicCostModelCalculator() {
        this.model.stream().filter(re -> StereotypeAPI.isStereotypeApplied(re, "CostReport")).forEach(re -> {
            final double interval = StereotypeAPI.getTaggedValue(re, "interval", "CostReport");
                globalPCMInstance.getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY).forEach(elem -> {
                    var monitorRepository = (MonitorRepository) elem;
                    for (final MeasurementSpecification measurementSpecification : MonitorRepositoryUtil
                        .getMeasurementSpecificationsForElement(monitorRepository, re)) {
                        final String metricID = measurementSpecification.getMetricDescription()
                            .getId();
                        if (metricID.equals(COST_OVER_TIME.getId())) {
                            final Probe probe = new EventProbeList(COST_OVER_TIME,
                                    new ContainerCostProbe(costModelEntityFactory.create(interval, interval)),
                                    Arrays.asList((TriggeredProbe) new TakeCurrentSimulationTimeProbe(simControl)));

                            calculatorFactory.buildCalculator(COST_OVER_TIME, measurementSpecification.getMonitor()
                                .getMeasuringPoint(),
                                    DefaultCalculatorProbeSets.createSingularProbeConfiguration(probe));
                        }
                        if (metricID.equals(AGGREGATED_COST_OVER_TIME.getId())) {
                            final Probe aggregatedProbe = new EventProbeList(AGGREGATED_COST_OVER_TIME,
                                    new AggregatedContainerCostProbe(
                                            costModelEntityFactory.create(interval, interval)),
                                    Arrays.asList((TriggeredProbe) new TakeCurrentSimulationTimeProbe(simControl)));

                            calculatorFactory.buildCalculator(AGGREGATED_COST_OVER_TIME,
                                    measurementSpecification.getMonitor()
                                        .getMeasuringPoint(),
                                    DefaultCalculatorProbeSets.createSingularProbeConfiguration(aggregatedProbe));
                        }
                    }
                });
        });
    }

    @Override
    public void initialize() {
        super.initialize();

        this.initPeriodicCostModelCalculator();

        this.model.forEach(re -> {
            re.getResourceContainer_ResourceEnvironment().forEach(this::initPeriodicCostCalculator);
        });
    }

    @Override
    protected void setTaggedValue(final Notification notification) {
        final MDSDProfilesNotifier.TaggedValueTuple taggedValueTuple = ((MDSDProfilesNotifier.TaggedValueTuple) notification
                .getNewValue());
        if (ResourceenvironmentPackage.eINSTANCE.getResourceContainer().isInstance(notification.getNotifier())
                && taggedValueTuple.getStereotypeName().equals("Price")
                && taggedValueTuple.getTaggedValueName().equals("unit")) {
            // "unit" is the last tagged value expected for a complete specification to
            // initialize a periodic cost calculator.
            initPeriodicCostCalculator((ResourceContainer) notification.getNotifier());
        }
    }

    @Override
    protected void remove(final Notification notification) {
        if (notification.getFeature() == ResourceenvironmentPackage.eINSTANCE
                .getResourceEnvironment_ResourceContainer_ResourceEnvironment()) {
            this.removeSimulatedResource((ResourceContainer) notification.getOldValue());
        } else if (notification.getFeature() == ResourceenvironmentPackage.eINSTANCE
                .getResourceEnvironment_LinkingResources__ResourceEnvironment()
                || notification.getFeature() == ResourceenvironmentPackage.eINSTANCE
                        .getLinkingResource_CommunicationLinkResourceSpecifications_LinkingResource()
                || notification.getFeature() == ResourceenvironmentPackage.eINSTANCE
                        .getLinkingResource_ConnectedResourceContainers_LinkingResource()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignoring sync (remove) of linking resources");
            }
        } else {
            this.logDebugInfo(notification);
        }
    }

}
