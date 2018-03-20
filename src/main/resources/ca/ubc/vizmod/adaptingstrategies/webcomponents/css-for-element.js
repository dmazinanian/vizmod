var getElementByXpath = function(path) {
  return document.evaluate(path, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
}

var cssForElement = function(element) {
    let sheets = document.styleSheets, o = [];
    element.matches = element.matches || element.webkitMatchesSelector || element.mozMatchesSelector || element.msMatchesSelector || element.oMatchesSelector;
    for (let i in sheets) {
        let rules = sheets[i].rules || sheets[i].cssRules;
        for (let r in rules) {
            if (rules[r].selectorText && "" !== rules[r].selectorText &&
                    element.matches(rules[r].selectorText)) {
            	let cssSource = sheets[i].href ? "EXTERNAL_CSS" : "EMBEDDED_CSS";
                o.push({ source: cssSource, css: rules[r].cssText, url: sheets[i].href });
            }
        }
    }
    let styleAttribute = element.getAttribute("style");
    if (styleAttribute) {
    		o.push({ source: "INLINE_CSS", css: styleAttribute, url: null })
    }
    return o;
}

var cssForElementByXPath = function(xpath) {
    return JSON.stringify(cssForElement(getElementByXpath(xpath)));
}