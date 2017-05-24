/**
 * Copyright (c) 2017 Cambridge Systematics, Inc.
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
// Ensure that all post requests add CSRF property.
// Assume AJAX will only be done through jQuery, and that jQuery is loaded when
// this file is included.
if (typeof jQuery != 'undefined') {
    jQuery(function () {
        // see https://stackoverflow.com/questions/28417781/jquery-add-csrf-token-to-all-post-requests-data
        var field = jQuery("#csrfField");
        var csrf_token = field.val();
        var csrf_name = field.attr('name');
        if (csrf_name && csrf_token) {
            jQuery.ajaxPrefilter(function (options, originalOptions, jqXHR) {
                if (options.type.toLowerCase() === "post") {
                    // initialize `data` to empty string if it does not exist
                    options.data = options.data || "";
                    // add leading ampersand if `data` is non-empty
                    options.data += options.data ? "&" : "";
                    // add _token entry
                    options.data += csrf_name + '=' + csrf_token;
                }
            });
        }
    });
}