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

import org.jupiter.registry.Registry;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;

import java.util.List;
import java.util.concurrent.Executor;

/**
 *  RPC服务器，该接口表示一个RPC服务器应该具备的基本功能
 */
public interface JServer extends Registry {

    /**
     * Service registry.
     */
    interface ServiceRegistry {

        /**
         * Sets up the service provider.
         */
        ServiceRegistry provider(Object serviceProvider);

        /**
         * Sets the weight of this provider at current server(0 < weight <= 100).
         */
        ServiceRegistry weight(int weight);

        /**
         * suggest that the number of connections
         */
        ServiceRegistry connCount(int connCount);

        /**
         * Sets a private {@link Executor} to this provider.
         */
        ServiceRegistry executor(Executor executor);

        /**
         * Sets a private {@link FlowController} to this provider.
         */
        ServiceRegistry flowController(FlowController<JRequest> flowController);

        /**
         * Register this provider to local scope.
         */
        ServiceWrapper register();
    }

    /**
     * Returns the global {@link FlowController} if have one.
     */
    FlowController<JRequest> getFlowController();

    /**
     * Sets a global {@link FlowController} to this server.
     */
    void setFlowController(FlowController<JRequest> flowController);

    /**
     * To obtains a service registry.
     */
    ServiceRegistry serviceRegistry();

    /**
     * Lookup the service.
     */
    ServiceWrapper lookupService(Directory directory);

    /**
     * Removes the registered service.
     */
    ServiceWrapper removeService(Directory directory);

    /**
     * Returns all the registered services.
     */
    List<ServiceWrapper> getRegisteredServices();

    /**
     * Publish a service.
     *
     * @param serviceWrapper service provider wrapper, created by {@link ServiceRegistry}
     */
    void publish(ServiceWrapper serviceWrapper);

    /**
     * Publish  services.
     *
     * @param serviceWrappers service provider wrapper, created by {@link ServiceRegistry}
     */
    void publish(ServiceWrapper... serviceWrappers);

    /**
     * Publish all services.
     */
    void publishAll();

    /**
     * Unpublish a service.
     * @param serviceWrapper service provider wrapper, created by {@link ServiceRegistry}
     */
    void unpublish(ServiceWrapper serviceWrapper);

    /**
     * unpublish all services.
     */
    void unpublishAll();
}
