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

package org.jupiter.example.flight.exec;

import org.jupiter.registry.ConfigServer;

/**
 * jupiter
 * org.jupiter.example.flight.exec
 *
 * @author jiachun.fjc
 */
public class FlightExecConfigServer {

    public static void main(String[] args) {
        ConfigServer configServer = new ConfigServer(20001, 1); // 注册中心
        try {
            configServer.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
