from dlib import correlation_tracker, rectangle
from datetime import timedelta
import cv2


class VideoTracker(object):

    def __init__(self, video_origin="sample-videos/cpf3.mp4", video_destiny="files/videos/debug_video.mp4", debug=False, resolution=(640, 480), exit_per=0.1, words=[]):

        # configurando o video
        self.debug = debug
        self.resolution = resolution
        self.video = cv2.VideoCapture(video_origin)
        self.video_output = video_destiny
        assert self.video.isOpened(), "0x001"

        # configurando FPS contagem de frames e tempo de reprodução
        self.video_fps = self.video.get(cv2.CAP_PROP_FPS)
        self.video_time_each_frame = timedelta(seconds=1) / self.video_fps
        self.video_process_time = timedelta(seconds=0)

        # configurando a equalização de histograma adaptativa
        self.clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))

        # configurarndo dicionario de palavras
        self.words = words
        self.matching = False

        # configurando rastreadores
        self.match_count = 0
        self.orb = cv2.ORB_create()
        self.matcher = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)
        self.tracker = correlation_tracker()
        self.exit_per = exit_per

        if self.debug:
            codec = cv2.VideoWriter_fourcc(*"mp4v")
            self.debug_video = cv2.VideoWriter(self.video_output, codec, 10, self.resolution)

    def __del__(self):
        self.video.release()
        if self.debug:
            self.debug_video.release()

    def get_frame(self):
        ok, frame = self.video.read()
        assert ok, "0x000"
        frame = cv2.resize(frame, self.resolution)

        self.video_process_time += self.video_time_each_frame

        # convertendo para Lab para equalizar a luminancia do quadro
        frame = cv2.cvtColor(frame, cv2.COLOR_RGB2Lab)
        h, l, s = cv2.split(frame)
        luminosity = self.clahe.apply(l)
        frame = cv2.merge((h, luminosity, s))
        frame = cv2.cvtColor(frame, cv2.COLOR_Lab2RGB)

        height, width, color_size = frame.shape
        return frame, height, width

    def crop_frame(self, frame, boundary_box):
        top = int(boundary_box.top()) if boundary_box.top() > 0 else 0
        bottom = int(boundary_box.bottom()) if boundary_box.bottom() > 0 else 0
        left = int(boundary_box.left()) if boundary_box.left() > 0 else 0
        right = int(boundary_box.right()) if boundary_box.right() > 0 else 0
        return frame[top:bottom, left:right]

    def draw_frame(self, frame, boundary_box):
        top = int(boundary_box.top()) if boundary_box.top() > 0 else 0
        bottom = int(boundary_box.bottom()) if boundary_box.bottom() > 0 else 0
        left = int(boundary_box.left()) if boundary_box.left() > 0 else 0
        right = int(boundary_box.right()) if boundary_box.right() > 0 else 0
        cv2.rectangle(frame, (left, top), (right, bottom), (255, 255, 255), 3)

    def track(self, in_image, out_image):

        # carregando quadro, redimencionando
        if not self.words.get('capturar', None):
            assert False, "0x002"

        while self.words['capturar']['start'] > self.video_process_time:
            frame, height, width = self.get_frame()

        while cv2.Laplacian(frame, cv2.CV_64F).var() < 20.0:
            frame, height, width = self.get_frame()

        cv2.imwrite(in_image, frame)
        # carregando a fronteira de rastreamento
        boundary_box = rectangle(width // 3, height // 3, 2 * (width // 3), 2 * (height // 3))

        # iniciando o rastreamento
        self.tracker.start_track(frame, boundary_box)

        # carregando a imagem base para rastrear e computando os descritores
        imagem_base = self.crop_frame(frame, boundary_box)
        base_keypoins, base_descriptors = self.orb.detectAndCompute(imagem_base, None)
        self.match_count = len(base_descriptors)

        while self.video.isOpened():

            # carregando quadro e redimencionando
            frame, height, width = self.get_frame()

            # carregando e calculando os descritores de cada frame para match
            imagem_teste = self.crop_frame(frame, boundary_box)
            teste_keypoins, teste_descriptors = self.orb.detectAndCompute(imagem_teste, None)
            if teste_keypoins:
                matches = self.matcher.match(base_descriptors, teste_descriptors)
            else:
                cv2.imwrite(out_image, frame)
                assert False, "0x003"

            # # saindo quando atingir a percentagem minima de descritores compativeis
            # if self.debug:
            #     print(int(self.match_count * self.exit_per), len(matches), cv2.Laplacian(frame, cv2.CV_64F).var())

            if int(self.match_count * self.exit_per) >= len(matches):
                cv2.imwrite(out_image, frame)
                assert False, "0x004"

            # atualizando estado do rastreador e boundary box
            self.tracker.update(frame)
            boundary_box = self.tracker.get_position()

            if self.debug:
                self.draw_frame(frame, boundary_box)
                self.debug_video.write(frame)

        cv2.imwrite(out_image, frame)
        assert False, "0x005"


if __name__ == '__main__':
    VideoTracker(debug=True, resolution=(1920, 1080)).track()
