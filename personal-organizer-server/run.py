import os
import json
from vt.vt import VocabularyTree
from vt.vt import extract_descriptors
from vt.vt import StrDescriptor
from flask import Flask
from flask import request
from audio_processor import extract_audio
from object_detector import VideoTracker
from datetime import datetime

import pymysql

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

database_parameters = {
    "host": "127.0.0.1",
    "user": "wesferr",
    "password": "Password123@",
    "database": "personal_organizer",
}


database_connector = pymysql.connect(**database_parameters)
database_cursor = database_connector.cursor()

tree = VocabularyTree(levels_to_use=4)

if not database_cursor.execute("SHOW TABLES FROM `personal_organizer` LIKE 'images';"):
    print("tabela de imagens não existe, criando")
    database_cursor.execute("CREATE TABLE images ( id BIGINT PRIMARY KEY, path VARCHAR(255) NOT NULL UNIQUE, descriptors JSON );")
else:
    print("tabela de imagens existete, importando")
    database_cursor.execute("SELECT id, descriptors FROM images")
    imagens = database_cursor.fetchall()

    descriptors = []
    for img_id, img_des in imagens:
        for descriptor in json.loads(img_des):
            if descriptor:
                temp_descriptor = StrDescriptor(descriptor)
                temp_descriptor.IMAGE_ID = img_id
                descriptors.append(temp_descriptor)

    tree.start(descriptors, len(imagens))


@app.route("/", methods=['post'])
def main_process():
    """
    Listener that recieves the phone requests
    """

    now = datetime.now().strftime("WF%Y%m%d%H%M%S")
    os.makedirs("./files/{}".format(now))

    data = request.data
    write_file(data, now)

    args = [
        in_video_url.format(now),
        audio_url.format(now),
        output_url.format(now)
    ]

    words = extract_audio(*args)

    tracker_parameters = {
        'debug': True,
        'resolution': (1920, 1080),
        'video_origin': in_video_url.format(now),
        'video_destiny': out_video_url.format(now),
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

    descriptors = extract_descriptors([in_image_url.format(now)])
    temp_descriptors = json.dumps(descriptors)
    path = in_image_url.format(now)
    identifier = now.replace("WF", "")

    result = database_cursor.execute(f"INSERT INTO images VALUES ({identifier}, '{path}', '{temp_descriptors}')")
    database_connector.commit()
    return("OK")


def write_file(data, now):
    """
    Get base64 encoded video file, decode and write it on disk
    """

    video_file = open(in_video_url.format(now), 'wb')
    video_file.write(base64.b64decode(data))
    video_file.close()


if __name__ == "__main__":
    app.run(**server_parameters)
