import numpy as np
import cv2

cap = cv2.VideoCapture('con.mp4')

fgbg = cv2.createBackgroundSubtractorMOG2()

while(1):
    ret, frame = cap.read()
    frame = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    thresh = cv2.Laplacian(frame, cv2.CV_64F)
    print(thresh.var())
    # gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    # ret, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)

    # fgmask = fgbg.apply(frame)

    # cv2.imshow('frame', fgmask)
    cv2.imshow('frame', thresh)
    # cv2.imshow('frame', gray)
    k = cv2.waitKey(30) & 0xff
    if k == 27:
        break

cap.release()
cv2.destroyAllWindows()
