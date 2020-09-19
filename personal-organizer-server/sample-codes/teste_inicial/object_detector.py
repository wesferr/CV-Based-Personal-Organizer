import cv2

minor_ver = 4
threshold = 100

tracker_types = [
    cv2.TrackerBoosting_create(),
    cv2.TrackerMIL_create(),
    cv2.TrackerKCF_create(),
    cv2.TrackerTLD_create(),
    cv2.TrackerMedianFlow_create(),
    cv2.TrackerGOTURN_create(),
    cv2.TrackerMOSSE_create(),
    cv2.TrackerCSRT_create()
]


def get_frame(video, resolution=(640, 480)):
    ok, frame = video.read()
    if not ok:
        return
    frame = cv2.resize(frame, resolution)
    height, width, color_size = frame.shape
    return frame, height, width


def track(inicio="inicio.jpeg", fim='fim.jpg', entrada="files/videos/video_capturado.mp4", type=2):

    print("Rodando o tracker")

    # define o tracker
    tracker = tracker_types[type]

    # carrega o video
    video = cv2.VideoCapture(entrada)

    # Exit if video not opened.
    if not video.isOpened():
        return

    try:
        frame, width, height = get_frame(video)
    except Exception as e:
        print("sem quadros")
        return

    # define a caixa de rastreio
    bbox = (width / 4, height / 4, 2 * (width / 4), 2 * (height / 4))
    ok = tracker.init(frame, bbox)

    while True:

        # salva frame anterior e pega um novo
        frame0 = frame

        try:
            frame, width, height = get_frame(video)
        except Exception as e:
            print("sem quadros")
            break

        # Start timer
        timer = cv2.getTickCount()

        # Update tracker
        ok, bbox = tracker.update(frame)
        bbox = tuple(map(int, bbox))

        # Calculate Frames per second (FPS)
        fps = cv2.getTickFrequency() / (cv2.getTickCount() - timer)

        if ok:  # Sucesso: desenha o quadrado
            p1 = (bbox[1], bbox[0])
            p2 = (bbox[1] + bbox[3], bbox[0] + bbox[2])
            cv2.rectangle(frame, p1, p2, (255, 0, 0), 2, 1)
        else:  # Falha: salva a imagem e sai
            print("objeto sumiu")
            cv2.imwrite(fim, frame)
            break

        # Mostra o FPS e a imagem
        cv2.putText(frame, "FPS: " + str(int(fps)), (100, 50), cv2.FONT_HERSHEY_SIMPLEX, 0.75, (50, 170, 50), 2)
        cv2.imshow("Tracking", frame)

        # Exit if ESC pressed
        k = cv2.waitKey(1) & 0xff
        if k == 27:
            break

    cv2.destroyAllWindows()
    return


if __name__ == '__main__':
    track()
