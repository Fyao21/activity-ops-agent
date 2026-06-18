import os
from typing import Any

from dotenv import load_dotenv
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI

from document_loader import load_document_text
from text_splitter import split_text
from vector_store import FaissVectorStore


load_dotenv()


NO_CONTEXT_ANSWER = "\u77e5\u8bc6\u5e93\u4e2d\u672a\u627e\u5230\u76f8\u5173\u4fe1\u606f"

RAG_SYSTEM_PROMPT = """\u4f60\u662f\u6d3b\u52a8\u8fd0\u8425\u77e5\u8bc6\u5e93\u95ee\u7b54\u52a9\u624b\u3002
\u4f60\u53ea\u80fd\u57fa\u4e8e\u63d0\u4f9b\u7684\u77e5\u8bc6\u5e93\u4e0a\u4e0b\u6587\u56de\u7b54\u95ee\u9898\u3002
\u5982\u679c\u4e0a\u4e0b\u6587\u4e2d\u6ca1\u6709\u7b54\u6848\uff0c\u8bf7\u56de\u7b54\u201c\u77e5\u8bc6\u5e93\u4e2d\u672a\u627e\u5230\u76f8\u5173\u4fe1\u606f\u201d\u3002
\u4e0d\u8981\u7f16\u9020\u4e0d\u5b58\u5728\u7684\u6d3b\u52a8\u89c4\u5219\u3002
\u56de\u7b54\u8981\u7b80\u6d01\u6e05\u6670\uff0c\u9002\u5408\u8fd0\u8425\u4eba\u5458\u9605\u8bfb\u3002
\u5982\u679c\u6d89\u53ca\u591a\u4e2a\u89c4\u5219\u70b9\uff0c\u8bf7\u5206\u6761\u8bf4\u660e\u3002"""


class RagService:
    def __init__(self) -> None:
        api_key = os.getenv("OPENAI_API_KEY", "").strip()
        base_url = os.getenv("OPENAI_BASE_URL", "").strip() or None
        model_name = os.getenv("MODEL_NAME", "").strip()
        if not api_key:
            raise RuntimeError("Missing required environment variable: OPENAI_API_KEY")
        if not model_name:
            raise RuntimeError("Missing required environment variable: MODEL_NAME")

        self.vector_store = FaissVectorStore()
        self.llm = ChatOpenAI(
            model=model_name,
            api_key=api_key,
            base_url=base_url,
            temperature=0,
            timeout=60,
        )

    def index_document(
        self,
        document_id: int,
        file_path: str,
        file_name: str,
    ) -> dict[str, Any]:
        text = load_document_text(file_path)
        documents = split_text(
            text,
            metadata={
                "document_id": document_id,
                "file_name": file_name,
                "file_path": file_path,
            },
        )
        vector_ids = self.vector_store.add_documents(documents)
        chunks = [
            {
                "document_id": document_id,
                "chunk_index": document.metadata["chunk_index"],
                "content": document.page_content,
                "vector_id": vector_id,
            }
            for document, vector_id in zip(documents, vector_ids)
        ]
        return {
            "success": True,
            "document_id": document_id,
            "chunk_count": len(documents),
            "message": "\u6587\u6863\u7d22\u5f15\u6210\u529f",
            "chunks": chunks,
        }

    def query(self, question: str, top_k: int = 4) -> dict[str, Any]:
        retrieve_result = self.retrieve(question, top_k)
        retrieved_chunks = retrieve_result["retrieved_chunks"]
        if not retrieved_chunks:
            return {
                "answer": NO_CONTEXT_ANSWER,
                "retrieved_chunks": [],
                "sources": [],
            }

        context = retrieve_result["context"]
        answer = self._generate_answer(question, context)
        return {
            "answer": answer or NO_CONTEXT_ANSWER,
            "retrieved_chunks": retrieved_chunks,
            "sources": retrieve_result["sources"],
        }

    def retrieve(self, question: str, top_k: int = 4) -> dict[str, Any]:
        results = self.vector_store.similarity_search(question, top_k)
        if not results:
            return {
                "retrieved_chunks": [],
                "sources": [],
                "context": "",
            }

        retrieved_chunks = [
            self._format_retrieved_chunk(document, score)
            for document, score in results
        ]
        sources = sorted(
            {
                chunk["file_name"]
                for chunk in retrieved_chunks
                if chunk.get("file_name")
            }
        )
        return {
            "retrieved_chunks": retrieved_chunks,
            "sources": sources,
            "context": self._build_context(retrieved_chunks),
        }

    def _generate_answer(self, question: str, context: str) -> str:
        messages = [
            SystemMessage(content=RAG_SYSTEM_PROMPT),
            HumanMessage(
                content=(
                    f"\u77e5\u8bc6\u5e93\u4e0a\u4e0b\u6587\uff1a\n{context}\n\n"
                    f"\u7528\u6237\u95ee\u9898\uff1a{question}\n\n"
                    "\u8bf7\u57fa\u4e8e\u77e5\u8bc6\u5e93\u4e0a\u4e0b\u6587\u56de\u7b54\u3002"
                )
            ),
        ]
        response = self.llm.invoke(messages)
        return str(response.content).strip()

    @staticmethod
    def _format_retrieved_chunk(document: Any, score: float) -> dict[str, Any]:
        metadata = document.metadata
        relevance_score = 1 / (1 + float(score))
        return {
            "document_id": metadata.get("document_id"),
            "chunk_index": metadata.get("chunk_index"),
            "content": document.page_content,
            "score": round(relevance_score, 6),
            "vector_id": metadata.get("vector_id"),
            "file_name": metadata.get("file_name"),
        }

    @staticmethod
    def _build_context(chunks: list[dict[str, Any]]) -> str:
        return "\n\n".join(
            (
                f"[\u6765\u6e90: {chunk.get('file_name', '')}, "
                f"document_id={chunk.get('document_id')}, "
                f"chunk_index={chunk.get('chunk_index')}]\n"
                f"{chunk.get('content', '')}"
            )
            for chunk in chunks
        )
