# xiehua_gateway
this is a java gateway project,And the project is dependent on spring cloud gateway and spring security.add dynamic route rule support and jwt support.   
features:(支持集群部署)   
1.基于spring cloud gateway 2.0.0.RELEASE   
2.使用spring security + JWT 做安全校验   
3.IP白名单   
4.限流(IP限流+系统负载限流)  
5.基于配置的(版本号或者其他表示)动态路由(可做灰度发布或AB test)
6.路由耗时监控  
7.自定义采样率  
8.链路追踪(使用agent技术,不侵入业务代码,目前仅支持feign client调用的链路追踪,使用该功能时熔断策略要修改为SEMAPHORE,暂不支持线程池方式的熔断)  
9.统一入参和出参抓取  
//TODO  
优雅下线  

