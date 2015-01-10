function showSubquestions() {
	if(document.getElementById(arguments[0]).checked) {
		for (var i = 1; i < arguments.length; i++) {
			document.getElementById(arguments[i]).style.display='block';
		}
	}
	if(!document.getElementById(arguments[0]).checked) {
		for (var i = 1; i < arguments.length; i++) {
			document.getElementById(arguments[i]).style.display='none';
		}
	}
}
