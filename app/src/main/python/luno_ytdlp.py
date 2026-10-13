import json
import yt_dlp

_opts = {
    "quiet": True,
    "no_warnings": True,
    "noplaylist": True,
    "skip_download": True,
    "format": "bestaudio[ext=m4a]/bestaudio/best",
    "socket_timeout": 6,
    "extractor_retries": 1,
    "extractor_args": {
        "youtube": {
            "player_client": ["android_vr", "android"],
            "skip": ["hls", "dash", "translated_subs"],
        }
    },
}

# one instance reused, startup is the slow part
_ydl = yt_dlp.YoutubeDL(_opts)


def resolve(video_id):
    info = _ydl.extract_info("https://www.youtube.com/watch?v=" + video_id, download=False)
    return json.dumps({"url": info.get("url"), "abr": info.get("abr"), "ext": info.get("ext")})
