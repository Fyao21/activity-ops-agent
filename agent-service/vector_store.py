import os
import logging
from pathlib import Path
from threading import Lock

from dotenv import load_dotenv
from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from langchain_openai import OpenAIEmbeddings

try:
    from langchain_huggingface import HuggingFaceEmbeddings
except ImportError:  # pragma: no cover - compatibility for older installs
    from langchain_community.embeddings import HuggingFaceEmbeddings


load_dotenv()


DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small"
DEFAULT_LOCAL_EMBEDDING_MODEL = "BAAI/bge-small-zh-v1.5"
DEFAULT_VECTOR_STORE_PATH = "./vector_store"
INDEX_NAME = "knowledge"

logger = logging.getLogger(__name__)


class EmbeddingProvider:
    def __init__(self) -> None:
        provider = os.getenv("EMBEDDING_PROVIDER", "openai").strip().lower()
        if provider == "local":
            model = (
                os.getenv("EMBEDDING_MODEL", "").strip()
                or DEFAULT_LOCAL_EMBEDDING_MODEL
            )
            self.embeddings = HuggingFaceEmbeddings(
                model_name=model,
                encode_kwargs={"normalize_embeddings": True},
            )
            return

        if provider != "openai":
            raise RuntimeError(
                "Unsupported EMBEDDING_PROVIDER. Use 'local' or 'openai'."
            )

        api_key = (
            os.getenv("EMBEDDING_API_KEY", "").strip()
            or os.getenv("OPENAI_API_KEY", "").strip()
        )
        base_url = (
            os.getenv("EMBEDDING_BASE_URL", "").strip()
            or os.getenv("OPENAI_BASE_URL", "").strip()
            or None
        )
        model = os.getenv("EMBEDDING_MODEL", "").strip() or DEFAULT_EMBEDDING_MODEL
        if not api_key:
            raise RuntimeError("Missing required environment variable: OPENAI_API_KEY")

        self.embeddings = OpenAIEmbeddings(
            model=model,
            api_key=api_key,
            base_url=base_url,
            timeout=60,
        )

    def get_embeddings(self) -> Embeddings:
        return self.embeddings


class FaissVectorStore:
    def __init__(self, embeddings: Embeddings | None = None) -> None:
        self.embeddings = embeddings or EmbeddingProvider().get_embeddings()
        self.store_path = Path(
            os.getenv("VECTOR_STORE_PATH", DEFAULT_VECTOR_STORE_PATH)
        ).expanduser()
        if not self.store_path.is_absolute():
            self.store_path = Path(__file__).resolve().parent / self.store_path
        self.store_path = self.store_path.resolve()
        self._lock = Lock()

    def add_documents(self, documents: list[Document]) -> list[str]:
        if not documents:
            return []

        ids = [str(document.metadata["vector_id"]) for document in documents]
        with self._lock:
            store = self._load()
            if store is None:
                store = FAISS.from_documents(documents, self.embeddings, ids=ids)
            else:
                self._delete_document_chunks(store, documents[0].metadata["document_id"])
                try:
                    store.add_documents(documents, ids=ids)
                except Exception:
                    logger.exception(
                        "Failed to append documents to existing FAISS store. "
                        "Rebuilding store with current embedding model."
                    )
                    store = FAISS.from_documents(documents, self.embeddings, ids=ids)
            self._save(store)
        return ids

    def similarity_search(
        self,
        query: str,
        top_k: int,
        course_id: int | None = None,
    ) -> list[tuple[Document, float]]:
        store = self._load()
        if store is None:
            return []

        fetch_k = max(top_k * 8, 20) if course_id is not None else top_k
        try:
            results = store.similarity_search_with_score(query, k=fetch_k)
        except Exception:
            logger.exception("FAISS similarity search failed.")
            return []

        if course_id is None:
            return results[:top_k]

        filtered = [
            (document, score)
            for document, score in results
            if document.metadata.get("course_id") == course_id
        ]
        return filtered[:top_k]

    def delete_document(self, document_id: int) -> int:
        with self._lock:
            store = self._load()
            if store is None:
                return 0

            deleted_count = self._delete_document_chunks(store, document_id)
            if deleted_count > 0:
                self._save(store)
            return deleted_count

    def _load(self) -> FAISS | None:
        index_file = self.store_path / f"{INDEX_NAME}.faiss"
        pickle_file = self.store_path / f"{INDEX_NAME}.pkl"
        if not index_file.exists() or not pickle_file.exists():
            return None
        try:
            return FAISS.load_local(
                str(self.store_path),
                self.embeddings,
                index_name=INDEX_NAME,
                allow_dangerous_deserialization=True,
            )
        except Exception:
            logger.exception("Failed to load FAISS vector store.")
            return None

    def _save(self, store: FAISS) -> None:
        self.store_path.mkdir(parents=True, exist_ok=True)
        store.save_local(str(self.store_path), index_name=INDEX_NAME)

    @staticmethod
    def _delete_document_chunks(store: FAISS, document_id: int) -> int:
        doc_ids = []
        for docstore_id in store.index_to_docstore_id.values():
            document = store.docstore.search(docstore_id)
            metadata = getattr(document, "metadata", {})
            if metadata.get("document_id") == document_id:
                doc_ids.append(docstore_id)
        if doc_ids:
            store.delete(ids=doc_ids)
        return len(doc_ids)
