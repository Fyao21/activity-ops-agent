package com.example.activityagent.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.activityagent.dto.KnowledgeQueryRequest;
import com.example.activityagent.dto.KnowledgeUploadResponse;
import com.example.activityagent.vo.KnowledgeDocumentVO;
import com.example.activityagent.vo.KnowledgeQueryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeService {
    KnowledgeUploadResponse upload(Long courseId, MultipartFile file);

    IPage<KnowledgeDocumentVO> listDocuments(Long courseId, long pageNum, long pageSize);

    KnowledgeQueryResponse query(KnowledgeQueryRequest request);

    Boolean delete(Long id);
}
