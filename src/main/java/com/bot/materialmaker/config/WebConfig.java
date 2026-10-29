package com.bot.materialmaker.config;

import com.bot.materialmaker.auth.AuthInterceptor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
        // 演示资源（封面/示例图）：no-cache 强制浏览器每次向服务器校验新旧，
        // 避免换图后仍显示旧缓存（同地址文件内容变化时）
        registry.addResourceHandler("/demo/**")
                .addResourceLocations("classpath:/static/demo/")
                .setCacheControl(CacheControl.noCache());
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // JSON 响应显式声明 charset=UTF-8：部分浏览器对无 charset 的
        // application/json 会按系统 GBK 解码，导致直接打开接口时中文乱码
        MappingJackson2HttpMessageConverter utf8Json = new MappingJackson2HttpMessageConverter();
        utf8Json.setSupportedMediaTypes(List.of(new MediaType("application", "json", StandardCharsets.UTF_8)));
        converters.add(0, utf8Json);
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
