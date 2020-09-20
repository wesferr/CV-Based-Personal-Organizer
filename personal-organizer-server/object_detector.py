from dlib import correlation_tracker, rectangle
import cv2


class VideoTracker(object):

    def __init__(self, video_origin="sample-videos/livro.mp4", debug=False, resolution=(640, 480), exit_per=0.1):

        # configurando o video
        self.debug = debug
        self.resolution = resolution
        self.video = cv2.VideoCapture(video_origin)
        assert self.video.isOpened(), "0x001"

        #configurando rastreadores
        self.match_count = 0
        self.orb = cv2.ORB_create()
        self.matcher = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)
        self.tracker = correlation_tracker()
        self.exit_per = exit_per


        if self.debug:
            codec = cv2.VideoWriter_fourcc(*"mp4v")
            self.debug_video = cv2.VideoWriter('files/debug_video.mp4', codec, 10, self.resolution)

    def __del__(self):
        self.video.release()
        self.debug_video.release()

    def get_frame(self):
        ok, frame = self.video.read()
        assert ok, "0x000"
        frame = cv2.resize(frame, self.resolution)
        height, width, color_size = frame.shape
        return frame, height, width

    def crop_frame(self, frame, boundary_box):
        top = int(boundary_box.top())
        bottom = int(boundary_box.bottom())
        left = int(boundary_box.left())
        right = int(boundary_box.right())
        return frame[top:bottom, left:right]

    def draw_frame(self, frame, boundary_box):
        top = int(boundary_box.top())
        bottom = int(boundary_box.bottom())
        left = int(boundary_box.left())
        right = int(boundary_box.right())
        cv2.rectangle(frame, (left, top), (right, bottom), (255, 255, 255), 3)



    def track(self):


        # carregando quadro, redimencionando
        frame, height, width = self.get_frame()

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
                self.video.release()
                self.debug_video.release()
                assert False, "0x002"

            # saindo quando atingir a percentagem minima de descritores compativeis
            if self.debug:
                print(int(self.match_count * self.exit_per), len(matches), cv2.Laplacian(frame, cv2.CV_64F).var())
                
            if int(self.match_count * self.exit_per) >= len(matches):
                self.video.release()
                self.debug_video.release()
                assert False, "0x003"

            # atualizando estado do rastreador e boundary box
            self.tracker.update(frame)
            boundary_box = self.tracker.get_position()

            if self.debug:
                self.draw_frame(frame, boundary_box)
                self.debug_video.write(frame)


        self.video.release()
        self.debug_video.release()
        assert False, "0x004"


if __name__ == '__main__':
    VideoTracker(debug=True).track()
