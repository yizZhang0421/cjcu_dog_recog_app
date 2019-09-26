#import os
#os.chdir('C:/Users/Administrator/Desktop/face_recognize_demo/darkflow-master')
from darkflow.net.build import TFNet
import cv2, base64
import numpy as np
options = {"model": "cfg/yolo.cfg", "load": "bin/yolov2.weights", "threshold": 0.1}
tfnet = TFNet(options)
from flask import Flask, request
from keras.models import load_model
from tensorflow import Graph, Session
app = Flask(__name__)

graph=Graph()
with graph.as_default():
    session=Session(graph=graph)
    with session.as_default():
        model=load_model('CNNmodel/64weights.h5')

@app.route('/',methods=['POST'])
def login():
    global graph
    global session
    global model
    img=bytes(request.data)
    img=base64.b64decode(img)
    img=np.frombuffer(img, dtype=np.uint8)
    img=cv2.imdecode(img,cv2.IMREAD_COLOR)
    
    result = tfnet.return_predict(img)
    return_string=''
    for obj in result:
        print(obj['label'])
        if(obj['label']=='dog'):
            tl = (obj['topleft']['x'],obj['topleft']['y'])
            br = (obj['bottomright']['x'],obj['bottomright']['y'])
            img_dog = img[tl[1]:br[1] , tl[0]:br[0]]        
            #cv2.imwrite('test.png',img_dog)
            img_dog=cv2.resize(img_dog,(64,64),interpolation=cv2.INTER_CUBIC)
            img_dog=img_dog/255
            img_dog=np.array([img_dog])
            with graph.as_default():
                with session.as_default():
                    label=model.predict_classes(img_dog)[0]
            if label==0:
                label="Julian"
            elif label==1:
                label="OldMi"
            elif label==2:
                label="Tiger"
            return_string+=label+' '+str(obj['topleft']['x'])+' '+str(obj['topleft']['y'])+' '+str(obj['bottomright']['x'])+' '+str(obj['bottomright']['y'])+'\n'
    if(return_string==''):
        return 'nodog'
    else:
        return return_string


if __name__ == '__main__':
    app.run(host='0.0.0.0',port=9487)