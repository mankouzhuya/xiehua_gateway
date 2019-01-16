package com.xiehua.filter;

import org.springframework.core.Ordered;

public interface XiehuaOrdered extends Ordered {

    int COUNTER_ORDER = 0;

    int RATE_LIMIT_CPU_ORDER = 10;

    int ROUTE_PRE_ORDER = 20;

    int RATE_LIMIT_IP_ORDER = 30;

    int TRACK_ORDER = 40;

}
