import os
from datetime import timedelta

import moviepy.editor as video_editor
from pydub import AudioSegment


def speech_to_text_watson(audio_url, output_url):
    from ibm_watson import SpeechToTextV1
    from ibm_cloud_sdk_core.authenticators import IAMAuthenticator
    from ibm_watson.websocket import RecognizeCallback, AudioSource
    log = open(output_url, "a")

    words_dict = {}

    authenticator = IAMAuthenticator()
    speech_to_text = SpeechToTextV1(authenticator=authenticator)
    speech_to_text.set_service_url()

    file = open(audio_url, "rb")
    speech_result = speech_to_text.recognize(file, content_type="audio/wav", model="pt-BR_NarrowbandModel", timestamps=True, speech_detector_sensitivity=0.9, end_of_phrase_silence_time=120.0)
    speech_result = speech_result.get_result()
    if "results" in speech_result and speech_result["results"]:
        result = speech_result["results"][0]
        if "alternatives" in result and result["alternatives"]:
            alternative = result["alternatives"][0]
            if "transcript" in alternative:
                transcript = alternative["transcript"]
                log.write("TRANSCRIÇÃO:\n")
                log.write(u"{}\n".format(transcript))
            for word in alternative["timestamps"]:
                log.write(u"word:{} start:{} end:{}\n".format(word[0], word[1], word[2]))
                words_dict[word[0]] = {"start": timedelta(seconds=word[1]), "end": timedelta(seconds=word[2])}
            return words_dict


def speech_to_text_gcloud(audio_url, output_url):
    from google.cloud import speech_v1 as speech_v1
    client = speech_v1.SpeechClient()
    log = open(output_url, "a")

    config = {
        # "language_code": "pt-BR",
        "language_code": "en-US",
        "sample_rate_hertz": 44100,
        "enable_word_time_offsets": True,
    }
    with open(audio_url, "rb") as f:
        content = f.read()

    audio = {"content": content}

    words_dict = {}

    speech_result = client.recognize(config, audio)
    print(speech_result)
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
                    start = word.start_time.seconds + (word.start_time.nanos * 1e-9)
                    end = word.end_time.seconds + (word.end_time.nanos * 1e-9)
                    words_dict[word.word] = {"start": timedelta(seconds=start), "end": timedelta(seconds=end)}
                    log.write(u"word:{} start:{} end:{}\n".format(word.word, start, end))

            return words_dict


def extract_audio(video_url, audio_url, output_url):
    video = video_editor.VideoFileClip(video_url)
    video.audio.write_audiofile(audio_url)
    audio = AudioSegment.from_wav(audio_url)
    audio = audio.set_channels(1)
    audio.export(audio_url, format="wav")
    words = speech_to_text_watson(audio_url, output_url)
    os.remove(audio_url)
    return words


if __name__ == "__main__":
    print(speech_to_text_watson("./files/audios/WF20200927204253.wav", "./"))
