from pathlib import Path


SUPPORTED_EXTENSIONS = {".txt", ".md"}


class DocumentLoadError(ValueError):
    pass


def resolve_document_path(file_path: str) -> Path:
    raw_path = Path(file_path).expanduser()
    candidates = []

    if raw_path.is_absolute():
        candidates.append(raw_path)
    else:
        service_dir = Path(__file__).resolve().parent
        candidates.extend(
            [
                Path.cwd() / raw_path,
                service_dir / raw_path,
                service_dir.parent / raw_path,
            ]
        )

    for candidate in candidates:
        resolved = candidate.resolve()
        if resolved.is_file():
            return resolved

    raise DocumentLoadError(f"Document file not found: {file_path}")


def load_document_text(file_path: str) -> str:
    path = resolve_document_path(file_path)
    extension = path.suffix.lower()
    if extension not in SUPPORTED_EXTENSIONS:
        raise DocumentLoadError(
            f"Unsupported document type: {extension}. Only txt and md are supported."
        )

    text = path.read_text(encoding="utf-8-sig").strip()
    if not text:
        raise DocumentLoadError("Document content is empty.")
    return text
