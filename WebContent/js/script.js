/*
 * showSubquestions() and hideSubquestions() take as input:
 * [0]		the answer which triggers subquestions
 * [1..n]	the subquestions which should show/hide when [0] is selected 
 */
function showSubquestions() {
	if(document.getElementById(arguments[0]).checked)
		for (var i = 1; i < arguments.length; i++)
			show(arguments[i]);
	if(!document.getElementById(arguments[0]).checked)
		for (var i = 1; i < arguments.length; i++)
			hide(arguments[i]);
}


function hideSubquestions() {
	if(!document.getElementById(arguments[0]).checked)
		for (var i = 1; i < arguments.length; i++)
			show(arguments[i]);
	if(document.getElementById(arguments[0]).checked)
		for (var i = 1; i < arguments.length; i++)
			hide(arguments[i]);
}


function show(eleId) {
	document.getElementById(eleId).style.display='block';
}


function hide(eleId) {
	var ele = document.getElementById(eleId);
	ele.style.display='none';
	if(ele.hasAttribute("onchange")) {
		var att = ele.getAttribute("onchange");
		att = att.substring(att.indexOf("(")+1, att.indexOf(")"));
		att = replaceAll("'", "", att);
		var arr = att.split(",");
		for(var i = 1; i < arr.length; i++) {
			hide(arr[i]);
		}
	}
	clearChildren(document.getElementById(eleId));
}


function replaceAll(find, replace, str) {
	return str.replace(new RegExp(find, 'g'), replace);
}


function clearChildren(element) {
	for (var i = 0; i < element.childNodes.length; i++) {
		var e = element.childNodes[i];
		if (e.tagName) {
			switch (e.tagName.toLowerCase()) {
				case 'input':
					switch (e.type) {
						case "radio":
						case "checkbox": e.checked = false; break;
						case "button":
						case "submit":
						case "image": break;
						default: e.value = ''; break;
					}
					break;
				case 'select': e.selectedIndex = 0; break;
				case 'textarea': e.value = ''; break;
				default: clearChildren(e);
			}
		}
	}
}