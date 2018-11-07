
Element.prototype.isVisible=function(){"use strict";function e(e,t){return window.getComputedStyle?document.defaultView.getComputedStyle(e,null)[t]:e.currentStyle?e.currentStyle[t]:void 0}return function t(o,f,i,n,r,d,l){var s=o.parentNode;return!!function(e){for(;e=e.parentNode;)if(e==document)return!0;return!1}(o)&&(9===s.nodeType||"0"!==e(o,"opacity")&&"none"!==e(o,"display")&&"hidden"!==e(o,"visibility")&&(void 0!==f&&void 0!==i&&void 0!==n&&void 0!==r&&void 0!==d&&void 0!==l||(f=o.offsetTop,r=o.offsetLeft,n=f+o.offsetHeight,i=r+o.offsetWidth,d=o.offsetWidth,l=o.offsetHeight),!s||("hidden"!==e(s,"overflow")&&"scroll"!==e(s,"overflow")||!(r+2>s.offsetWidth+s.scrollLeft||r+d-2<s.scrollLeft||f+2>s.offsetHeight+s.scrollTop||f+l-2<s.scrollTop))&&(o.offsetParent===s&&(r+=s.offsetLeft,f+=s.offsetTop),t(s,f,i,n,r,d,l))))}(this)};

var getElementFromXpath = function(xpath) {
  return document.evaluate(xpath, document, null, XPathResult.ANY_UNORDERED_NODE_TYPE, null).singleNodeValue;
}

