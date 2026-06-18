package com.example.activityagent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.activityagent.dto.DocumentUploadResponse;
import com.example.activityagent.dto.KnowledgeQueryRequest;
import com.example.activityagent.vo.KnowledgeDocumentVO;
import com.example.activityagent.vo.KnowledgeQueryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeService {
    DocumentUploadResponse upload(MultipartFile file);

    IPage<KnowledgeDocumentVO> listDocuments(long pageNum, long pageSize);

    KnowledgeQueryResponse query(KnowledgeQueryRequest request);
}
