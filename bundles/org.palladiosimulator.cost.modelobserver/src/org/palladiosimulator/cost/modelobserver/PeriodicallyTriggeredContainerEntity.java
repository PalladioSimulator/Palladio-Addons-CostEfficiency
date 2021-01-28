package org.palladiosimulator.cost.modelobserver;

import org.apache.log4j.Logger;
import org.palladiosimulator.commons.designpatterns.AbstractObservable;
import org.palladiosimulator.commons.designpatterns.IAbstractObservable;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.simulizar.simulationevents.PeriodicallyTriggeredSimulationEntity;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimEngineFactory;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationTimeProvider;

/**
 * An entity that can trigger periodic events with an attached container.
 *
 * @author Hendrik Eikerling, Sebastian Lehrig
 *
 */
public class PeriodicallyTriggeredContainerEntity extends PeriodicallyTriggeredSimulationEntity
        implements IAbstractObservable<IAbstractPeriodicContainerListener> {
    @AssistedFactory
    public static interface Factory {
        PeriodicallyTriggeredContainerEntity create(final ResourceContainer resourceContainer);
    }

    private static final Logger LOGGER = Logger.getLogger(PeriodicallyTriggeredSimulationEntity.class);

    private final ResourceContainer resourceContainer;
    private final CostModel costModel;
    private final double containerPrice;
    private final String unit;
    private final AbstractObservable<IAbstractPeriodicContainerListener> observableDelegate;

    private final ISimulationTimeProvider timeProvider;

    @AssistedInject
    public PeriodicallyTriggeredContainerEntity(ISimEngineFactory simFactory, 
            final ISimulationTimeProvider timeProvider, final CostModel costModel,
            @Assisted final ResourceContainer resourceContainer) {
        super(simFactory, 0.0, getDelay(resourceContainer));
        this.timeProvider = timeProvider;

        this.containerPrice = StereotypeAPI.getTaggedValue(resourceContainer, "amount", "Price");
        this.unit = StereotypeAPI.getTaggedValue(resourceContainer, "unit", "Price");
        this.resourceContainer = resourceContainer;
        this.costModel = costModel;
        this.observableDelegate = new AbstractObservable<IAbstractPeriodicContainerListener>() {
        };
    }

    private static double getDelay(final ResourceContainer resourceContainer) {
        if (!StereotypeAPI.isStereotypeApplied(resourceContainer, "Price")) {
            throw new RuntimeException(
                    "Periodically triggered container entities need to have a 'Price' stereotype applied!");
        }

        return StereotypeAPI.getTaggedValue(resourceContainer, "interval", "Price");
    }
    

    @Override
    protected void triggerInternal() {
        final Double timestamp = timeProvider.getCurrentSimulationTime();

        if (LOGGER.isInfoEnabled()) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.resourceContainer.getEntityName());
            stringBuilder.append(" caused operation cost of ");
            stringBuilder.append(this.containerPrice);
            stringBuilder.append(" ");
            stringBuilder.append(this.unit);
            stringBuilder.append(" at time ");
            stringBuilder.append(timestamp.toString());
            LOGGER.info(stringBuilder.toString());
        }

        this.costModel.addCostTuple(this.resourceContainer.getId(), timestamp, Double.valueOf(this.containerPrice));
    }

    @Override
    public void addObserver(final IAbstractPeriodicContainerListener observer) {
        this.observableDelegate.addObserver(observer);
    }

    @Override
    public void removeObserver(final IAbstractPeriodicContainerListener observer) {
        this.observableDelegate.removeObserver(observer);
    }
    
}
