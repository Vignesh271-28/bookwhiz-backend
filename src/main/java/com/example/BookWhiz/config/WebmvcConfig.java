package com.example.BookWhiz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Paths;

@Configuration
public class WebmvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/posters}")
    private String uploadDir;

    /**
     * Maps GET /uploads/posters/filename.jpg
     *   → ./uploads/posters/filename.jpg on disk
     *
     * The "file:" prefix tells Spring to look on the filesystem,
     * not inside the JAR classpath.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Resolve the FULL absolute path to the uploads root
        // e.g. uploadDir = "uploads/posters"
        //   → uploadRoot = "/home/user/project/uploads/"
        String uploadRoot = Paths.get(uploadDir)          // uploads/posters
                                 .toAbsolutePath()         // /home/.../uploads/posters
                                 .getParent()              // /home/.../uploads
                                 .normalize()
                                 .toString()
                                 .replace("\\", "/");      // Windows safety

        registry
            .addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadRoot + "/");
    }
}