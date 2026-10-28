package com.bot.materialmaker.controller;

import com.bot.materialmaker.auth.AuthInterceptor;
import com.bot.materialmaker.dto.MaterialView;
import com.bot.materialmaker.service.MaterialService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping
    public MaterialView upload(@RequestParam("templateId") String templateId,
                               @RequestParam("file") MultipartFile file,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
        return materialService.upload(userId, templateId, file);
    }
}
