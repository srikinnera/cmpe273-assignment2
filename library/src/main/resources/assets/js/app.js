$(":button").click(function() {

    var isbn = this.id;

    alert('About to report lost on ISBN ' + isbn);
	$.ajax({
		url: "/library/v1/books/"+isbn+"?status=lost",
		type: 'PUT',
		contentType: 'application/json',
		success: function() {
		//var button=document.getElementById(isbn);
		//var stat = getElementById(stat);
		$('#stat_'+isbn).html("lost");
	    $('#'+isbn).prop("disabled",true);
	  //  window.location.reload(true);
		
		}	
	});

});