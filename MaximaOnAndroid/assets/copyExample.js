/*
    Copyright 2012, 2013 Yasuaki Honda (yasuaki.honda@gmail.com)
    This file is part of MaximaOnAndroid.

    MaximaOnAndroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    MaximaOnAndroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MaximaOnAndroid.  If not, see <http://www.gnu.org/licenses/>.
*/

console.log('test.js');

var exampleNodes = document.getElementsByClassName('example');

for (var i=0; i<exampleNodes.length; i++) {
	exampleNodes[i].onclick = function(ev) {
		var text=ev.target.textContent;
		console.log("CECB:"+text);
	};
};
console.log('honda');

