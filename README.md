# LunoMusic

Open source music app for Android. Search a song, tap it, it plays. No ads.

Streams come from the BravePipe extractor. When it breaks (YouTube changes something every other week), yt-dlp running inside the app takes over.

## Features

- Google login (YT Music) and your own personalised home feed with mood chips, loads more as you scroll
- Profile: your YT Music photo and name, a bio, or a photo from your gallery
- In app updates: finds the newest release on GitHub, downloads and installs it without opening the browser
- Full screen player: blurred artwork, seek bar, synced lyrics, queue, shuffle, repeat, sleep timer, like button
- Queue: swipe a song away or hit the cross to remove it
- Endless queue: when the queue runs low it keeps going with the YouTube Music radio of the current song
- Search with live suggestions and filters (songs, albums, playlists, artists, videos)
- Artist and album / playlist pages
- Library: liked songs, history, your playlists, downloads, your top 50
- Downloads for offline playback
- Stats: time listened, top artists, top songs, listening by day
- Settings and detailed in-app logs (Settings > Logs), copy or share the whole log when something breaks
- Auto recovery when a stream URL dies mid-song (re-resolves, other extractor first)

Coming next: equalizer, crossfade, playlists you can edit.

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

- [EchoMusic](https://github.com/EchoMusicApp/Echo-Music): feature and UI inspiration, reference for the YT Music login and home feed
- [ArchiveTune](https://github.com/rukamori/ArchiveTune) by Rukamori: theme, Poppins typography, vector icons, wavy slider and animated playback controls are taken from it, the rest of the UI follows its design
- [BravePipe](https://github.com/bravepipeproject) / [NewPipe](https://github.com/TeamNewPipe) extractor
- [LRCLIB](https://lrclib.net): synced lyrics
- [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- [Chaquopy](https://chaquo.com/chaquopy/)

## Releasing

Every push to `main` builds a signed APK and publishes it as a GitHub release named after `versionName` in `app/build.gradle.kts`. The app checks the latest release for updates, so bump `versionCode` and `versionName` before pushing a new version. Edit the release notes on GitHub, they show up in the in app update dialog.

## Contributing

Issues and PRs are welcome. Keep changes small and focused, and say what you tested on. When reporting a bug, attach the log from Settings > Logs.

## License

GPL-3.0, see [LICENSE](LICENSE). The extractor LunoMusic links against is GPL-3.0 too, and the UI draws on EchoMusic and ArchiveTune which are GPL-3.0.

Not affiliated with YouTube or Google.

By SparkyNox
