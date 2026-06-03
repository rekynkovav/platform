package com.platform.config;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import java.lang.annotation.*;

/**
 * Meta-annotation for @WebMvcTest controller tests.
 * Disables security filters so that authentication() post-processor
 * can set SecurityContext directly without OAuth2 filter chain interference.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AutoConfigureMockMvc(addFilters = false)
public @interface TestWebMvc {
}
