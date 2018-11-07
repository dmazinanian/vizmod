import json
import os
import time
from Driver import BrowserInstance, TabInstance
from typing import List, Set, Dict, NamedTuple, Tuple, Union
import pychrome
from Util import imshows, ndarray, abstraction_js_path, saveToOutputDir, saveImagesToOutputDir
import numpy as np
from XPath import getXPaths, clusterXPaths, overlayXPaths
from sklearn import cluster
from itertools import groupby, combinations, compress
from scipy.spatial.distance import pdist, squareform
from scipy.spatial.distance import cosine as cosdist
from collections import Counter

ROI = NamedTuple("ROI", xpath=str, bbox=Dict[str,int], type=str)
ROICluster = NamedTuple("ROICluster", parent=ROI, children=List[ROI])
Template = NamedTuple("Template", instancesXpaths=List[str])
np.set_printoptions(formatter={'float': '{: 0.3f}'.format})

def roundDims(bbox):
    return {k: int(round(v)) for k,v in bbox.items()}

def getROIs(browser:BrowserInstance, #type: ignore
            tab:TabInstance=0) -> List[ROI]:
    with open(abstraction_js_path, "r") as fp:
        abstraction_js = fp.read()
    browser.run(abstraction_js, tab)
    abstract = browser.eval("getAbstraction();", tab)
    return [ROI(x, roundDims(y), t) #pylint:disable=E1102
            for x, y, t in zip(abstract['xpaths'], abstract['bboxes'], abstract['types'])] 

def getROIClusters(browser:BrowserInstance, #type: ignore
            tab:TabInstance=0) -> List[ROICluster]:
    xpaths = getXPaths(browser, tab)
    xpaths = clusterXPaths(xpaths)
    with open(abstraction_js_path, "r") as fp:
        abstraction_js = fp.read()
    browser.run(abstraction_js, tab)
    abstract = browser.eval("getAbstraction('%s');"%json.dumps(xpaths), tab)
    # parents = [ROI(xpath=x, bbox=roundDims(y)) for x,y in zip(abstract['xpaths'], abstract['bboxes'])] # pylint: disable=E1102
    parents = [ROI(x, roundDims(y), t) #pylint:disable=E1102
               for x, y, t in zip(abstract['xpaths'], abstract['bboxes'], abstract['types'])]
    
    children = getROIs(browser, tab)
    return [ROICluster(parent, children=[child for parent, child in list(group)]) # pylint: disable=E1102
            for parent, group in groupby(zip(parents, children), key = lambda pair: pair[0])]
    # clusters = [ROICluster(parent=parent, children=takewhile(lambda ic: ic==ip, range(len(children)))) for ip, parent in enumerate(parents)] # pylint: disable=E1102
    # groupby(zip(parents, children), key = lambda pair: pair[0])

    # for key, group in groupby(zip(parents, children), key = lambda pair: pair[0]):
    #     print("key = %s" % str(key))
    #     print("group = %s" % str([child for parent, child in list(group)]))

    # mapping = {parent.xpath: [indecies] for }
    # for i, c in enumerate(children):
    #     parent = xpaths[i]

