package com.taskflow.controller;

import com.taskflow.dto.response.ApiResponse;
import com.taskflow.dto.response.PageResponse;
import com.taskflow.service.PageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pages")
@RequiredArgsConstructor
public class GlobalPageController {

    private final PageService pageService;

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<PageResponse>>> getFavoritePages() {
        List<PageResponse> pages = pageService.getFavoritePages();
        return ResponseEntity.ok(ApiResponse.success(pages));
    }
}
