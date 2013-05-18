var UpdateMath = function (TeX) {
    console.log("UpdateMath "+TeX);
    var updateMathFunction = function () {
		console.log("UpdateMathFunction "+TeX);
		var newNode=document.createElement("p");
		newNode.innerHTML="<p> $$ "+TeX+" $$ </p>";
		MathJax.Hub.Typeset(newNode);
		document.getElementById('c1').appendChild(newNode);
		// scroll to bottom
	$('#maximaInput').trigger( "updatelayout" );
	$('#c1').trigger( "updatelayout" );
		$(document).scrollTop(10000000);
    };
    MathJax.Hub.Queue([updateMathFunction, MathJax.Hub]);
};

var UpdateText = function (text) {
    console.log("UpdateText "+text);
    var updateTextFunction = function () {
	    console.log("updateTextFunction "+text);
		var newNode=document.createElement("span");
		newNode.innerHTML=text;
		document.getElementById('c1').appendChild(newNode);
	$('#maximaInput').trigger( "updatelayout" );
	$('#c1').trigger( "updatelayout" );
		// scroll to bottom
		$(document).scrollTop(10000000);
    };
    MathJax.Hub.Queue([updateTextFunction, MathJax.Hub]);
};

var UpdateInput=function (text) {
    console.log('UpdateInput '+text);
    UpdateText("<span onclick=\"RBT(\'"+escape(text)+ "\')\"> "+text+" </span>");
};

var RBT=function (text) {
    console.log('RBT '+text);
    var dectext=unescape(text);
    $('#maximaInput').val(dectext);
    $('#maximaInput').focus();
};

/* Simple History */
var myHistory = new Object();
myHistory.record = new Array();
myHistory.counter = 0;

var myPushHistory = function (url) {
    console.log('myPushHistory '+url);
    myHistory.record[myHistory.counter++]=url;
};

var myPopHistory = function () {
    var newURL = myHistory.record[--(myHistory.counter)];
    console.log('myPopHistory '+newURL);
    // $.mobile.changePage(newURL); /* ここ間違っている。iframeの中でページ遷移させる必要がある */
    document.getElementById('maniframe').src=newURL;
};

var BackButton = function () {
    console.log("BackButton "+$.mobile.activePage.attr('id'));
    var currentPageId=$.mobile.activePage.attr('id');
    if (currentPageId == 'mainpage') {
	window.MOA.sendToMaxima("toast: Please use Quit menu to quit.");
    } else if (currentPageId == 'menu') {
	console.log('in menu');
	$.mobile.changePage('#mainpage');
    } else {
	console.log("other case");
	if (myHistory.counter == 0) {
	    console.log('no history. back to maxima');
	    $.mobile.changePage('#mainpage');
	} else {
	    console.log('counter = '+myHistory.counter);
	    myPopHistory();
	}
    }
    /*
    if (currentPageId == 'aboutMoA') {
	console.log("page is aboutMoA");
	var winObj=document.getElementById('aboutIFR').contentWindow;
	if (winObj.history.length>0) {
	    console.log("page is aboutMoA page back");
	    window.history.back();
	} else {
	    console.log("page is aboutMoA window back");
	    window.history.back();
	}
    }
    */
	
};

var MenuButton = function () {
    console.log('MenuButton');
    $.mobile.changePage("#menu");
};

$(document).bind("mobileinit", function(){
    console.log('mobileinit');
});

$(document).ready(function (){
    $.mobile.ajaxEnabled = false;
    $.mobile.hashListeningEnabled = false;
    $.mobile.pushStateEnabled = false;
    // main page event handler
    $('#maximaInput').keypress(function(ev) {
	$('#maximaInput').trigger( "updatelayout" );
	$('#c1').trigger( "updatelayout" );
    	console.log("keypress"+ev.which);
		if (ev.which == 13) {
		    var text=$(this).val();
		    if (window.MOA == null) {
				// for debugging with PC browser
				UpdateInput(text);
				UpdateMath(text);
				UpdateText('<br><%i3> ');
		    } else {
				UpdateInput(text);
				window.MOA.sendToMaxima(text);
		    }
		}
    });

    var isSVGsupported = function () {
	return !!(document.createElementNS &&
		  document.createElementNS('http://www.w3.org/2000/svg', 'svg').createSVGRect);
    };
    var initSVGRenderer=function () {
		console.log("initSVG");
		var script = document.createElement("script");
		script.type = "text/javascript";
		script.src  = "mathjax-MathJax-24a378e/MathJax.js?config=TeX-AMS-MML_SVG";
		document.getElementsByTagName("head")[0].appendChild(script);
    };
    var initHTMLRenderer=function () {
		console.log("initHTML");
		var script = document.createElement("script");
		script.type = "text/javascript";
		script.src  = "mathjax-MathJax-24a378e/MathJax.js?config=TeX-AMS-MML_HTMLorMML";
		document.getElementsByTagName("head")[0].appendChild(script);
    };
    if (isSVGsupported()) {
    	initSVGRenderer();
    } else {
    	initHTMLRenderer();
    }
    $(document).scrollTop(10000000);

    // Menu page handler
    $("#man-lang-select").change(function() {
	var newVal = $(this).val();
	$("#maniframe").attr('src', newVal);
	console.log("man lang select "+$("#maniframe").attr('src'));
    });
    $("#quit-btn").click(function() {
	console.log("Quit");
	if (window.MOA != null) {
	    window.MOA.sendToMaxima("quit();");
	}
    });
    $('#ssave').click(function() {
	var cmd="ssave();";
	console.log(cmd);
	if (window.MOA != null) {
	    UpdateInput(cmd);
	    window.MOA.sendToMaxima(cmd);
	}
	$.mobile.changePage("#mainpage");
    });
    $('#srestore').click(function() {
	var cmd="srestore();";
	console.log(cmd);
	if (window.MOA != null) {
	    UpdateInput(cmd);
	    window.MOA.sendToMaxima(cmd);
	}
	$.mobile.changePage("#mainpage");
    });
    $('#playback').click(function() {
	var cmd="playback();";
	console.log(cmd);
	if (window.MOA != null) {
	    UpdateInput(cmd);
	    window.MOA.sendToMaxima(cmd);
	}
	$.mobile.changePage("#mainpage");
    });

    // Manual event handler
    $('#maniframe').load(function () {
	console.log('maniframe loaded');
	console.log(document.getElementById('maniframe').contentWindow.location);
	myPushHistory(document.getElementById('maniframe').contentWindow.location);
	$('#maniframe').contents().find('body').css('margin','8px 2px 8px 2px');
	$('#maniframe').contents().find('dd').css('margin','2px 2px 2px 2px');
    });
    $('.backToMaxima').click(function() {
	console.log("back "+$.mobile.activePage.attr('id'));
	//window.history.back();
	$.mobile.navigate('#mainpage',true);
    });

});