# def groupROIClusters(clusters:List[ROICluster], factor=-100, normalize=True, prefFunc=None) -> List[List[ROICluster]]:
#  factor = -1.00    normalize = True    prefFunc = lambda mat: (np.min(mat)+np.median(mat))/2
# def groupROIClusters(clusters:List[ROICluster], factor, normalize, prefFunc) -> List[List[ROICluster]]:
def groupROIClusters(clusters:List[ROICluster], factor=-1, normalize=True, prefFunc=lambda mat: (np.min(mat)+np.median(mat))/2) -> List[List[ROICluster]]:
    def dist(A, B):
        widthA  = A[0];  widthB = B[0]
        heightA = A[1]; heightB = B[1]
        # return (abs(widthA-widthB)+abs(heightA-heightB))/2
        return (abs(widthA-widthB)+abs(heightA-heightB))/1

        # return abs(A.parent.bbox['area'] - B.parent.bbox['area'])    
    # afmat = pdist(np.matrix([cluster.parent.bbox['area'] for cluster in clusters]).transpose(), lambda x,y:2*abs(x-y)/(x+y))
    # afmat = pdist(np.matrix([cluster.parent.bbox['area'] for cluster in clusters]).transpose(), lambda x,y:abs(x-y))
    afmat = pdist(np.matrix([
                            [cluster.parent.bbox['width'],
                            cluster.parent.bbox['height']]
                            for cluster in clusters
                            ]), dist)

    # afmat = (-100)*squareform(afmat/np.max(afmat))
    # afmat = (-100)*squareform(afmat)
    if normalize == True:
        afmat = (factor)*squareform(afmat/np.max(afmat))
    else:
        afmat = (factor)*squareform(afmat)
    
    if prefFunc:
        pref = prefFunc(afmat)
    else:
        pref = np.min(np.min(np.min(afmat)))
    ap = cluster.AffinityPropagation(affinity='precomputed', preference=pref)
    ap.fit(afmat)
    # allpoints_labels, centers_indices = (ap.labels_, ap.cluster_centers_indices_)
    groups = set()
    for label in ap.labels_:
        groups.add(frozenset([i for i,x in enumerate(ap.labels_) if x==label]))
    groups = [list(group) for group in groups]
    return [[clusters[i] for i in indexGroup] for indexGroup in groups]

# def printGroups(clusters, factor=-100, normalize=True, prefFunc=None):
def printGroups(clusters, factor=-1, normalize=True, prefFunc=lambda mat: (np.min(mat)+np.median(mat))/2):
    groups = groupROIClusters(clusters, factor, normalize, prefFunc)
    print("----------------------------------")
    print(" factor = %.2f    normalize = %s" % (factor, str(normalize)))
    numgroups = len(groups)
    print(" Number of groups: %d\n" % numgroups)
    for i in range(numgroups):
        # areas = ["%d_%d"%(cluster.parent.bbox['width'],cluster.parent.bbox['height']) for cluster in [clusters[i] for i in groups[i]]]
        areas = ["%d_%d"%(cluster.parent.bbox['width'],cluster.parent.bbox['height']) for cluster in groups[i]]        
        print("    Group %d areas:  %s" % (i, str(areas)))
    print("----------------------------------")
    return groups

def getFullROIImage(browser:BrowserInstance, #type: ignore
                tab:TabInstance=0) -> ndarray:
    r = getROIImage(browser)
    c = getClustersImage(browser)
    r, c = pad(r, c)
    return r+c

def getROIImage(browser:BrowserInstance, #type: ignore
                tab:TabInstance=0) -> ndarray:
    with open(abstraction_js_path, "r") as fp:
        abstraction_js = fp.read()
    browser.run(abstraction_js, tab)
    abstraction = browser.eval("getAbstraction();", tab)
    ROIs_html = abstraction['html']
    tempfile = os.path.dirname(os.path.realpath(__file__)) + "/temp.html"
    with open(tempfile, "w") as fp:
        fp.write(ROIs_html)
    newTab = browser.newTab()
    try:
        browser.goto("file://"+tempfile, newTab)
        image = browser.getScreenshot(newTab)
        os.remove(tempfile)
        browser.closeTab(newTab)
        return image
    except:
        os.remove(tempfile)
        raise(Exception("getROIImage exception"))

def getClustersImage(browser:BrowserInstance, #type: ignore
                tab:TabInstance=0) -> ndarray:
    x = getXPaths(browser, tab)
    c = clusterXPaths(x)
    return overlayXPaths(c, browser, tab)

def getClusterROIImage(cluster:ROICluster, fullimroi:ndarray) -> ndarray:
    box = cluster.parent.bbox
    t, b = box['top'], box['bottom']
    l, r = box['left'], box['right']
    return fullimroi[t:b,l:r,:]

