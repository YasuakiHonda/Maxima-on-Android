var UpdateMath = function (TeX) {
    console.log("UpdateMath "+TeX);
    var updateMathFunction = function () {
		console.log("UpdateMathFunction "+TeX);
		var newNode=document.createElement("p");
		newNode.innerHTML="<p> $$ "+TeX+" $$ </p>";
		MathJax.Hub.Typeset(newNode);
		document.getElementById('c1').appendChild(newNode);
		// scroll to bottom
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
};


$(document).ready(function (){
    $('#optionMenuBtn').bind('click',function(ev) {
	$('#panel1').panel('toggle');
    });
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

});
