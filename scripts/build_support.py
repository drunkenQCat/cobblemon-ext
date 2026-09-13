"""Shared packaging and archive helpers; dependency resolution belongs to Gradle."""
import hashlib
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def digest(path: Path, algorithm: str = "sha256") -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, algorithm).hexdigest()


def extract_safe(archive: zipfile.ZipFile, destination: Path) -> None:
    destination = destination.resolve()
    for entry in archive.infolist():
        target = (destination / entry.filename).resolve()
        if not target.is_relative_to(destination):
            raise ValueError(f'Unsafe archive path: {entry.filename}')
        if entry.is_dir():
            target.mkdir(parents=True, exist_ok=True)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(entry) as source, target.open('wb') as out:
                shutil.copyfileobj(source, out)


