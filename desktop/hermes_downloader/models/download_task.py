from pydantic import BaseModel, Field
from enum import Enum
import uuid


class TaskStatus(str, Enum):
    QUEUED = "queued"
    PARSING = "parsing"
    DOWNLOADING = "downloading"
    POST_PROCESSING = "post_processing"
    COMPLETED = "completed"
    CANCELLED = "cancelled"
    ERROR = "error"


class DownloadTask(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4())[:8])
    url: str
    title: str = "Ожидание анализа..."
    quality: str = "best"  # best, 1080p, 720p, audio_only
    format: str = "mp4"    # mp4, mp3
    status: TaskStatus = TaskStatus.QUEUED
    progress: float = 0.0  # 0.0 to 1.0
    file_path: str | None = None
    speed: str | None = None
    eta: str | None = None
    filesize: str | None = None