function getAbstraction(xpathOverlayArray) {
  return JSON.stringify((function(){
    xpathOverlayArray = (typeof xpathOverlayArray !== 'undefined') ? xpathOverlayArray : null;
	  let html_header = '\
  <!DOCTYPE html> \
  <html><head> \
  <style type="text/css"> \
   \
  html { \
  	background-color: #000000; \
  	font-family: OpenSans; \
  	font-size: 12pt; \
      -webkit-font-smoothing: antialiased; \
      -webkit-text-shadow: rgba(0,0,0,.01) 0 0 1px; \
      text-shadow: rgba(0,0,0,.01) 0 0 1px; \
  	text-rendering: optimizeLegibility; \
  	-moz-osx-font-smoothing: grayscale; \
  	position: relative; \
  } \
   \
   .overlay { \
   	border: 2px solid #ff0000; \
   	position: absolute; \
   	padding: 0px; \
   	overflow: hidden; \
   	font-size: 8pt; \
    z-index: 10; \
   } \
  .roi { \
  	border: 3px solid #000000; \
  	position: absolute; \
  	padding: 0px; \
  	overflow: hidden; \
  	font-size: 8pt; \
  } \
   \
  .text { \
  	background-color: #00ff00; \
  	z-index: 2; \
  } \
   \
  .image { \
  	background-color: #0000ff; \
  	z-index: 1; \
  } \
   \
  </style> \
  <title>ROIs</title></head> \
  <body> \
    ';

  	let html_footer = ' \
  	</body></html>';
  	// let html_footer = '';


  	// Begin runtime measurement
  	// Take a timestamp at the beginning.
  	let runtime_start = performance.now();


  	////////////////////////////////////////
  	////////////////////////////////////////
  	////////////////////////////////////////
  	//
  	//		   START OF DUPLICATION
  	//		FROM 'extract_all_rois.js'
  	//
  	////////////////////////////////////////
  	////////////////////////////////////////
  	////////////////////////////////////////


  	let roi_extraction = (function(){
  	// Begin block scope; everything defined using "let" after this will not be visible to the outside.


  	// The returned 'result' will contain a list of ROIs extracted from the page,
  	// as well as the total runtime it took the algorithm to complete.

  	let result = {rois: [], runtime: null};

  	// ============================   STEP 1   ===============================
  	// Some utility functions to generate an xpath of an element, and to
  	// retrieve an element given its xpath.
  	// =======================================================================

  	function __getElementTreeXPath(element) {
  	    // Don't call this function; use "getXpathOfElement" instead.
  	    var paths = [];

  	    // Use nodeName (instead of localName) so namespace prefix is included (if any).
  	    for (; element && element.nodeType == 1; element = element.parentNode) {
  	        var index = 0;

  	        for (var sibling = element.previousSibling; sibling; sibling = sibling.previousSibling) {
  	            // Ignore document type declaration.
  	            if (sibling.nodeType == Node.DOCUMENT_TYPE_NODE)
  	                continue;

  	            if (sibling.nodeName == element.nodeName)
  	                ++index;
  	        }

  	        var tagName = element.nodeName.toLowerCase();
			// var pathIndex = (index ? "[" + (index+1) + "]" : "");
			var pathIndex = "[" + (index+1) + "]";
			paths.splice(0, 0, tagName + pathIndex);
  	    }

  	    return paths.length ? "/" + paths.join("/") : null;
  	}

  	function getXpathOfElement(element) {
  	    // if (element && element.id) {
  	    //     return '//*[@id="]' + element.id + '"]';
  	    // } else {
  	        return __getElementTreeXPath(element);
  	    // }
  	}

  	function getElementsByXpath(xpath) {
  	    var iterator = document.evaluate(xpath, document, null, XPathResult.UNORDERED_NODE_ITERATOR_TYPE, null );

  	    var result = [];
  	    try {
  	        var thisNode = iterator.iterateNext();

  	        while (thisNode) {
  	            result.push(thisNode);
  	            thisNode = iterator.iterateNext();
  	        }

  	        return result;
  	    }
  	    catch (e) {
  	        dump( 'Error: Document tree modified during iteration ' + e );
  	    }

  	    return result;
  	}


  	// ============================   STEP 2   ===============================
  	// Extract the text ROIs.
  	// =======================================================================


  	let all_elements = document.getElementsByTagName("*");

  	let i = 0, j = 0, child_nodes = null, rect = null;

  	let TAG_IGNORE = ['SCRIPT', 'script', 'STYLE', 'style'];

  	let text_elements = [];


  	function isSpaces(str) {
  		// returns true if "str" is only one or more whitespaces
  	    // return str.match(/^ *$/) !== null;
  			var newStr = str.toString();
  			return newStr.replace(/\s/g,'') == '';
  	}



  	for (i = 0; i < all_elements.length; i++) {

  	    if ( TAG_IGNORE.indexOf(all_elements[i].nodeName) != -1 ) {
  	        // Tag found in ignore list .. so skip it
  	        continue;
  	    };

  	    child_nodes = all_elements[i].childNodes;

  	    for(j = 0; j < child_nodes.length; j++) {

  	        if (   child_nodes[j].nodeType == 3  // nodeType 3 means a text node
  	            && child_nodes[j].nodeName === '#text'
  	            && child_nodes[j].nodeValue != null
  	            && isSpaces(child_nodes[j].nodeValue) == false) {

  	            rect = child_nodes[j].parentElement.getBoundingClientRect();

  	            // if (rect.width <= 0 || rect.height <= 0 || rect.left < 0 || rect.top < 0) {
  	            //      // Not an on-screen node .. so skip it
  	            //      continue;
  	            // } else {
  							if (child_nodes[j].parentElement.isVisible()) {
  	                text_elements[text_elements.length] = { // push to array
  	                    'roi_type': 'text',
  	                    'tag_type': child_nodes[j].parentElement.nodeName.toLowerCase(),
  	                    'xpath': getXpathOfElement(child_nodes[j].parentElement),
  	                    'text': child_nodes[j].nodeValue,
  	                    'bbox': rect
  	                };

  									console.log("\n\n\n\n-----------\n" + child_nodes[j].nodeValue + "\n-----------\n\n\n\n");
  	            // };
  							};
  	        };
  	    };
  	};


  	//
  	// Find the text of all 'input' elements on the page.
  	//

  	let input_tags = document.getElementsByTagName("input");

  	for (i = 0; i < input_tags.length; i++) {
  	    if (input_tags[i].type === 'hidden') {
  	        continue;
  	    };

  	    if (! isSpaces(input_tags[i].value) ) {
  	        rect = input_tags[i].getBoundingClientRect();

  	        // if (rect.width <= 0 || rect.height <= 0 || rect.left < 0 || rect.top < 0) {
  	        //       // Not an on-screen node .. so skip it
  	        //          continue;
  	        // } else {
  								if (input_tags[i].isVisible()) {
  	                text_elements[text_elements.length] = { // push to array
  	                    'roi_type': 'text',
  	                    'tag_type': input_tags[i].nodeName.toLowerCase() + ' | ' + input_tags[i].type.toLowerCase(),
  	                    'xpath': getXpathOfElement(input_tags[i]),
  	                    'text': input_tags[i].value,
  	                    'bbox': rect
  	                };
  								};
  	        // };
  	    };
  	};


  	// ============================   STEP 2   ===============================
  	// Extract image ROIs.
  	// =======================================================================

  	let image_elements = [];

  	let img_tags = document.getElementsByTagName("img");

  	for (i = 0; i < img_tags.length; i++) {
  	    rect = img_tags[i].getBoundingClientRect();
  	    // if (rect.width <= 0 || rect.height <= 0 || rect.left < 0 || rect.top < 0)
  	    //     // Not an on-screen node .. so skip it
  	    //     continue;

  			if (img_tags[i].isVisible()) {
  		    image_elements[image_elements.length] = { // push to the array
  			    'roi_type': 'image',
  			    'tag_type': img_tags[i].nodeName.toLowerCase(),
  			    'xpath': getXpathOfElement(img_tags[i]),
  			    'src': img_tags[i].src,
  			    'bbox': rect
  		    };
  			};
  	};



  	// Not all images are in 'img' tags; many are in 'background-image' of divs
  	// and other elements. These 'background-image' images are what we will extract now.

  	for (i = 0; i < all_elements.length; i++) {

  	    if ( TAG_IGNORE.indexOf(all_elements[i].nodeName) != -1 ) {
  	        // Tag found in ignore list .. so skip it
  	        continue;
  	    };

  	    child_nodes = all_elements[i].childNodes;

  	    for(j = 0; j < child_nodes.length; j++) {

  	        if ( child_nodes[j].nodeType == Node.ELEMENT_NODE ) {

  	            var bg_img = window.getComputedStyle(child_nodes[j], null).getPropertyValue("background-image");

  	            if (   bg_img != null
  	                && bg_img != 'initial'
  	                && bg_img != 'none'
  	                && bg_img != 'inherit'
  	                && bg_img != ""
  	                && isSpaces(bg_img) == false ) {

  	                rect = child_nodes[j].getBoundingClientRect();

  	                // if (rect.width <= 0 || rect.height <= 0 || rect.left < 0 || rect.top < 0) {
  	                //      // Not an on-screen node .. so skip it
  	                //      continue;
  	                // } else {
  	                    image_elements[image_elements.length] = { // push to the array
  		                    'roi_type': 'image',
  		                    'tag_type': child_nodes[j].nodeName.toLowerCase() + ' | ' + 'background-image',
  		                    'xpath': getXpathOfElement(child_nodes[j]),
  		                    'src': bg_img,
  							// Do not include source:  sometimes a div loads a large image with multi-icons; then crops them using css ..
  							// therefore the src becomes useless really ..
  							// The final actual displayed image can be determined by cropping the screenshot
  							// as per the returned bbox coordinates ;) :)
  		                    'bbox': rect
  	                	};
  	                // };
  	            };
  	        };
  	    };
  	};


  	// Report all ROIs

  	for (i = 0; i < text_elements.length; i++) {
  		result.rois[result.rois.length] = text_elements[i]; // push to array
  	};

  	for (i = 0; i < image_elements.length; i++) {
  		result.rois[result.rois.length] = image_elements[i]; // push to array
  	};

  	return result;

  	// End block scope
  	})();



  	////////////////////////////////////////
  	////////////////////////////////////////
  	////////////////////////////////////////
  	//
  	//		    END OF DUPLICATION
  	//		FROM 'extract_all_rois.js'
  	//
  	////////////////////////////////////////
  	////////////////////////////////////////
  	////////////////////////////////////////


  	// Take a final timestamp & record total runtime.
  	let runtime_end = performance.now();

    let final_result = {html: null, bboxes: [], types: [], xpaths: [],  runtime: null};
  	final_result.runtime = (runtime_end - runtime_start);




  	// HTML creation loop. Loop through all extracted ROIs and represent them with elements on
  	// the final HTML page.

  	let rois_html = '';

    let overlay_html = '';
	var rect, el;

    if (xpathOverlayArray != null) {
      xpathOverlayArray = JSON.parse(xpathOverlayArray);
      for (i = 0; i < xpathOverlayArray.length; i++) {
          el = getElementFromXpath(xpathOverlayArray[i]);
          rect = el.getBoundingClientRect();
          overlay_html += '<div class="overlay" \
    	style="left: ' + rect.left + 'px; top: ' + rect.top
		+ 'px; width: ' + rect.width + 'px; height: ' + rect.height + 'px;"></div>\n';
		final_result.bboxes.push({
			left: rect.left, bottom: rect.bottom, right: rect.right, top: rect.top,
			width: rect.width, height: rect.height, area: rect.width*rect.height
		});
		final_result.types.push('overlay');
	  };
	  final_result.xpaths = xpathOverlayArray;
    } else {
		for (i = 0; i < roi_extraction.rois.length; i++) {
			rois_html += '<div class="roi ' + roi_extraction.rois[i].roi_type + '" \
			style="left: ' +
			roi_extraction.rois[i].bbox.left
			+ 'px; top: '    +
			roi_extraction.rois[i].bbox.top
			+ 'px; width: ' +
			roi_extraction.rois[i].bbox.width
			+ 'px; height: ' +
			roi_extraction.rois[i].bbox.height + 'px;" title=\''+roi_extraction.rois[i].xpath+'\'></div>\n';
			rect = roi_extraction.rois[i].bbox;
			final_result.bboxes.push({
				left: rect.left, bottom: rect.bottom, right: rect.right, top: rect.top,
				width: rect.width, height: rect.height, area: rect.width*rect.height				
			});
			final_result.types.push(roi_extraction.rois[i].roi_type);
			final_result.xpaths.push(roi_extraction.rois[i].xpath);
		};
	};

  	final_result.html = html_header + rois_html + overlay_html + html_footer;

  	return final_result;


  })());
};
