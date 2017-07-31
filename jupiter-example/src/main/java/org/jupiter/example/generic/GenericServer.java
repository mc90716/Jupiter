/*
 * Copyright (c) 2016 The Jupiter Project
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

package org.jupiter.example.generic;

import org.jupiter.example.GenericServiceTestImpl;
import org.jupiter.monitor.MonitorServer;
import org.jupiter.rpc.JRequest;
import org.jupiter.rpc.flow.control.ControlResult;
import org.jupiter.rpc.flow.control.FlowController;
import org.jupiter.rpc.model.metadata.ServiceWrapper;
import org.jupiter.transport.netty.JNettyTcpAcceptor;
import org.jupiter.transport.netty.NettyAcceptor;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoop;
import io.netty.util.Recycler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * jupiter org.jupiter.example.generic
 *
 * @author jiachun.fjc
 */
public class GenericServer {

	public static void main(String[] args) throws Exception {
		/**
		 * RPC的Server端，首先连接到ConfigServer，然后将服务发布到ConfigServer中去
		 */
		NettyAcceptor server = new JNettyTcpAcceptor(18090);
		MonitorServer monitor = new MonitorServer();
		
		ProcessBuilder pb = new ProcessBuilder("");
		pb.start();
		
		try {
			monitor.start();
			ServiceWrapper provider = server.serviceRegistry().provider(new GenericServiceTestImpl())
					.flowController(new FlowController<JRequest>() { // Provider级别限流器,
																		// 可以不设置

						private AtomicLong count = new AtomicLong();

						@Override
						public ControlResult flowControl(JRequest request) {
							if (count.getAndIncrement() > 9999) {
								return new ControlResult(false, "fuck out!!!");
							}
							return ControlResult.ALLOWED;
						}
					}).register();// 调用ServiceRegistry把ServiceWrapper注册到本地

			// server.setFlowController(); // App级别限流器
			server.connectToConfigServer("127.0.0.1:20001");
			server.publish(provider);
			server.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
