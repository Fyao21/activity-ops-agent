package com.example.activityagent.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.activityagent.client.PythonRagClient;
import com.example.activityagent.dto.RagIndexResponse;
import com.example.activityagent.entity.KnowledgeChunk;
import com.example.activityagent.entity.KnowledgeDocument;
import com.example.activityagent.mapper.KnowledgeChunkMapper;
import com.example.activityagent.mapper.KnowledgeDocumentMapper;
import com.example.activityagent.mq.constant.RocketMqConstant;
import com.example.activityagent.mq.dto.KnowledgeIndexMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = RocketMqConstant.KNOWLEDGE_INDEX_TOPIC,
    consumerGroup = RocketMqConstant.KNOWLEDGE_INDEX_CONSUMER_GROUP,
    selectorExpression = RocketMqConstant.TAG_DOC_INDEX
)
public class KnowledgeIndexConsumer implements RocketMQListener<KnowledgeIndexMessage> {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final PythonRagClient pythonRagClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(KnowledgeIndexMessage message) {
        log.info("Received knowledge index message: {}", message);
        try {
            KnowledgeDocument document = knowledgeDocumentMapper.selectById(message.getDocumentId());
            if (document == null) {
                throw new IllegalStateException("Knowledge document not found: " + message.getDocumentId());
            }
            if (KnowledgeDocument.STATUS_SUCCESS == document.getStatus()) {
                log.info("Knowledge document already indexed, documentId={}", document.getId());
                return;
            }

            RagIndexResponse response = pythonRagClient.index(
                document.getId(),
                document.getCourseId(),
                document.getFilePath(),
                document.getFileName()
            );
            if (Boolean.TRUE.equals(response.getSuccess())) {
                saveChunks(document, response);
                markSuccess(document.getId(), response.getChunkCount());
                log.info("Knowledge document indexed successfully, documentId={}, chunkCount={}", document.getId(), response.getChunkCount());
                return;
            }

            String errorMessage = StringUtils.hasText(response.getMessage()) ? response.getMessage() : "Python RAG index failed";
            markFailed(document.getId(), errorMessage);
            log.warn("Knowledge document index failed, documentId={}, message={}", document.getId(), errorMessage);
        } catch (Exception ex) {
            log.error("Consume knowledge index message failed, payload={}", message, ex);
            if (message.getDocumentId() != null) {
                markFailed(message.getDocumentId(), ex.getMessage());
            }
            throw ex;
        }
    }

    private void markSuccess(Long documentId, Integer chunkCount) {
        KnowledgeDocument update = new KnowledgeDocument();
        update.setId(documentId);
        update.setStatus(KnowledgeDocument.STATUS_SUCCESS);
        update.setChunkCount(chunkCount == null ? 0 : chunkCount);
        update.setErrorMessage(null);
        knowledgeDocumentMapper.updateById(update);
    }

    private void saveChunks(KnowledgeDocument document, RagIndexResponse response) {
        knowledgeChunkMapper.delete(
            new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getDocumentId, document.getId())
        );
        if (response.getChunks() == null || response.getChunks().isEmpty()) {
            return;
        }
        for (RagIndexResponse.RagChunkInfo chunkInfo : response.getChunks()) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setCourseId(document.getCourseId());
            chunk.setChunkIndex(chunkInfo.getChunkIndex());
            chunk.setContent(chunkInfo.getContent());
            chunk.setVectorId(chunkInfo.getVectorId());
            knowledgeChunkMapper.insert(chunk);
        }
    }

    private void markFailed(Long documentId, String errorMessage) {
        KnowledgeDocument update = new KnowledgeDocument();
        update.setId(documentId);
        update.setStatus(KnowledgeDocument.STATUS_FAILED);
        update.setErrorMessage(errorMessage);
        knowledgeDocumentMapper.updateById(update);
    }
}
