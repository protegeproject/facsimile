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
}