package org.palladiosimulator.cost.modelobserver;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.palladiosimulator.simulizar.di.component.core.SimuLizarRuntimeComponent;
import org.palladiosimulator.simulizar.di.component.dependency.QUALComponent;
import org.palladiosimulator.simulizar.di.component.dependency.SimuComFrameworkComponent;
import org.palladiosimulator.simulizar.di.extension.ExtensionComponent;
import org.palladiosimulator.simulizar.modelobserver.IModelObserver;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;

import dagger.Binds;
import dagger.Component;
import dagger.Provides;

@Component(dependencies = { SimuLizarRuntimeComponent.class, SimuComFrameworkComponent.class, QUALComponent.class }, modules = CostEfficiencyExtensionComponent.Module.class)
@RuntimeExtensionScope
public interface CostEfficiencyExtensionComponent extends ExtensionComponent {
    
    IModelObserver costObserver();
    
    @dagger.Module
    public static interface Module {
        @Binds @RuntimeExtensionScope
        IModelObserver bindModelObserver(ResourceEnvironmentCostObserver impl);
        
        @Provides @RuntimeExtensionScope
        static CostModel provideCostModel() {
            return new CostModel();
        }
    }
    
    @Component.Factory
    public static interface Factory extends ExtensionComponent.Factory {
        CostEfficiencyExtensionComponent create(SimuLizarRuntimeComponent runtimeComponent, SimuComFrameworkComponent simucomComponent, QUALComponent qual);
    }
    
    public static class EclipseFactory implements IExecutableExtensionFactory {
        @Override
        public Object create() throws CoreException {
            return DaggerCostEfficiencyExtensionComponent.factory();
        }
    }
    
    

}
