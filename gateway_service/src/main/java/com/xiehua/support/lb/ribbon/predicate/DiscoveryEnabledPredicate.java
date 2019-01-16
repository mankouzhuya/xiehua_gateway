/**
 * Copyright (c) 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiehua.support.lb.ribbon.predicate;

import com.netflix.loadbalancer.AbstractServerPredicate;
import com.netflix.loadbalancer.PredicateKey;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;


public abstract class DiscoveryEnabledPredicate extends AbstractServerPredicate {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean apply(PredicateKey input) {
        return input != null && input.getServer() instanceof DiscoveryEnabledServer && apply((DiscoveryEnabledServer) input.getServer(),input.getLoadBalancerKey());
    }

    /**
     * Returns whether the specific {@link DiscoveryEnabledServer} matches this predicate.
     *
     * @param server the discovered server
     * @param loadBalancerKey
     * @return whether the server matches the predicate
     */
    protected abstract boolean apply(DiscoveryEnabledServer server,Object loadBalancerKey);
}
