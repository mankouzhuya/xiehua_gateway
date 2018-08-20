/**
 * Copyright (c) 2015 the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xiehua.support.lb.ribbon.predicate;

import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A default implementation of {@link DiscoveryEnabledServer} that matches the instance against the attributes
 * registered through
 * @see DiscoveryEnabledPredicate
 */
public class MetadataAwarePredicate extends DiscoveryEnabledPredicate {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean apply(DiscoveryEnabledServer server, Object loadBalancerKey) {
        if (loadBalancerKey != null) {
            Map<String, String> map = (Map<String, String>) loadBalancerKey;
            final Set<Map.Entry<String, String>> attributes = Collections.unmodifiableSet(map.entrySet());
            final Map<String, String> metadata = server.getInstanceInfo().getMetadata();
            return metadata.entrySet().containsAll(attributes);
        }
        return true;

    }
}
