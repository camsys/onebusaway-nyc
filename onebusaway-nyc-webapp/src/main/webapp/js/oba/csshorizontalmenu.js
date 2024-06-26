/*
 * Copyright (c) 2011 Metropolitan Transportation Authority
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

var cssmenuids=["cssmenu1"] //Enter id(s) of CSS Horizontal UL menus, separated by commas
var csssubmenuoffset=1 //Offset of submenus from main menu. Default is 0 pixels.

function createcssmenu2(){
    for (var i=0; i<cssmenuids.length; i++){
        var ultags=document.getElementById(cssmenuids[i]).getElementsByTagName("ul")
        for (var t=0; t<ultags.length; t++){
            ultags[t].style.top= (ultags[t].parentNode.offsetHeight+csssubmenuoffset -1) +"px"
            var spanref=document.createElement("span")
            spanref.className="arrowdiv"
            spanref.innerHTML=""
            ultags[t].parentNode.getElementsByTagName("a")[0].appendChild(spanref)
            ultags[t].parentNode.onmouseover=function(){
                this.style.zIndex=100
                this.getElementsByTagName("ul")[0].style.visibility="visible"
                this.getElementsByTagName("ul")[0].style.zIndex=0
            }
            ultags[t].parentNode.onmouseout=function(){
                this.style.zIndex=0
                this.getElementsByTagName("ul")[0].style.visibility="hidden"
                this.getElementsByTagName("ul")[0].style.zIndex=100
            }
        }
    }
}

if (window.addEventListener)
    window.addEventListener("load", createcssmenu2, false)
else if (window.attachEvent)
    window.attachEvent("onload", createcssmenu2)