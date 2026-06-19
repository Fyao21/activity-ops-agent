package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activityagent.client.PythonRagClient;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.dto.KnowledgeQueryRequest;
import com.example.activityagent.dto.KnowledgeUploadResponse;
import com.example.activityagent.dto.RagQueryResponse;
import com.example.activityagent.entity.AgentQaRecord;
import com.example.activityagent.entity.KnowledgeChunk;
import com.example.activityagent.entity.KnowledgeDocument;
import com.example.activityagent.mapper.AgentQaRecordMapper;
import com.example.activityagent.mapper.CourseMapper;
import com.example.activityagent.mapper.KnowledgeChunkMapper;
import com.example.activityagent.mapper.KnowledgeDocumentMapper;
import com.example.activityagent.mq.dto.KnowledgeIndexMessage;
import com.example.activityagent.mq.producer.KnowledgeIndexProducer;
import com.example.activityagent.service.KnowledgeService;
import com.example.activityagent.vo.KnowledgeDocumentVO;
import com.example.activityagent.vo.KnowledgeQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final Path UPLOAD_DIR = Paths.get("uploads", "knowledge");
    private static final Set<String> SUPPORTED_TYPES = Set.of("txt", "md");

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final CourseMapper courseMapper;
    private final AgentQaRecordMapper agentQaRecordMapper;
    private final PythonRagClient pythonRagClient;
    private final KnowledgeIndexProducer knowledgeIndexProducer;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeUploadResponse upload(Long courseId, MultipartFile file) {
        if (courseId == null) {
            throw new BusinessException("courseId must not be null");
        }
        if (courseMapper.selectById(courseId) == null) {
            throw new BusinessException("Course does not exist: " + courseId);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file must not be empty");
        }

        String originalName = normalizeFileName(file.getOriginalFilename());
        String fileType = extractFileType(originalName);
        if (!SUPPORTED_TYPES.contains(fileType)) {
            throw new BusinessException("Only txt and md files are supported");
        }

        Path savedPath = saveFile(file, originalName);
        KnowledgeDocument document = createPendingDocument(courseId, originalName, fileType, savedPath);
        knowledgeDocumentMapper.insert(document);

        // Upload returns immediately after enqueueing. Python RAG indexing runs in RocketMQ consumer.
        sendKnowledgeIndexMessage(document);
        return buildUploadResponse(document, "Document uploaded, waiting for async indexing");
    }

    @Override
    public IPage<KnowledgeDocumentVO> listDocuments(Long courseId, long pageNum, long pageSize) {
        if (courseId == null) {
            throw new BusinessException("courseId must not be null");
        }
        Page<KnowledgeDocument> page = new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
        IPage<KnowledgeDocument> entityPage = knowledgeDocumentMapper.selectPage(
            page,
            new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getCourseId, courseId)
                .orderByDesc(KnowledgeDocument::getCreateTime)
        );

        Page<KnowledgeDocumentVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toDocumentVO).toList());
        return voPage;
    }

    @Override
    public KnowledgeQueryResponse query(KnowledgeQueryRequest request) {
        try {
            RagQueryResponse response = pythonRagClient.query(request.getCourseId(), request.getQuestion(), request.getTopK());
            saveRagQaRecord(request, response, true, null);
            return response;
        } catch (RuntimeException ex) {
            KnowledgeQueryResponse failedResponse = new KnowledgeQueryResponse();
            failedResponse.setAnswer("");
            saveRagQaRecord(request, failedResponse, false, ex.getMessage());
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        if (id == null) {
            throw new BusinessException("document id must not be null");
        }

        KnowledgeDocument document = knowledgeDocumentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("Knowledge document does not exist: " + id);
        }

        // Indexed documents may have vectors in FAISS, so remove vectors before deleting database rows.
        boolean mayHaveVectors = Integer.valueOf(KnowledgeDocument.STATUS_SUCCESS).equals(document.getStatus())
            || (document.getChunkCount() != null && document.getChunkCount() > 0);
        if (mayHaveVectors) {
            pythonRagClient.delete(id);
        }

        knowledgeChunkMapper.delete(
            new LambdaQueryWrapper<KnowledgeChunk>().eq(KnowledgeChunk::getDocumentId, id)
        );
        knowledgeDocumentMapper.deleteById(id);
        deleteLocalFile(document.getFilePath());
        return true;
    }

    private void sendKnowledgeIndexMessage(KnowledgeDocument document) {
        KnowledgeIndexMessage message = new KnowledgeIndexMessage();
        message.setDocumentId(document.getId());
        message.setCourseId(document.getCourseId());
        message.setFilePath(document.getFilePath());
        message.setFileName(document.getFileName());
        message.setEventTime(LocalDateTime.now());
        knowledgeIndexProducer.send(message);
    }

    private Path saveFile(MultipartFile file, String originalName) {
        try {
            Files.createDirectories(UPLOAD_DIR);
            String savedName = UUID.randomUUID() + "-" + originalName;
            Path target = UPLOAD_DIR.resolve(savedName).normalize();
            if (!target.startsWith(UPLOAD_DIR)) {
                throw new BusinessException("Invalid file name");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException ex) {
            throw new BusinessException("Save uploaded file failed");
        }
    }

    private String normalizeFileName(String fileName) {
        String cleaned = StringUtils.cleanPath(fileName == null ? "" : fileName);
        String name = Paths.get(cleaned).getFileName() == null ? "" : Paths.get(cleaned).getFileName().toString();
        if (!StringUtils.hasText(name) || name.contains("..")) {
            throw new BusinessException("Invalid file name");
        }
        return name;
    }

    private KnowledgeDocument createPendingDocument(Long courseId, String fileName, String fileType, Path savedPath) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setCourseId(courseId);
        document.setFileName(fileName);
        document.setFileType(fileType);
        document.setFilePath(savedPath.toString());
        document.setStatus(KnowledgeDocument.STATUS_PENDING);
        document.setChunkCount(0);
        return document;
    }

    private String extractFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new BusinessException("File extension is required");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private KnowledgeDocumentVO toDocumentVO(KnowledgeDocument document) {
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        BeanUtils.copyProperties(document, vo);
        return vo;
    }

    private KnowledgeUploadResponse buildUploadResponse(KnowledgeDocument document, String message) {
        KnowledgeUploadResponse response = new KnowledgeUploadResponse();
        response.setDocumentId(document.getId());
        response.setCourseId(document.getCourseId());
        response.setFileName(document.getFileName());
        response.setFileType(document.getFileType());
        response.setStatus(document.getStatus());
        response.setChunkCount(document.getChunkCount());
        response.setMessage(message);
        return response;
    }

    private void deleteLocalFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException ex) {
            // Database rows and vectors are already removed; keep deletion idempotent and log file cleanup failure.
            log.warn("Delete local knowledge file failed, filePath={}", filePath, ex);
        }
    }

    private void saveRagQaRecord(
        KnowledgeQueryRequest request,
        KnowledgeQueryResponse response,
        boolean success,
        String errorMessage
    ) {
        AgentQaRecord record = new AgentQaRecord();
        record.setUserId(request.getUserId() == null ? 1L : request.getUserId());
        record.setCourseId(request.getCourseId());
        record.setQuestion(request.getQuestion());
        record.setRouteType("RAG");
        record.setRetrievedContext(toJson(response.getRetrievedChunks()));
        record.setAnswer(response.getAnswer());
        record.setSuccess(success ? 1 : 0);
        record.setErrorMessage(errorMessage);
        agentQaRecordMapper.insert(record);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
