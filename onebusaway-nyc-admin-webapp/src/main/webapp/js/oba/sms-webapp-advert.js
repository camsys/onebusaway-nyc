/*
 * Copyright (c) 2021 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
    Created by IntelliJ IDEA.
    User: caylasavitzky
    Date: 5/21/24
    Time: 11:56 PM
*/

var csrfParameter = "";
var csrfToken = "";
var csrfHeader = "";

jQuery(function() {
    csrfParameter = $("meta[name='_csrf_parameter']").attr("content");
    csrfHeader = $("meta[name='_csrf_header']").attr("content");
    csrfToken = $("meta[name='_csrf_token']").attr("content");
    $('#showAd').change(function(){
        if($('#showAd').val() == 'true') {
            $('.advert_conditional_display').show();
        } else {
            $('.advert_conditional_display').hide();
        }
    });
    getConfigParameters();
});


function saveParameters() {
    var data = buildData();
    data = {"params": data}

    if(data != null) {
        $.ajax({
            url: "parameters!saveParameters.action",
            type: "POST",
            dataType: "json",
            data: data,
            traditional: true,
            success: function(response) {
                if(response.saveSuccess) {
                    $("#result #message").text("Your changes have been saved. " +getTime());
                    $("#results #message").delay(10000).fadeOut(5000);
                } else {
                    alert("Failed to save parameters. Please try again.");
                    $("#result #message").text("Your changes were not saved. " +getTime());
                }
            },
            error: function(request) {
                alert("Error saving parameter values");
            }
        });
    } else {
        alert("Cannot save parameters. One or more values is blank");
    }
}


function buildData() {
    var data = new Array();
    var invalid = false;
    var elements = $(".ad_update");
    for(var i=0; i<elements.length; i++) {
        var value = elements[i].value;
        if(value!=""&value!=null) {
            var key = elements[i].name;
            data.push(key + ":" + value);
        }
    }
    if(invalid) {
        data = null;
    }
    return data;
}

function getTime() {
    var lastUpdateTime = new Date();
    var hours = lastUpdateTime.getHours();
    var meridian;
    if(hours > 12) {
        hours = hours - 12;
        meridian = "PM";
    } else {
        if(hours == 12) {
            meridian = "PM";
        } else {
            meridian = "AM";
        }
    }
    var minutes = lastUpdateTime.getMinutes();
    if(minutes < 10) {
        minutes = "0" + minutes;
    }
    var seconds = lastUpdateTime.getSeconds();
    if(seconds < 10) {
        seconds = "0" + seconds;
    }
    var time = hours + ":" +  minutes + ":" + seconds + " " +meridian;

    return time;
}

function getConfigParameters() {
    $.ajax({
        url: "parameters!getParameters.action?ts=" + new Date().getTime(),
        type: "GET",
        dataType: "json",
        success: function(response) {
            updateParametersView(response.configParameters);
        },
        error: function(request) {
            alert("Error loading parameters from the server : ", request.statusText);
        }

    });
}

function updateParametersView(parameters){
    var elements = $(".ad_update")
    for(var i=0; i<elements.length; i++) {
        $(".ad_update")[i].value = parameters[$(".ad_update")[i].name]
    }
    if($('#showAd').val() == 'true') {
        $('.advert_conditional_display').show();
    } else {
        $('.advert_conditional_display').hide();
    }
}