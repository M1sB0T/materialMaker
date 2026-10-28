package com.bot.materialmaker.controller;

import com.bot.materialmaker.common.ApiException;
import com.bot.materialmaker.dto.TemplateView;
import com.bot.materialmaker.mapper.TemplateMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 模板浏览：公开接口，无需登录。 */
@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateMapper templateMapper;

    public TemplateController(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @GetMapping
    public List<TemplateView> list(@RequestParam(required = false) String category) {
        return templateMapper.findAll().stream()
                .filter(t -> category == null || category.isBlank() || category.equals(t.getCategory()))
                .map(this::toView)
                .toList();
    }

    @GetMapping("/{id}")
    public TemplateView detail(@PathVariable String id) {
        var template = templateMapper.findById(id);
        if (template == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "模板不存在");
        }
        return toView(template);
    }

    private TemplateView toView(com.bot.materialmaker.model.Template t) {
        return new TemplateView(t.getId(), t.getTitle(), t.getCategory(), t.getCoverUrl(),
                t.getDescription(), t.getRequiredImageCount(), t.acceptedMimeList(),
                t.getMaxFileSizeBytes());
    }
}
