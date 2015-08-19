/**
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

jQuery(function() {
	console.log("in bundle-validation");
	jQuery("#validateBundleButton").click(onValidateBundleButtonClick);
});

function onValidateBundleButtonClick() {
	var csvFile = jQuery("#csvFile").val();
	console.log("CSV File:" + csvFile);
	console.log(jQuery("#csvFile").val());
	console.log(jQuery("#csvFile").attr('value'));
	console.log(jQuery("input[name=environmentOptions]:checked").val());
	var checkEnvironment = jQuery("input[name=environmentOptions]:checked").val();
	var checkEnvironment = jQuery("input[name=environmentOptions]:checked").val();
	// var csvDataFile = jQuery("#csvFile")[0].files[0];
	//var csvDataFile = jQuery("#csvFile").files[0];
	//var csvDataFile = jQuery("#csvFile").val();
	var csvDataFile = document.getElementById('csvFile').files[0];
	
	console.log("file name is: " + csvDataFile.name);
	var formData = new FormData();
	formData.append("ts", new Date().getTime());
	formData.append("csvFile", csvFile);
	formData.append("checkEnvironment", checkEnvironment);
	formData.append("csvDataFile", csvDataFile);

	jQuery.ajax({
		url: "validate-bundle!runValidateBundle.action",
		type: "POST",
		data: formData,
		cache: false,
		processData: false,
		contentType: false, 
		async: false,
		success: function(data) {
			console.log("Successfully called Validate");
			$('#bundleValidationResults').text('');
			var header_row = '<tr> \
			      <th>Line</th> \
			      <th>Csv file line</th> \
			      <th>Result</th> \
			      <th>Specific Test</th> \
			      <th>Summary</th> \
			      <th>Query Used</th> \
			    </tr>';
			$('#bundleValidationResults').append(header_row);
			
			$.each(data, function(index, value) {
				var testClass = '';
				if (value.testStatus === 'Pass') {
				    testClass = 'testPass';
				} else {
				    testClass = 'testFail';
				}
				var new_row = '<tr> \
					<td>' + value.linenum + '</td> \
					<td>' + value.csvLinenum + '</td> \
					<td class=' + testClass + '>' + value.testStatus + '</td> \
					<td>' + value.specificTest + '</td> \
					<td>' + value.testResult + '</td> \
					<td>' + value.testQuery + '</td> \
					</tr>';
				$('#bundleValidationResults').append(new_row);
				
			});
		},
		error: function(request) {
			console.log("Error calling Validate");
			alert("There was an error processing your request. Please try again.");
		}
	});

}
