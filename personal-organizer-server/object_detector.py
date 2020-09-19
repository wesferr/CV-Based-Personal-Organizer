from dlib import correlation_tracker, rectangle
import cv2


def get_frame(video, resolution=(640, 480)):
    ok, frame = video.read()
    if not ok:
        return
    frame = cv2.resize(frame, resolution)
    height, width, color_size = frame.shape
    return frame, height, width


def track(inicio="inicio.jpeg", fim='fim.jpg', entrada="files/videos/video_capturado.mp4"):

    # criando objeto de video a partir do aquivo
    video = cv2.VideoCapture(entrada)
    print(video)

    # criando rastreador de caracteristicas e matcher
    orb = cv2.ORB_create()
    matcher = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)
    match_count = 0

    # carregando quadro, redimencionando
    try:
        frame, height, width = get_frame(video)
    except Exception as e:
        video.release()
        return

    # carregando pontos e o rastreador
    tracker = correlation_tracker()
    boundary_box = rectangle(width // 4, height // 4, 3 * (width // 4), 3 * (height // 4))
    tracker.start_track(frame, boundary_box)

    imagem_base = frame[int(boundary_box.top()):int(boundary_box.bottom()), int(boundary_box.left()):int(boundary_box.right())]
    base_keypoins, base_descriptors = orb.detectAndCompute(imagem_base, None)

    while video.isOpened():
        # carregando quadro e redimencionando
        try:
            frame, height, width = get_frame(video)
        except Exception as e:
            print("Sem quadros, finalizando.")
            break

        try:
            imagem_teste = frame[int(boundary_box.top()):int(boundary_box.bottom()), int(boundary_box.left()):int(boundary_box.right())]
            teste_keypoins, teste_descriptors = orb.detectAndCompute(imagem_teste, None)
            matches = matcher.match(base_descriptors, teste_descriptors)
        except Exception:
            print("sem descritores")
            break

        if not match_count:
            match_count = len(matches)

        print(len(matches), int((match_count * 0.75)) + 1)
        if (match_count // 10) + 1 >= len(matches):
            break
        # time.sleep(.2)

        # atualizando estado do rastreador e boundary box
        tracker.update(frame)
        boundary_box = tracker.get_position()
        pt1 = (int(boundary_box.left()), int(boundary_box.top()))
        pt2 = (int(boundary_box.right()), int(boundary_box.bottom()))

        # quadro e boundary box na tela até apertar esc
        cv2.rectangle(frame, pt1, pt2, (255, 255, 255), 3)
        # cv2.namedWindow("Image", cv2.WINDOW_NORMAL)
        # cv2.startWindowThread()
        # cv2.imshow("Image", frame)
        # k = cv2.waitKey(1) & 0xff
        # if k == 27:
        #     break

    print('saindo')
    # liberando video
    video.release()
    # cv2.destroyAllWindows()
    # cv2.waitKey(1)
    return


if __name__ == '__main__':
    track()
