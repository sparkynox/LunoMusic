# LunoMusic

Open source music app for Android, written from scratch. Search a song, tap it, it plays. No ads.

Streams come from the BravePipe extractor. When it breaks (YouTube changes something every other week), yt-dlp running inside the app takes over.

## Features

- Google login (YT Music) and your own personalised home feed
- Mood chips, quick picks, mixes, albums and playlists from the feed
- Full screen player with blurred artwork, seek bar, queue, shuffle and repeat
- Song search (YouTube Music results)
- Background playback with media notification and lock screen controls

Coming next: synced lyrics, library (liked songs, playlists), downloads, sleep timer, search filters, artist and album pages.

## How it works

Queue items are stored as `luno://track/<id>` and the real stream URL is only looked up when the player needs it, so a long queue doesn't hammer YouTube up front. Resolved URLs are cached for a few hours.

1. BravePipe extractor tries first
2. If that fails, yt-dlp (Chaquopy, in-process Python) tries
3. If both fail you get an error and the next song is one tap away

The home feed talks to the YouTube Music InnerTube API directly with your login cookies. The cookies stay on your phone.

## Stack

- Kotlin, Jetpack Compose, Material 3
- Media3 (ExoPlayer + MediaSession)
- BravePipe extractor, yt-dlp via Chaquopy
- OkHttp, Coil

## Building

Everything builds on GitHub Actions and the workflow produces a signed release APK (`LunoMusic-release` artifact). It needs four repository secrets:

`LUNOMUSIC_KEYSTORE_BASE64`, `LUNOMUSIC_KEYSTORE_PASSWORD`, `LUNOMUSIC_KEY_ALIAS`, `LUNOMUSIC_KEY_PASSWORD`

Locally you need JDK 17, Python 3.11 on your PATH (Chaquopy needs it at build time) and Gradle 8.7.

## Credits

- [EchoMusic](https://github.com/EchoMusicApp/Echo-Music): feature and UI inspiration, and the reference for how a YT Music login and home feed behave
- [ArchiveTune](https://github.com/rukamori/ArchiveTune): UI inspiration, mainly the player layout
- [BravePipe](https://github.com/bravepipeproject) / [NewPipe](https://github.com/TeamNewPipe) extractor
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [Chaquopy](https://chaquo.com/chaquopy/)

LunoMusic is written from scratch. No source code from EchoMusic or ArchiveTune is used, they are credited for ideas and design inspiration only.

## Contributing

Issues and PRs are welcome. Keep changes small and focused, and say what you tested on.

## License

The LunoMusic source code is MIT, see [LICENSE](LICENSE).

The BravePipe/NewPipe extractor it links against is GPL-3.0, so the compiled APK as a whole has to be distributed under GPL-3.0 terms.

Not affiliated with YouTube or Google.

By SparkyNox
