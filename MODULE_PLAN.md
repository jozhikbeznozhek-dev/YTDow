# YTDow Module Structure — planned for Maven Central access

## Current (single-module)
```
app/
  src/main/java/com/hermes/downloader/
    domain/      → domain layer
    data/        → data layer
    presentation/→ presentation layer
    core/        → utilities
    di/          → DI (future)
    ui/          → UI components (future)
```

## Target (multi-module)
```
app/              → application entry (MainActivity, Application)
core/             → Logger, ServiceLocator, extensions
common/           → shared resources, theme
domain/           → models, repository interfaces, use cases, queue
data/             → DownloadRepositoryImpl, SettingsRepositoryImpl, QueueManagerImpl
downloader/       → yt-dlp integration (plugin system base)
database/         → Room entities, DAOs (future)
network/          → UpdateRepository (future)
player/           → file opening (future)
ui/               → Compose screens (future)
settings/         → SettingsRepository (future)
```

## Dependency graph
```
app → ui, core, di
ui → presentation, core
presentation → domain, core
data → domain, core
domain → core
downloader → domain, core
database → domain, core

No circular dependencies.
```

## Plugin system (Goal 10)
Each service provider implements:
```
interface VideoServiceProvider {
    val name: String
    val metadataProvider: MetadataProvider
    val formatExtractor: FormatExtractor
    val downloader: ServiceDownloader
    val thumbnailProvider: ThumbnailProvider?
    val authenticator: ServiceAuthenticator?
}
```
