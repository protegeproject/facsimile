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
		for (var i = 1; i < arguments.length; i++) {
			hide(arguments[i]);
			log("Hiding: " + arguments[i]);
			hideChildren(document.getElementById(arguments[i].childNodes))
		}
}


function hideChildren() {
	for (var i = 0; i < arguments.length; i++) {
		hide(arguments[i]);
		log("Hiding: " + arguments[i]);
	}
}


function showChildren() {
	for (var i = 0; i < arguments.length; i++)
		show(arguments[i]);
}


function show(eleId) {
	document.getElementById(eleId).style.display='block';
}


function hide(eleId) {
	document.getElementById(eleId).style.display='none';
}


function log(msg) {
    setTimeout(function() {
        throw new Error(msg);
    }, 0);
}