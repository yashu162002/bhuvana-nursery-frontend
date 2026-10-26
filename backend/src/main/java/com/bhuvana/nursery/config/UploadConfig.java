package com.bhuvana.nursery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class UploadConfig implements WebMvcConfigurer {

    private final Path uploadDirectory;

    public UploadConfig(
            @Value("${app.upload.directory:./uploads}") String directory
    ) {
        this.uploadDirectory = Path.of(directory)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDirectory.toUri().toString());
    }
}
