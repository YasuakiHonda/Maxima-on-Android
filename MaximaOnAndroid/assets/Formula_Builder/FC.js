$( document ).on( "pageinit", function() {
    $(".mathPop").bind("click", function(ev) {
	var cmd=$(ev.target).text();
	var newText=computeNewText(cmd);
	$('#textarea1').selection('replace', {
	    text: newText,
	    caret: 'keep'
	});
	$(this).popup('close');
	return false;
    });

    var computeNewText = function (cmd) {
	var replacementText=cmd;
	var curtext=$('#textarea1').val();
	console.log(curtext);
	var sel=$('#textarea1').selection('getPos');
	console.log('computeNT '+curtext[sel.end]);
	if (curtext[sel.end]=='(') {
	    var fnameend=cmd.indexOf('(',0);
	    if (fnameend != -1) {
		replacementText=cmd.substring(0,fnameend);
	    }
	}
	return replacementText;
    };

    $("#caretR").bind("click", function(ev) {
	setNewCaretPos(1);
	return false;
    });
    
    $("#caretL").bind("click", function(ev) {
	setNewCaretPos(-1);
	return false;
    });

    var setNewCaretPos=function(vector) {
	var len=$('#textarea1').val().length;
	var sel=$('#textarea1').selection('getPos');
	if (sel.start != sel.end) {
	    $('#textarea1').selection('setPos', {start: sel.end, end: sel.end});
	    return false;
	}
	var newpos=sel.start+vector;
	if ((0<=newpos) && (newpos<=len)) {
	    $('#textarea1').selection('setPos', {start: newpos, end: newpos});
	}
	return false;
    };
	
    $("#selR").bind("click", function(ev) {
	console.log('selR');
	ev.preventDefault();
	var str=$('#textarea1').val();
	var sel=$('#textarea1').selection('getPos');
	var rpos=sel.end;
	var numb = '0123456789';
	var lwr = 'abcdefghijklmnopqrstuvwxyz';
	var upr = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
	var alphnum=numb+lwr+upr;
	while (alphnum.indexOf(str.substring(rpos,rpos+1)) == -1) {
	    rpos++;
	    if (rpos > str.length) {
		$('#textarea1').selection('setPos', {start: str.length, end: str.length});
		return false;
	    }
	    
	}
	while (alphnum.indexOf(str.substring(rpos,rpos+1)) != -1) {
	    rpos++;
	    if (rpos > str.length) {
		rpos=str.length;
		break;
	    }
	}
	var lpos=rpos-1;
	while (alphnum.indexOf(str.substring(lpos,lpos+1)) != -1) {
	    lpos--;
	    if (lpos < 0) {
		lpos=0;
		break;
	    }
	}
	if (lpos>0) lpos++;
	$('#textarea1').selection('setPos', {start: lpos, end: rpos});
	console.log('selR'+ lpos + ' ' + rpos)
	return false;
    });
    
    $("#selL").bind("click", function(ev) {
	console.log('selR');
	var str=$('#textarea1').val();
	var sel=$('#textarea1').selection('getPos');
	var lpos=sel.start;
	if (lpos == 0) {
	    $('#textarea1').focus();
	    return false;
	}
	var numb = '0123456789';
	var lwr = 'abcdefghijklmnopqrstuvwxyz';
	var upr = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
	var alphnum=numb+lwr+upr;
	while (alphnum.indexOf(str.substring(lpos-1,lpos)) == -1) {
	    lpos--;
	    if (lpos <= 1) {
		$('textarea1').selection('setPos', {start: 0, end: 0});
		return false;
	    }
	}
	while (alphnum.indexOf(str.substring(lpos-1,lpos)) != -1) {
	    lpos--;
	    if (lpos < 0) {
		lpos=0;
		break;
	    }
	}
	var rpos=lpos;
	while (alphnum.indexOf(str.substring(rpos,rpos+1)) != -1) {
	    rpos++;
	    if (rpos > str.length) {
		rpos=str.lenght;
		break;
	    }
	}
	$('#textarea1').selection('setPos', {start:lpos, end:rpos});
	return false;
    });
    

});
