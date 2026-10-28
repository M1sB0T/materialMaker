package com.bot.materialmaker.config;

import com.bot.materialmaker.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final StorageProperties storageProperties;

    public WebConfig(AuthInterceptor authInterceptor, StorageProperties storageProperties) {
        this.authInterceptor = authInterceptor;
        this.storageProperties = storageProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 模板浏览公开；任务与素材接口要求登录
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/tasks/**", "/api/materials/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将本地上传目录与结果目录通过 /files/** 对外提供访问
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + storageProperties.getRoot() + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:3000", "http://127.0.0.1:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