def finalMatching(groups:List[List[ROICluster]], fullimroi:ndarray, actualscrnshot:ndarray, factor=-1):
    templates:List[List[str]] = [] # list of xpaths of instances
    jsonStruct = {'refactoring_repetitions': []}
    xpathsAccum = set()
    for i, clusterGroup in enumerate(groups):
        if len(clusterGroup) < 2:
            # print("skipping clusterGroup. len < 2")
            continue
        # print('i = %d'%i)
        shouldDraw = input("Should plot imrois/ss? Enter = yes. 'x' = no.")
        imrois = [getClusterROIImage(cluster, fullimroi) for cluster in clusterGroup]
        imss = [getClusterROIImage(cluster, actualscrnshot) for cluster in clusterGroup]
        imshows(imrois)
        imshows(imss)
        print('len(imrois) = %d' % len(imrois))
        input("Plotted imrois for clusterGroup[%i]. Press Enter to continue ..."%i)
        # t0 = time.time()
        afmat = [calcDiffScore(imrois[a],imrois[b]) for a,b in combinations(range(len(imrois)), r=2)]
        # tf = time.time()
        # print("Total duration for all pair-wise calcDiffScore = %.3f MILLIseconds" % ((tf-t0)*1000))
        afmat = (factor)*squareform(np.asarray(afmat)/np.max(afmat))
        # print('afmat.shape = %d, %d' % (afmat.shape[0],afmat.shape[1]))
        print(afmat)
        input("Printed afmat. Press Enter to continue...")
        prefFunc = lambda mat: np.median(mat)
        ap = cluster.AffinityPropagation(affinity='precomputed', preference=prefFunc(afmat))
        ap.fit(afmat)
        print(" ap.labels_ = ")
        print(ap.labels_)
        input("Press Enter to continue...")
        if len(clusterGroup) == 2 and ap.labels_[0] != ap.labels_[1]:
            # print("skipping. len == 2, non-matching")
            continue
        # winningLabel = np.bincount(ap.labels_).argmax()
        # print("winningLabel = %d" % winningLabel)
        # input("Press Enter to continue...")



        winningLabels = [k for k, v in Counter(ap.labels_).items() if v>1]
        for winningLabel in winningLabels:
            idxes = [label==winningLabel for label in ap.labels_]
            if len(idxes) < 2:
                # print("skipping. template has only one element.")
                continue
            xpaths = [cluster.parent.xpath for cluster in compress(clusterGroup, idxes)]
            xpathsFiltered = []
            idxesFiltered = []
            # TODO: Finish the filtration below to remove duplicates.
            for i,x in enumerate(xpaths):
                if x not in xpathsAccum:
                    xpathsFiltered.append(x)
                    xpathsAccum.add(x)
                    idxesFiltered.append(idxes[i])
            xpaths = xpathsFiltered
            idxes = idxesFiltered                    
            templates.append(Template(instancesXpaths=xpaths))
            jsonStruct['refactoring_repetitions'].append(xpaths)

            snapshots = [getClusterROIImage(cluster, actualscrnshot) for cluster in compress(clusterGroup, idxes)]
            # snapshots = [imroi for imroi in compress(imrois, idxes)]
            saveImagesToOutputDir(snapshots, subdir="template_%.3d"%(len(templates)-1))
            # saveToOutputDir(data="\n".join(xpaths), filename="xpaths.txt", subdir="template_%.3d"%len(templates))
        # print("Finished processing group[%d]"%i)

    saveToOutputDir(data=json.dumps(jsonStruct, indent=4, sort_keys=True), filename="refactoring_result.json")
    return templates

def patchify(im, blocks:[int,int]=[3,3]) -> List[ndarray]:
    h, w = (im.shape[0], im.shape[1])
    nh, nw = blocks
    h = h-1 if h%nh else h
    w = w-1 if w%nw else w
    bh = int(h/nh); bw = int(w/nw)
    hs = [i*bh for i in range(nh)]
    he = [(i+1)*bh for i in range(nh)]
    ws = [i*bw for i in range(nw)]
    we = [(i+1)*bw for i in range(nw)]
    if im.ndim == 2:
        imlist = [im[hs[ih]:he[ih],ws[iw]:we[iw]] for ih in range(nh) for iw in range(nw)]
    else:
        imlist = [im[hs[ih]:he[ih],ws[iw]:we[iw],:] for ih in range(nh) for iw in range(nw)]
    return imlist

