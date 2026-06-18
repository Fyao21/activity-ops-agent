from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter


DEFAULT_CHUNK_SIZE = 500
DEFAULT_CHUNK_OVERLAP = 80


def split_text(
    text: str,
    metadata: dict,
    chunk_size: int = DEFAULT_CHUNK_SIZE,
    chunk_overlap: int = DEFAULT_CHUNK_OVERLAP,
) -> list[Document]:
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        separators=["\n\n", "\n", "\u3002", "\uff1b", ";", "\uff0c", ",", " ", ""],
    )
    documents = splitter.create_documents([text], metadatas=[metadata])
    for index, document in enumerate(documents):
        document.metadata["chunk_index"] = index
        document.metadata["vector_id"] = build_vector_id(
            document.metadata["document_id"],
            index,
        )
    return documents


def build_vector_id(document_id: int, chunk_index: int) -> str:
    return f"doc-{document_id}-chunk-{chunk_index}"
