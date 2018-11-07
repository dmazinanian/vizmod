from collections import defaultdict
from operator import mul
# mul:Callable[[int,int],int]
from functools import reduce
import re
import pychrome
import time
from typing import List, Tuple, Union, Dict, Callable
import json
import os
from Driver import BrowserInstance, TabInstance
from Util import ndarray, abstraction_js_path

XPATH_NODES = re.compile("\/([^\/\s]+)")

def getXPaths(browser:BrowserInstance, #type: ignore
            tab:TabInstance=0) -> List[str]:
    with open(abstraction_js_path, "r") as fp:
        abstraction_js = fp.read()
    browser.run(abstraction_js, tab)
    abstraction = browser.eval("getAbstraction();", tab)
    return abstraction['xpaths']

def overlayXPaths(xpaths:List[str], #type:ignore
                  browser:BrowserInstance,
                  tab:TabInstance=0) -> ndarray:
    with open(abstraction_js_path, "r") as fp:
        abstraction_js = fp.read()
    browser.run(abstraction_js, tab)
    abstraction = browser.eval("getAbstraction('%s');"%json.dumps(xpaths), tab)
    ROIs_html = abstraction['html']
    tempfile = os.path.dirname(os.path.realpath(__file__)) + "/temp.html"
    with open(tempfile, "w") as fp:
        fp.write(ROIs_html)
    newTab = browser.newTab()
    browser.goto("file://"+tempfile, newTab)
    image = browser.getScreenshot(newTab)
    os.remove(tempfile)
    browser.closeTab(newTab)
    return image

def computeDiversityScore(xpaths:List[str]) -> int:
    counted:Dict[str,int] = defaultdict(int)
    for i,v in enumerate(xpaths):
        counted[v] += 1
    score:int = reduce(mul, counted.values(), 1)
    return score

def shrinkXPaths(xpaths:List[str]) -> List[str]:
    tree:List[List[str]] = []
    lengths:List[int] = []
    maxLength = 0
    for i in range(len(xpaths)):
        nodes:List[str] = re.findall(XPATH_NODES, xpaths[i])
        length = len(nodes)
        tree.append(nodes)
        lengths.append(length)
        if length > maxLength:
            maxLength = length
    for i in range(len(xpaths)):
        if lengths[i] == maxLength:
            tree[i].pop()
    final = []
    for i in range(len(xpaths)):
        final.append('/'+'/'.join(tree[i]))
    return final

def clusterXPaths(xpaths:List[str]) -> List[str]:
    numiter:int = getMaxLength(xpaths)
    scores:List[int] = []
    xpathsCollection:List[List[str]] = []
    maxScore = 0
    currentXpaths = xpaths
    for i in range(numiter):
        score:int = computeDiversityScore(currentXpaths)
        scores.append(score)
        xpathsCollection.append(currentXpaths)
        if score > maxScore:
            maxScore = score
        currentXpaths = shrinkXPaths(currentXpaths)
    for i in range(numiter):
        if scores[i] == maxScore:
            finalXpaths = xpathsCollection[i]
            break
    return finalXpaths

def calculateDistance(xpathA:str, xpathB:str, onlyUptoCommonAncestor:bool = False) -> int:
    # returns the shortest distance from either xpaths to the nearest common ancestor.
    a:List[str] = re.findall(XPATH_NODES, xpathA)
    b:List[str] = re.findall(XPATH_NODES, xpathB)
    distance = 0
    lenDiff = len(a)-len(b)
    commonLen = len(b) if lenDiff >= 0 else len(a)
    for i in range(commonLen):
        factor = commonLen - i
        if a[i] != b[i]:
            distance += 1*factor
            break
    if not onlyUptoCommonAncestor:
        # if not only upto common ancestor,
        # add downwards leg of distance
        # (i.e., from common ancestor to other branch)
        if len(b) > commonLen:
            distance += len(b) - i
        else:
            distance += len(a) - i
    return distance

def getMaxLength(xpaths:List[str]) -> int:
    maxLength = 0
    for i in range(len(xpaths)):
        nodes = re.findall(XPATH_NODES, xpaths[i])
        length = len(nodes)
        if length > maxLength:
            maxLength = length
    return maxLength
