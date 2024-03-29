import os
import json
from vt.vocabulary_tree import VocabularyTree
from vt.vocabulary_tree import extract_descriptors
from vt.vocabulary_tree import StrDescriptor
from flask import Flask, Response
from flask import request
from flask import send_file
from audio_processor import extract_audio
from object_detector import VideoTracker
from datetime import datetime

import pymysql

import base64

if not os.path.exists("./files"):
    os.makedirs("./files/")
    os.makedirs("./files/uploaded")

in_video_url = "files/{0}/{0}.mp4"
out_video_url = "files/{0}/{0}.debug.mp4"
audio_url = "files/{0}/{0}.wav"
output_url = "files/{0}/{0}.dat"
in_image_url = "files/{0}/{0}-in.jpg"
out_image_url = "files/{0}/{0}-out.jpg"

search_image = "files/uploaded/{}.jpg"

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

    if len(imagens) > 0:
        tree.start(descriptors, len(imagens))

if not database_cursor.execute("SHOW TABLES FROM `personal_organizer` LIKE 'words';"):
    database_cursor.execute("CREATE TABLE words ( id INT NOT NULL AUTO_INCREMENT, id_image BIGINT(20), word VARCHAR(45), word_start DATETIME(6), word_end DATETIME(6), PRIMARY KEY (id), FOREIGN KEY (id_image) REFERENCES images(id) );")


@app.route("/", methods=['post'])
def main_process():
    """
    Listener that recieves the phone requests
    """
    dt_now = datetime.now()
    now = dt_now.strftime("WF%Y%m%d%H%M%S")
    os.makedirs("./files/{}".format(now))

    # data = request.data
    file = request.files["file"]
    file.save(in_video_url.format(now))
    extra_data = request.form.get("extra_data")
    extra_data = json.loads(extra_data)
    for network in extra_data.get("wireless").replace("[", "").replace("]", "").split(", "):
        network = json.loads(network.replace(';', ','))

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
        video_tracker = VideoTracker(**tracker_parameters)
        video_tracker.track(in_image_url.format(now), out_image_url.format(now))
    except Exception as e:
        video_tracker.debug_video.release()
        if e in ["0x000", "0x002"]:
            return Response(e, 400)

    identifier = now.replace("WF", "")
    path = in_image_url.format(now)
    temp_descriptors = json.dumps(tree.image_insert(identifier, path))

    result = database_cursor.execute(f"INSERT INTO images VALUES ({identifier}, '{path}', '{temp_descriptors}')")
    database_connector.commit()

    for word in words.keys():
        temp_start = (dt_now + words[word].get('start', None)).strftime("%Y-%m-%d %H:%M:%S.%f")
        temp_end = (dt_now + words[word].get('end', None)).strftime("%Y-%m-%d %H:%M:%S.%f")
        result = database_cursor.execute(f"INSERT INTO words (id_image, word, word_start, word_end) VALUES ({identifier}, '{word}', '{temp_start}', '{temp_end}');")
    database_connector.commit()

    return Response("ok", 200)


@app.route("/search", methods=['post'])
def search_process():
    dt_now = datetime.now()
    now = dt_now.strftime("WF%Y%m%d%H%M%S")
    file = request.files['file']
    file.save(search_image.format(now))
    element = (now.replace("WF", ""), search_image.format(now))
    best_score = tree.image_search([element, ])

    extra_data = request.form.get("extra_data")
    extra_data = json.loads(extra_data)
    words = extra_data.get("words", "").split(", ")
    words = "', '".join(words)

    if not best_score or best_score[0] > 1:

        database_cursor.execute(f"select id_image, count(id_image) from words where word in ('{words}') group by id_image order by -count(id_image);")
        results = database_cursor.fetchall()
        if results[0][1] > 1:
            file = out_image_url.format("WF" + str(results[0][0]))
            return send_file(file)

    else:

        database_cursor.execute(f"SELECT path from images where id={best_score[1]}")
        file = database_cursor.fetchone()
        return send_file(file[0].replace("in", "out"))


def write_file(data, path):
    """
    Get base64 encoded video file, decode and write it on disk
    """

    video_file = open(path, 'wb')
    video_file.write(base64.b64decode(data))
    video_file.close()


if __name__ == "__main__":
    app.run(**server_parameters)
