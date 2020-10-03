import os
from flask import Flask
from flask import request
from audio_processor import extract_audio
from object_detector import VideoTracker
from datetime import datetime

import base64

if not os.path.exists("./files"):
    os.makedirs("./files/")

in_video_url = "files/{0}/{0}.mp4"
out_video_url = "files/{0}/{0}.debug.mp4"
audio_url = "files/{0}/{0}.wav"
output_url = "files/{0}/{0}.dat"
in_image_url = "files/{0}/{0}-in.jpg"
out_image_url = "files/{0}/{0}-out.jpg"

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
    os.makedirs("./files/{}".format(now))

    data = request.data
    write_file(data, now)

    words = extract_audio(in_video_url.format(now), audio_url.format(now), output_url.format(now))

    tracker_parameters = {
        'debug': True,
        'resolution': (1920, 1080),
        'video_origin': in_video_url.format(now),
        'video_destiny':out_video_url.format(now),
        'words': words
    }

    try:
        vt = VideoTracker(**tracker_parameters)
        vt.track(in_image_url.format(now), out_image_url.format(now))
    except Exception as e:
        if str(e) == "0x000":
            print("fim do video")
        elif str(e) == "0x001":
            print("problema com o carregamento do video")
        else:
            print(e)

    return "OK"


def write_file(data, now):
    """
    Get base64 encoded video file, decode and write it on disk
    """

    video_file = open(in_video_url.format(now), 'wb')
    video_file.write(base64.b64decode(data))
    video_file.close()


if __name__ == "__main__":
    app.run(**server_parameters)
