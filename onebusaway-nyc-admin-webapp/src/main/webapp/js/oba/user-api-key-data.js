/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

$(document).ready( function () {
    var CONTEXT_PATH = $('#contextPathHolder').attr('data-contextPath');
    $('#userApiKeyData').DataTable({
        "lengthMenu": [[100, 500, 1000, -1], [100, 500, 1000, "All"]],
        ajax: {
            url: CONTEXT_PATH + '/api/api-key/data',
            dataSrc: ''
        },
        columns: [
            { data: 'created' },
            { data: 'name' },
            { data: 'email' },
            { data: 'projectName' },
            { data: 'projectUrl' },
            { data: 'platform' },
            { data: 'apiKey' },
        ]
    } );
} );
