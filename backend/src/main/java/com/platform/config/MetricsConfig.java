package com.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {
    // TimedAspect bean is defined in com.platform.aspect.TimedAspect (@Component)
    // No duplicate bean definition needed here
}
