/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.rpc;

import org.jupiter.common.util.JServiceLoader;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Maps;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.registry.RegisterMeta;
import org.jupiter.registry.RegistryService;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.rpc.model.metadata.ServiceWrapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public abstract class AbstractJServer implements JServer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractJServer.class);

    /**
     * RPC Server端保存所有服务提供者的容器，内部是一个Map来存放
     */
    private final ServiceProviderContainer providerContainer = new DefaultServiceProviderContainer();
    /**
     * SPI，存在于Server端的注册器，用于将Server端的服务注册到注册中心去，
     * publish-->register方法
     */
    private final RegistryService registryService = JServiceLoader.load(RegistryService.class);

    private volatile FlowController<JRequest> flowController;

    @Override
    public void connectToConfigServer(String connectString) {
        registryService.connectToConfigServer(connectString);
    }

    @Override
    public FlowController<JRequest> getFlowController() {
        return flowController;
    }

    @Override
    public void setFlowController(FlowController<JRequest> flowController) {
        this.flowController = flowController;
    }

    @Override
    public ServiceRegistry serviceRegistry() {
        return new DefaultServiceRegistry();
    }

    @Override
    public ServiceWrapper lookupService(Directory directory) {
        return providerContainer.lookupService(directory.directory());
    }

    @Override
    public ServiceWrapper removeService(Directory directory) {
        return providerContainer.removeService(directory.directory());
    }

    @Override
    public List<ServiceWrapper> getRegisteredServices() {
        return providerContainer.getAllServices();
    }

    @Override
    public void publish(ServiceWrapper serviceWrapper) {
        ServiceMetadata metadata = serviceWrapper.getMetadata();

        RegisterMeta meta = new RegisterMeta();
        meta.setPort(bindPort());
        meta.setGroup(metadata.getGroup());
        meta.setVersion(metadata.getVersion());
        meta.setServiceProviderName(metadata.getServiceProviderName());
        meta.setWeight(serviceWrapper.getWeight());
        meta.setConnCount(serviceWrapper.getConnCount());

        registryService.register(meta);
    }

    @Override
    public void publish(ServiceWrapper... serviceWrappers) {
        for (ServiceWrapper wrapper : serviceWrappers) {
            publish(wrapper);
        }
    }

    @Override
    public void publishAll() {
        for (ServiceWrapper wrapper : providerContainer.getAllServices()) {
            publish(wrapper);
        }
    }

    @Override
    public void unpublish(ServiceWrapper serviceWrapper) {
        ServiceMetadata metadata = serviceWrapper.getMetadata();

        RegisterMeta meta = new RegisterMeta();
        meta.setPort(bindPort());
        meta.setGroup(metadata.getGroup());
        meta.setVersion(metadata.getVersion());
        meta.setServiceProviderName(metadata.getServiceProviderName());
        meta.setWeight(serviceWrapper.getWeight());
        meta.setConnCount(serviceWrapper.getConnCount());

        registryService.unregister(meta);
    }

    @Override
    public void unpublishAll() {
        for (ServiceWrapper wrapper : providerContainer.getAllServices()) {
            unpublish(wrapper);
        }
    }

    protected abstract int bindPort();

    ServiceWrapper registerService(
            String group,
            String version,
            String providerName,
            Object serviceProvider,
            Map<String, List<Class<?>[]>> methodsParameterTypes,
            int weight,
            int connCount,
            Executor executor,
            FlowController<JRequest> flowController) {

        ServiceWrapper serviceWrapper = new ServiceWrapper(group, version, providerName, serviceProvider, methodsParameterTypes);
        serviceWrapper.setWeight(weight);
        serviceWrapper.setConnCount(connCount);
        serviceWrapper.setExecutor(executor);
        serviceWrapper.setFlowController(flowController);

        providerContainer.registerService(serviceWrapper.getMetadata().directory(), serviceWrapper);

        return serviceWrapper;
    }

    /**
     * 本地的服务注册，把服务注册到本地，也就是把服务保存到ProviderContainer中
     */
    class DefaultServiceRegistry implements ServiceRegistry {

        private Object serviceProvider;
        private int weight;
        private int connCount;
        protected Executor executor;
        protected FlowController<JRequest> flowController;

        @Override
        public ServiceRegistry provider(Object serviceProvider) {
            this.serviceProvider = serviceProvider;
            return this;
        }

        @Override
        public ServiceRegistry weight(int weight) {
            this.weight = weight;
            return this;
        }

        @Override
        public ServiceRegistry connCount(int connCount) {
            this.connCount = connCount;
            return this;
        }

        @Override
        public ServiceRegistry executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public ServiceRegistry flowController(FlowController<JRequest> flowController) {
            this.flowController = flowController;
            return this;
        }

        @Override
        public ServiceWrapper register() {
            checkNotNull(serviceProvider, "serviceProvider");

            Class<?>[] interfaces = serviceProvider.getClass().getInterfaces();
            ServiceProvider annotation = null;
            String providerName = null;
            Map<String, List<Class<?>[]>> methodsParameterTypes = Maps.newHashMap();
            if (interfaces != null) {
                for (Class<?> providerInterface : interfaces) {
                    annotation = providerInterface.getAnnotation(ServiceProvider.class);
                    if (annotation == null) {
                        continue;
                    }

                    providerName = annotation.value();
                    providerName = Strings.isNotBlank(providerName) ? providerName : providerInterface.getSimpleName();

                    // method's parameterTypes
                    for (Method method : providerInterface.getMethods()) {
                        String methodName = method.getName();
                        List<Class<?>[]> list = methodsParameterTypes.get(methodName);
                        if (list == null) {
                            list = Lists.newArrayList();
                            methodsParameterTypes.put(methodName, list);
                        }
                        list.add(method.getParameterTypes());
                    }
                    break;
                }
            }
            checkArgument(annotation != null, serviceProvider.getClass() + " is not a ServiceProvider");

            String group = annotation.group();
            String version = annotation.version();

            checkNotNull(group, "group");
            checkNotNull(version, "version");

            return registerService(
                    group,
                    version,
                    providerName,
                    serviceProvider,
                    methodsParameterTypes,
                    weight,
                    connCount,
                    executor,
                    flowController);
        }
    }

    /**
     * Local service provider container.
     */
    interface ServiceProviderContainer {

        void registerService(String uniqueKey, ServiceWrapper serviceWrapper);

        ServiceWrapper lookupService(String uniqueKey);

        ServiceWrapper removeService(String uniqueKey);

        List<ServiceWrapper> getAllServices();
    }

    class DefaultServiceProviderContainer implements ServiceProviderContainer {

        private final ConcurrentMap<String, ServiceWrapper> serviceProviders = Maps.newConcurrentHashMap();

        @Override
        public void registerService(String uniqueKey, ServiceWrapper serviceWrapper) {
            serviceProviders.put(uniqueKey, serviceWrapper);

            logger.debug("ServiceProvider [{}, {}] is registered.", uniqueKey, serviceWrapper.getServiceProvider());
        }

        @Override
        public ServiceWrapper lookupService(String uniqueKey) {
            return serviceProviders.get(uniqueKey);
        }

        @Override
        public ServiceWrapper removeService(String uniqueKey) {
            ServiceWrapper provider = serviceProviders.remove(uniqueKey);
            if (provider == null) {
                logger.warn("ServiceProvider [{}] not found.", uniqueKey);
            } else {
                logger.debug("ServiceProvider [{}, {}] is removed.", uniqueKey, provider.getServiceProvider());
            }
            return provider;
        }

        @Override
        public List<ServiceWrapper> getAllServices() {
            return Lists.newArrayList(serviceProviders.values());
        }
    }
}
