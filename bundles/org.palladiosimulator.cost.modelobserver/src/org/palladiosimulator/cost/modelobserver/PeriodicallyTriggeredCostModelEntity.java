package org.palladiosimulator.cost.modelobserver;

import org.apache.log4j.Logger;
import org.palladiosimulator.commons.designpatterns.AbstractObservable;
import org.palladiosimulator.commons.designpatterns.IAbstractObservable;
import org.palladiosimulator.simulizar.simulationevents.PeriodicallyTriggeredSimulationEntity;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimEngineFactory;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationTimeProvider;

public class PeriodicallyTriggeredCostModelEntity extends PeriodicallyTriggeredSimulationEntity
        implements IAbstractObservable<IAbstractPeriodicContainerListener> {
    @AssistedFactory
    public static interface Factory {
        PeriodicallyTriggeredCostModelEntity create(final double firstOccurrence, final double delay);
    }

    private static final Logger LOGGER = Logger.getLogger(PeriodicallyTriggeredSimulationEntity.class);
    private final CostModel costModel;
    private final double delay;

    private final AbstractObservable<IAbstractPeriodicContainerListener> observableDelegate;
    private final ISimulationTimeProvider timeProvider;

    @AssistedInject
    public PeriodicallyTriggeredCostModelEntity(ISimEngineFactory simFactory, 
            final ISimulationTimeProvider timeProvider, final CostModel costModel,
            @Assisted final double firstOccurrence, @Assisted final double delay) {
        super(simFactory, firstOccurrence, delay);
        this.timeProvider = timeProvider;
        this.costModel = costModel;
        this.delay = delay;
        this.observableDelegate = new AbstractObservable<IAbstractPeriodicContainerListener>() {
        };
    }

    @Override
    protected void triggerInternal() {
        final Double timestamp = timeProvider.getCurrentSimulationTime();

        if (LOGGER.isInfoEnabled()) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Periodic trigger for the cost model occured at time");
            stringBuilder.append(timestamp.toString());
            LOGGER.info(stringBuilder.toString());
        }
        this.observableDelegate.getEventDispatcher().triggerPeriodicUpdate(this.costModel, timestamp, this.delay);
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