def calcHist(patch:ndarray) -> [float,float]:
    norm = patch.shape[0]*patch.shape[1]
    hist = [np.count_nonzero(patch[:,:,c]>0.5)/norm for c in [1,2]]
    return hist

def calcSignature(im:ndarray) -> List[float]:
    patching = patchify(im)
    signature = [bin for p in patching for bin in calcHist(p)]
    return signature

def calcDiffScore(im1:ndarray, im2:ndarray) -> float:
    t0 = time.time()
    h1, w1 = im1.shape[0], im1.shape[1]
    h2, w2 = im2.shape[0], im2.shape[1]
    s1 = calcSignature(im1)
    s2 = calcSignature(im2)
    # score = cosdist(s1, s2) + abs(h1-h2)*1.0 + abs(w1-w2)*1.0
    score = cosdist(s1, s2)
    # dist = 2*np.arccos((-1)*(cosdist(s1, s2)-1))/np.pi  # angular cosine **metric**
    tf = time.time()
    # print("duration: %.3f miliseconds" % ((tf-t0)*1000))
    return score

def pad(im1, im2, extendOnly=True):
    h1, w1 = im1.shape[0], im1.shape[1]
    h2, w2 = im2.shape[0], im2.shape[1]
    diffh = h2 - h1; diffw = w2 - w1
    if diffh > 0:
        if diffh%2:
            if extendOnly:
                im1 = np.pad(im1, ((0,diffh), (0,0), (0,0)), 'constant')
            else:
                im1 = np.pad(im1, ((int(diffh/2),int(diffh/2)+1), (0,0), (0,0)), 'constant')
        else:
            if extendOnly:
                im1 = np.pad(im1, ((0,diffh), (0,0), (0,0)), 'constant')
            else:
                im1 = np.pad(im1, ((diffh/2,diffh/2), (0,0), (0,0)), 'constant')
    if diffh < 0:
        if diffh%2:
            if extendOnly:
                im2 = np.pad(im2, ((0,-diffh), (0,0), (0,0)), 'constant')
            else:
                im2 = np.pad(im2, ((int(-diffh/2),int(-diffh/2)+1), (0,0), (0,0)), 'constant')
        else:
            if extendOnly:
                im2 = np.pad(im2, ((0,-diffh), (0,0), (0,0)), 'constant')
            else:
                im2 = np.pad(im2, ((-diffh/2,-diffh/2), (0,0), (0,0)), 'constant')
    if diffw > 0:
        if diffw%2:
            if extendOnly:
                im1 = np.pad(im1, ((0,0), (0,diffw), (0,0)), 'constant')
            else:
                im1 = np.pad(im1, ((0,0), (int(diffw/2),int(diffw/2)+1), (0,0)), 'constant')
        else:
            if extendOnly:
                im1 = np.pad(im1, ((0,0), (0,diffw), (0,0)), 'constant')
            else:
                im1 = np.pad(im1, ((0,0), (diffw/2,diffw/2), (0,0)), 'constant')
    if diffw < 0:
        if diffw%2:
            if extendOnly:
                im2 = np.pad(im2, ((0,0), (0,-diffw), (0,0)), 'constant')
            else:
                im2 = np.pad(im2, ((0,0), (int(-diffw/2),int(-diffw/2)+1), (0,0)), 'constant')
        else:
            if extendOnly:
                im2 = np.pad(im2, ((0,0), (0,-diffw), (0,0)), 'constant')
            else:
                im2 = np.pad(im2, ((0,0), (-diffw/2,-diffw/2), (0,0)), 'constant')
    return (im1, im2)
