/*
 * Licensed to Metamarkets Group Inc. (Metamarkets) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Metamarkets licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.druid.server.coordination.broker;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.metamx.common.lifecycle.LifecycleStart;
import com.metamx.common.lifecycle.LifecycleStop;
import io.druid.client.ServerInventoryView;
import io.druid.client.ServerView;
import io.druid.curator.discovery.ServiceAnnouncer;
import io.druid.guice.ManageLifecycle;
import io.druid.guice.annotations.Self;
import io.druid.server.DruidNode;

@ManageLifecycle
public class DruidBroker
{
  private final DruidNode self;
  private final ServiceAnnouncer serviceAnnouncer;
  private volatile boolean started = false;

  @Inject
  public DruidBroker(
      final ServerInventoryView serverInventoryView,
      final @Self DruidNode self,
      final ServiceAnnouncer serviceAnnouncer
  )
  {
    this.self = self;
    this.serviceAnnouncer = serviceAnnouncer;

    serverInventoryView.registerSegmentCallback(
        MoreExecutors.sameThreadExecutor(),
        new ServerView.BaseSegmentCallback()
        {
          @Override
          public ServerView.CallbackAction segmentViewInitialized()
          {
            serviceAnnouncer.announce(self);
            return ServerView.CallbackAction.UNREGISTER;
          }
        }
    );
  }

  @LifecycleStart
  public void start()
  {
    synchronized (self) {
      if(started) {
        return;
      }
      started = true;
    }
  }

  @LifecycleStop
  public void stop()
  {
    synchronized (self) {
      if (!started) {
        return;
      }
      serviceAnnouncer.unannounce(self);
      started = false;
    }
  }
}
