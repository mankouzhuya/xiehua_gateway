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
package com.xiehua.support.lb;

import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;
import com.xiehua.support.lb.filter.XiehuaLoadBalancerClientFilter;
import com.xiehua.support.lb.ribbon.XiehuaRibbonLoadBalancerClient;
import com.xiehua.support.lb.ribbon.rule.DiscoveryEnabledRule;
import com.xiehua.support.lb.ribbon.rule.MetadataAwareRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * The Ribbon discovery filter auto configuration.
 *
 * @author Jakub Narloch
 */
@Configuration
@ConditionalOnClass(DiscoveryEnabledNIWSServerList.class)
@AutoConfigureBefore(RibbonClientConfiguration.class)
@ConditionalOnProperty(value = "ribbon.filter.metadata.enabled", matchIfMissing = true)
public class RibbonDiscoveryRuleAutoConfiguration {

    @Autowired
    private SpringClientFactory factory;

    @Bean
    @ConditionalOnMissingBean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DiscoveryEnabledRule metadataAwareRule() {
        return new MetadataAwareRule();
    }

    @Bean
    public XiehuaLoadBalancerClientFilter balancerClientFilter(){
        return new XiehuaLoadBalancerClientFilter(loadBalancerClient());
    }



    @Bean
    public XiehuaRibbonLoadBalancerClient loadBalancerClient() {
        return new XiehuaRibbonLoadBalancerClient(factory);
    }
}
