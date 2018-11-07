from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from typing import Any, NamedTuple, Union
from Util import ndarray
import json
import os
import pychrome
import base64
import io
import re
from matplotlib.pyplot import imread

class BrowserInstance: pass
TabInstance = Union[int, pychrome.tab.Tab]

class ChromeInstance(BrowserInstance): # pylint: disable=E0239
    def __init__(self, headless:bool=False, width:int=1920, height:int=1080) -> None:
        chrome_options = webdriver.ChromeOptions()
        chrome_options.add_argument("--window-size=%s"%("%d,%d"%(width,height)))
        if headless:
            chrome_options.add_experimental_option("excludeSwitches",["ignore-certificate-errors"])
            chrome_options.add_argument('--disable-gpu') # necessary for headless
            chrome_options.add_argument('--headless')
            chrome_options.add_argument("--hide-scrollbars")
        logpath = os.path.dirname(os.path.realpath(__file__)) + "/chromedriver.log"
        driver = webdriver.Chrome(chrome_options=chrome_options, service_args=["--verbose","--log-path=%s"%logpath])
        with open(logpath) as fp:
            log = fp.read()
            devtools_port = int(re.search(r"--remote-debugging-port=(\d+)", log).groups()[0]) # type: ignore
        os.remove(logpath)
        devtools = pychrome.Browser(url="http://127.0.0.1:%d"%devtools_port)
        devtools.list_tab()[0].start()
        self.headless = headless
        self.driver = driver
        self.devtools = devtools
        self.chrome_options = chrome_options
        self.enableTab(0)

    def getTab(self, tabIdx:int):
        tabs = self.devtools.list_tab()
        return tabs[len(tabs)-1-tabIdx]

    def newTab(self) -> pychrome.tab.Tab: #type:ignore
        tab = self.devtools.new_tab()
        self.enableTab(len(self.devtools.list_tab())-1)
        return tab

    def enableTab(self, tab:TabInstance) -> None:
        if isinstance(tab, int):
            tab = self.getTab(tab)
        else:
            tab = tab
        tab.start()
        tab.Page.enable()
        tab.Runtime.enable()

    def closeTab(self, tab:TabInstance): #type:ignore
        if isinstance(tab, int):
            tab = self.getTab(tab)
        else:
            tab = tab
        self.devtools.close_tab(tab)

    def closeAllTabsButFirst(self) -> None:
        if len(self.devtools.list_tab()) > 1:
            tab = self.devtools.list_tab()[0]
            self.devtools.close_tab(tab.id)

    def goto(self, url:str, tab:TabInstance=0) -> None: # type:ignore
        if isinstance(tab, int):
            self.enableTab(tab)
            tab = self.getTab(tab)
        else:
            tab = tab
        tab.Page.navigate(url=url)

    def close(self):
        while True:
            try:
                self.devtools.close_tab(self.devtools.list_tab()[0])
            except IndexError:
                break
        self.driver.quit()

    def eval(self, jsCode, tab:TabInstance=0) -> Any: #type:ignore
        if isinstance(tab, int):
            self.enableTab(tab)
            tab = self.getTab(tab)
        else:
            tab = tab
        jsonResp = tab.Runtime.evaluate(expression=jsCode, returnByValue=True)
        try:
            return json.loads(jsonResp['result']['value'])
        except (json.decoder.JSONDecodeError, TypeError):
            return jsonResp['result']['value']
        except KeyError:
            print("--------------------")
            print(" Exception thrown while evaluating script.")
            print(" DevTools response below: \n")
            print(jsonResp)
            print('\nDropped without exiting')


    def run(self, jsCode, tab:TabInstance=0) -> None: #type:ignore
        if isinstance(tab, int):
            self.enableTab(tab)
            tab = self.getTab(tab)
        else:
            tab = tab
        tab.Runtime.evaluate(expression=jsCode, returnByValue=True)

    def getScreenshot(self, tab:TabInstance=0) -> ndarray: #type:ignore
        if isinstance(tab, int):
            self.enableTab(tab)
            tab = self.getTab(tab)
        else:
            tab = tab        
        viewport = self.eval("""
                    (function () {
                    var body = document.body,
                    html = document.documentElement;
                    var height = Math.max( body.scrollHeight, body.offsetHeight,
                                   html.clientHeight, html.scrollHeight, html.offsetHeight );
                    var width = Math.max(body.scrollWidth, body.offsetWidth,
                                     html.clientWidth, html.scrollWidth, html.offsetWidth );
                    return JSON.stringify({x: 0, y: 0, width: width, height: height, scale: 1});
                    }());
                    """, tab=tab)
        # print(" viewport = ")
        # print(type(viewport))
        # print(viewport)
        buffer = base64.b64decode(tab.Page.captureScreenshot(clip=viewport,fromSurface=True)['data'])
        return imread(io.BytesIO(buffer))
