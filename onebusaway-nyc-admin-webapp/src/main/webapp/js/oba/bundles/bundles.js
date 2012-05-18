/*
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/*jQuery(document).ready(function(){
	$('.tab').click(function(){
		$('#content > #text > #breadcrumb > li.active').removeClass('active');
		$(this).parent().addClass('active');
		$('#content > #text > #breadcrumb_contents_container > div.tab_contents_active')
			.removeClass('tab_contents_active');
		$(this.rel).addClass('tab_contents_active');
	});
});*/

jQuery(document).ready(function() {

	//Select the first tab on page load
	var $tabs = $('#breadcrumb').tabs({selected:0});
	var selected = $tabs.tabs('option', 'selected');
	alert(selected);
	$('#breadcrumb_contents_container').html();
	
	$('#breadcrumb').bind('tabsselect', function(event, ui) {
	    alert("tab");
	});
});



