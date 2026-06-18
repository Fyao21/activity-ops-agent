import hashlib
import math
import os
import re
from pathlib import Path
from threading import Lock

from dotenv import load_dotenv
from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from langchain_openai import OpenAIEmbeddings


load_dotenv()


DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small"
DEFAULT_VECTOR_STORE_PATH = "./vector_store"
INDEX_NAME = "knowledge"
LOCAL_EMBEDDING_DIMENSIONS = 384


class LocalHashEmbeddings(Embeddings):
    """Deterministic local embeddings for environments without an embedding API."""

    def __init__(self, dimensions: int = LOCAL_EMBEDDING_DIMENSIONS) -> None:
        self.dimensions = dimensions

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self._embed(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._embed(text)

    def _embed(self, text: str) -> list[float]:
        vector = [0.0] * self.dimensions
        for token in self._tokens(text):
            digest = hashlib.md5(token.encode("utf-8")).digest()
            index = int.from_bytes(digest[:4], "big") % self.dimensions
            sign = 1.0 if digest[4] % 2 == 0 else -1.0
            vector[index] += sign

        norm = math.sqrt(sum(value * value for value in vector))
        if norm == 0:
            return vector
        return [value / norm for value in vector]

    @staticmethod
    def _tokens(text: str) -> list[str]:
        lowered = text.lower()
        words = re.findall(r"[a-z0-9]+", lowered)
        chars = [char for char in lowered if "\u4e00" <= char <= "\u9fff"]
        bigrams = [
            "".join(chars[index : index + 2])
            for index in range(max(len(chars) - 1, 0))
        ]
        return words + chars + bigrams


class EmbeddingProvider:
    def __init__(self) -> None:
        provider = os.getenv("EMBEDDING_PROVIDER", "openai").strip().lower()
        if provider == "local":
            self.embeddings = LocalHashEmbeddings()
            return

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
                store.add_documents(documents, ids=ids)
            self._save(store)
        return ids

    def similarity_search(self, query: str, top_k: int) -> list[tuple[Document, float]]:
        store = self._load()
        if store is None:
            return []
        return store.similarity_search_with_score(query, k=top_k)

    def _load(self) -> FAISS | None:
        index_file = self.store_path / f"{INDEX_NAME}.faiss"
        pickle_file = self.store_path / f"{INDEX_NAME}.pkl"
        if not index_file.exists() or not pickle_file.exists():
            return None
        return FAISS.load_local(
            str(self.store_path),
            self.embeddings,
            index_name=INDEX_NAME,
            allow_dangerous_deserialization=True,
        )

    def _save(self, store: FAISS) -> None:
        self.store_path.mkdir(parents=True, exist_ok=True)
        store.save_local(str(self.store_path), index_name=INDEX_NAME)

    @staticmethod
    def _delete_document_chunks(store: FAISS, document_id: int) -> None:
        doc_ids = []
        for docstore_id in store.index_to_docstore_id.values():
            document = store.docstore.search(docstore_id)
            metadata = getattr(document, "metadata", {})
            if metadata.get("document_id") == document_id:
                doc_ids.append(docstore_id)
        if doc_ids:
            store.delete(ids=doc_ids)
