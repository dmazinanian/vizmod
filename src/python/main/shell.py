from Driver import ChromeInstance
from Util import imshow, loadTestSubject, setOutputDir, getOutputDir, saveImageToOutputDir, saveToOutputDir
from ROI import getROIImage, getROIs, groupROIClusters, getROIClusters, printGroups, finalMatching, getFullROIImage

from XPath import *
import numpy as np
from skimage.measure import label, regionprops

import json
import time
import os
from typing import List, Set, Dict, NamedTuple, Tuple, Union
import pychrome
from Util import ndarray, abstraction_js_path
import numpy as np
from XPath import getXPaths, clusterXPaths
from sklearn import cluster
from itertools import groupby
from skimage.measure import label, regionprops
from scipy.spatial.distance import pdist, squareform

def loadAllCs():
    global browser
    c = []
    for i in range(5):
        subjId = i + 1
        loadTestSubject(subjId, browser)
        time.sleep(3)
        c.append(getROIClusters(browser))
        setOutputDir('~/vizmod-dev/test-output/subject_%d'%subjId)
        saveImageToOutputDir(getFullROIImage(browser), "fullroi.png")
    return c

# testGrouping(c, factor=-1, normalize=True, prefFunc='lambda mat: (np.min(mat)+np.median(mat))/2')
# testGrouping(c, factor=-1, normalize=True, prefFunc='lambda mat: np.median(mat)')
def testGrouping(c, factor, normalize, prefFunc:str):
    output = "--------------------------------------------------------------------------------------------------\n"
    output += " factor = %.2f    normalize = %s    prefFunc = %s\n" % (factor, str(normalize), prefFunc)
    scope = {}
    exec('lambdafunc = %s' % prefFunc, globals(), scope)
    for subjId, clusters in enumerate(c):
        groups = groupROIClusters(clusters, factor=factor, normalize=normalize, prefFunc=scope['lambdafunc'])
        numgroups = len(groups)
        output += " Subject %d\n Number of groups: %d\n\n" % (subjId+1, numgroups)
        for i in range(numgroups):
            # areas = ["%d_%d"%(cluster.parent.bbox['width'],cluster.parent.bbox['height']) for cluster in [clusters[i] for i in groups[i]]]
            areas = ["%d_%d"%(cluster.parent.bbox['width'],cluster.parent.bbox['height']) for cluster in groups[i]]        
            output += "    Group %d areas:  %s\n" % (i, str(areas))
        output += "--------------------------------------------------------------------------------------------------\n"
    setOutputDir('~/vizmod-dev/test-output/grouping-tests')
    filename = "%d.txt"%hash(output)
    saveToOutputDir(output, filename)
    print("Saved result to %s" % filename)

def testOnSubj(subjID):
    global browser
    loadTestSubject(subjID, browser)
    time.sleep(3)
    setOutputDir('~/vizmod-dev/test-output/subject_%d'%subjID)
    c = getROIClusters(browser)
    groups = printGroups(c)
    saveImageToOutputDir(getFullROIImage(browser), "fullroi.png")


browser = ChromeInstance(headless=False)

# c = loadAllCs()
# testGrouping(c, factor=-1, normalize=True, prefFunc='lambda mat: np.median(mat)')

subjID = 1
loadTestSubject(subjID, browser) #type:ignore
setOutputDir('~/vizmod-dev/test-output/subject_%d'%subjID)
time.sleep(3)  # to give enough time for DOM to load
origss = browser.getScreenshot()
c = getROIClusters(browser)
# g = groupROIClusters(c, factor=-1, normalize=True, prefFunc=lambda mat: np.median(mat))
g = printGroups(c)
fullim = getROIImage(browser)
# templates = finalMatching(g, fullim, origss)


# # # Code for fixing headless screenshotting in new tabs
# browser = ChromeInstance(headless=True)
# browser.goto('https://www.google.com')
# ntab = browser.newTab()
# browser.goto('https://www.bing.com', ntab)
# loadTestSubject(1, browser)



def quit():
    browser.close()
    exit()

from ptpython.repl import embed
embed(globals(), locals())

