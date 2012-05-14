$(document).ready(function(){
	$('.tab').click(function(){
		$('#content > #text > #breadcrumb > li.active').removeClass('active');
		$(this).parent().addClass('active');
		$('#content > #text > #breadcrumb_contents_container > div.tab_contents_active')
			.removeClass('active');
		$(this.rel).addClass('active');
	});
});
