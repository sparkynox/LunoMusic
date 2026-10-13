# LunoMusic

Open source music app for Android, written from scratch. Search a song, tap it, it plays. No ads, no accounts.

Streams come from the BravePipe extractor. If it breaks (YouTube changes something every other week), yt-dlp running inside the app takes over.

## Features

- Song search (YouTube Music results)
- Background playback with media notification and lock screen controls
- Queue built from your search results
- Dark amber UI

Coming next: downloads, library/playlists, settings, lyrics.

## How it works

Playlist items are stored as `luno://track/<id>` and the real stream URL is only looked up when the player actually needs it, so a long queue doesn't hammer YouTube up front. Resolved URLs are cached for a few hours.

1. BravePipe extractor tries first
2. If that fails, yt-dlp (Chaquopy, in-process Python) tries
3. If both fail, you get an error and the next song is one tap away

The order is one constant in `Extractor.kt` (`YTDLP_FIRST`) if you'd rather try yt-dlp first.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Media3 (ExoPlayer + MediaSession)
- BravePipe extractor
- yt-dlp via Chaquopy
- OkHttp, Coil

## Building

Everything builds on GitHub Actions. Push to `main` and download the APK from the run's artifacts (`LunoMusic-debug`).

Locally you need JDK 17, Python 3.11 on your PATH (Chaquopy needs it at build time) and Gradle 8.7:

```
gradle :app:assembleDebug
```

## Contributing

Issues and PRs are welcome. Keep changes small and focused, and say what you tested on.

## Credits

[BravePipe](https://github.com/bravepipeproject), [NewPipe](https://github.com/TeamNewPipe), [yt-dlp](https://github.com/yt-dlp/yt-dlp), [Chaquopy](https://chaquo.com/chaquopy/)

## License

The LunoMusic source code is MIT, see [LICENSE](LICENSE).

The BravePipe/NewPipe extractor it links against is GPL-3.0, so the compiled APK as a whole has to be distributed under GPL-3.0 terms.

Not affiliated with YouTube or Google. Use it for content you're allowed to listen to.

By SparkyNox
