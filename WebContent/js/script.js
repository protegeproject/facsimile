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
	document.getElementById(eleId).style.display='none';
	clearChildren(document.getElementById(eleId));
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