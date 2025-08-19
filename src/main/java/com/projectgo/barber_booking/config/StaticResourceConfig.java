package com.projectgo.barber_booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/css/**")
        .addResourceLocations("classpath:/static/css/", "file:src/main/resources/static/css/");
    registry.addResourceHandler("/js/**")
        .addResourceLocations("classpath:/static/js/", "file:src/main/resources/static/js/");
    registry.addResourceHandler("/images/**")
        .addResourceLocations("classpath:/static/images/", "file:src/main/resources/static/images/");
    registry.addResourceHandler("/fonts/**")
        .addResourceLocations("classpath:/static/fonts/", "file:src/main/resources/static/fonts/");
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("classpath:/static/uploads/", "file:src/main/resources/static/uploads/");
  }
}

