package com.example.activityagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.activityagent.client.PythonRagClient;
import com.example.activityagent.common.BusinessException;
import com.example.activityagent.dto.DocumentUploadResponse;
import com.example.activityagent.dto.KnowledgeQueryRequest;
import com.example.activityagent.entity.KnowledgeChunk;
import com.example.activityagent.entity.KnowledgeDocument;
import com.example.activityagent.mapper.KnowledgeChunkMapper;
import com.example.activityagent.mapper.KnowledgeDocumentMapper;
import com.example.activityagent.service.KnowledgeService;
import com.example.activityagent.vo.KnowledgeDocumentVO;
import com.example.activityagent.vo.KnowledgeQueryResponse;
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
    private final PythonRagClient pythonRagClient;

    @Override
    @Transactional
    public DocumentUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file must not be empty");
        }

        String originalName = normalizeFileName(file.getOriginalFilename());
        String fileType = extractFileType(originalName);
        if (!SUPPORTED_TYPES.contains(fileType)) {
            throw new BusinessException("Only txt and md files are supported");
        }

        Path savedPath = saveFile(file, originalName, fileType);
        KnowledgeDocument document = createPendingDocument(originalName, fileType, savedPath);
        knowledgeDocumentMapper.insert(document);

        try {
            // Python reads the uploaded file directly, so pass an absolute path.
            PythonRagClient.RagIndexResponse indexResponse = pythonRagClient.index(
                document.getId(),
                savedPath.toAbsolutePath().toString(),
                originalName
            );
            if (!Boolean.TRUE.equals(indexResponse.getSuccess())) {
                throw new BusinessException(indexResponse.getMessage());
            }
            int chunkCount = indexResponse.getChunkCount() == null ? 0 : indexResponse.getChunkCount();
            saveChunks(document.getId(), indexResponse);
            markSuccess(document.getId(), chunkCount);
            return buildUploadResponse(document.getId(), originalName, fileType, KnowledgeDocument.STATUS_SUCCESS, chunkCount, "Document indexed successfully");
        } catch (Exception ex) {
            log.error("Index knowledge document failed, documentId={}", document.getId(), ex);
            markFailed(document.getId(), ex.getMessage());
            return buildUploadResponse(document.getId(), originalName, fileType, KnowledgeDocument.STATUS_FAILED, 0, ex.getMessage());
        }
    }

    @Override
    public IPage<KnowledgeDocumentVO> listDocuments(long pageNum, long pageSize) {
        Page<KnowledgeDocument> page = new Page<>(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
        IPage<KnowledgeDocument> entityPage = knowledgeDocumentMapper.selectPage(
            page,
            new LambdaQueryWrapper<KnowledgeDocument>()
                .orderByDesc(KnowledgeDocument::getCreateTime)
        );

        Page<KnowledgeDocumentVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(
            entityPage.getRecords().stream()
                .map(this::toDocumentVO)
                .toList()
        );
        return voPage;
    }

    @Override
    public KnowledgeQueryResponse query(KnowledgeQueryRequest request) {
        return pythonRagClient.query(request.getQuestion(), request.getTopK());
    }

    private Path saveFile(MultipartFile file, String originalName, String fileType) {
        // Keep files under uploads/knowledge and avoid trusting client-side paths.
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

    private KnowledgeDocument createPendingDocument(String fileName, String fileType, Path savedPath) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setFileName(fileName);
        document.setFileType(fileType);
        document.setFilePath(savedPath.toString());
        document.setStatus(KnowledgeDocument.STATUS_PENDING);
        document.setChunkCount(0);
        return document;
    }

    private void saveChunks(Long documentId, PythonRagClient.RagIndexResponse indexResponse) {
        if (indexResponse.getChunks() == null || indexResponse.getChunks().isEmpty()) {
            return;
        }
        for (PythonRagClient.RagChunkInfo chunkInfo : indexResponse.getChunks()) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(chunkInfo.getChunkIndex());
            chunk.setContent(chunkInfo.getContent());
            chunk.setVectorId(chunkInfo.getVectorId());
            knowledgeChunkMapper.insert(chunk);
        }
    }

    private void markSuccess(Long documentId, int chunkCount) {
        KnowledgeDocument update = new KnowledgeDocument();
        update.setId(documentId);
        update.setStatus(KnowledgeDocument.STATUS_SUCCESS);
        update.setChunkCount(chunkCount);
        update.setErrorMessage(null);
        knowledgeDocumentMapper.updateById(update);
    }

    private void markFailed(Long documentId, String errorMessage) {
        KnowledgeDocument update = new KnowledgeDocument();
        update.setId(documentId);
        update.setStatus(KnowledgeDocument.STATUS_FAILED);
        update.setChunkCount(0);
        update.setErrorMessage(errorMessage);
        knowledgeDocumentMapper.updateById(update);
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

    private DocumentUploadResponse buildUploadResponse(
        Long documentId,
        String fileName,
        String fileType,
        Integer status,
        Integer chunkCount,
        String message
    ) {
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setDocumentId(documentId);
        response.setFileName(fileName);
        response.setFileType(fileType);
        response.setStatus(status);
        response.setChunkCount(chunkCount);
        response.setMessage(message);
        return response;
    }
}
