package com.example.activityagent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.activityagent.common.Result;
import com.example.activityagent.dto.KnowledgeQueryRequest;
import com.example.activityagent.dto.KnowledgeUploadResponse;
import com.example.activityagent.service.KnowledgeService;
import com.example.activityagent.vo.KnowledgeDocumentVO;
import com.example.activityagent.vo.KnowledgeQueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/upload")
    public Result<KnowledgeUploadResponse> upload(
        @RequestParam("courseId") Long courseId,
        @RequestParam("file") MultipartFile file
    ) {
        return Result.success(knowledgeService.upload(courseId, file));
    }

    @GetMapping("/list")
    public Result<IPage<KnowledgeDocumentVO>> list(
        @RequestParam("courseId") Long courseId,
        @RequestParam(defaultValue = "1") long pageNum,
        @RequestParam(defaultValue = "10") long pageSize
    ) {
        return Result.success(knowledgeService.listDocuments(courseId, pageNum, pageSize));
    }

    @PostMapping("/query")
    public Result<KnowledgeQueryResponse> query(@Valid @RequestBody KnowledgeQueryRequest request) {
        return Result.success(knowledgeService.query(request));
    }
}
