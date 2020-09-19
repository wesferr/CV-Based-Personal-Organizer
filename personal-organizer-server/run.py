from flask import Flask
from flask import request
from audio_processor import extract_audio
from object_detector import track
from datetime import datetime

import base64

video_url = "files/videos/{}.mp4"
audio_url = "files/audios/{}.wav"
output_url = "files/outputs/{}.dat"
in_image_url = "files/images/{}-in.jpg"
out_image_url = "files/images/{}-out.jpg"
server_parameters = {
    "host": "0",
    "port": 5000,
    "debug": True,
    "threaded": True
}

app = Flask(__name__)


@app.route("/", methods=['post'])
def main_process():
    """
    Listener that recieves the phone requests
    """

    now = datetime.now().strftime("WF%Y%m%d%H%M%S")

    data = request.data
    write_file(data, now)

    extract_audio(video_url.format(now), audio_url.format(now), output_url.format(now))
    track(in_image_url.format(now), out_image_url.format(now), video_url.format(now))

    return "OK"


def write_file(data, now):
    """
    Get base64 encoded video file, decode and write it on disk
    """

    video_file = open(video_url.format(now), 'wb')
    video_file.write(base64.b64decode(data))
    video_file.close()


if __name__ == "__main__":
    app.run(**server_parameters)
