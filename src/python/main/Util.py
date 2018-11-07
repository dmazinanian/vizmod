class ndarray: pass # type stub

import matplotlib.pyplot as plt
import os
plt.ion()

rootdir = '~/vizmod'
abstraction_js_path = os.path.dirname(os.path.realpath(__file__)) + "/Abstraction.js"
OUTPUT_DIR = ""

def loadTestSubject(subjID, browser, tab=0):
    browser.goto("file://" + os.path.expanduser(rootdir + '/test-subjects/subject-%.2d/index.html' % subjID), tab)

def setOutputDir(dirpath):
    global OUTPUT_DIR
    OUTPUT_DIR = os.path.abspath(os.path.expanduser(dirpath))

def getOutputDir():
    global OUTPUT_DIR
    return OUTPUT_DIR

def saveToOutputDir(data, filename, subdir=None):
    fulldir = getOutputDir() + '/' + subdir if subdir else getOutputDir()
    if not os.path.isdir(fulldir): os.makedirs(fulldir)
    with open(fulldir+"/"+filename, "w") as fp:
        fp.write(data)

def saveImagesToOutputDir(imlist, subdir=None):
    fulldir = getOutputDir() + '/' + subdir if subdir else getOutputDir()
    if not os.path.isdir(fulldir): os.makedirs(fulldir)
    for i,im in enumerate(imlist): plt.imsave(fname=fulldir+"/snapshot_%.3d.png"%i, arr=im)

def saveImageToOutputDir(im, filename=None):
    outDir = getOutputDir()
    filename = outDir+"/"+filename
    if not os.path.isdir(outDir): os.makedirs(outDir)
    plt.imsave(fname=filename, arr=im)
    print("Saved to file: %s" % filename)

def imshow(*par, **kw):
    plt.imshow(*par, **kw)
    plt.show()

def imshows(ims):
    for im in ims:
        plt.figure()
        imshow(im)
