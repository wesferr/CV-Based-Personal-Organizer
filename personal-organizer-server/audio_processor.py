import os

import moviepy.editor as video_editor
from google.cloud import speech_v1 as speech_v1
from pydub import AudioSegment

if __name__ == "__main":
    """
    Do something here
    """
    pass


def speech_to_text(audio_url, output_url):
    client = speech_v1.SpeechClient()
    log = open(output_url, "a")

    config = {
        "language_code": "pt-BR",
        "enable_word_time_offsets": True,
    }
    with open(audio_url, "rb") as f:
        content = f.read()

    audio = {"content": content}

    speech_result = client.recognize(config, audio)
    if speech_result.results:
        result = speech_result.results[0]
        if result.alternatives:
            alternative = result.alternatives[0]
            if alternative.transcript:
                transcript = alternative.transcript
                log.write("TRANSCRIÇÃO:\n")
                log.write(u"{}\n".format(transcript))
            if alternative.words:

                log.write(u"PALAVRAS:\n")
                for word in alternative.words:
                    start = word.start_time.seconds + (word.start_time.nanos / 1000000000.0)
                    end = word.end_time.seconds + (word.end_time.nanos / 1000000000.0)
                    log.write(u"word:{} start:{} end:{}\n".format(word.word, start, end))


def extract_audio(video_url, audio_url, output_url):
    video = video_editor.VideoFileClip(video_url)
    video.audio.write_audiofile(audio_url)
    audio = AudioSegment.from_wav(audio_url)
    audio = audio.set_channels(1)
    audio.export(audio_url, format="wav")
    speech_to_text(audio_url, output_url)
    os.remove(audio_url)
