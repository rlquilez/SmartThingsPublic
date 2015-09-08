/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Unlock It When I Arrive
 *
 *  Author: SmartThings
 *  Date: 2013-02-11
 */

definition(
    name: "Destrancar porta quando chegar",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Destranca as portas quando voce estiver dentro da sua região.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/lock_it_when_i_leave.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/lock_it_when_i_leave@2x.png"
/**
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
*/
)

preferences {
	section("Quando eu chegar..."){
		input "presence1", "capability.presenceSensor", title: "Quem?", multiple: true
	}
	section("Destancar as fechaduras..."){
		input "lock1", "capability.lock", title: "Fechaduras?", multiple: true
	}
}

def installed()
{
	subscribe(presence1, "presence.present", presence)
}

def updated()
{
	unsubscribe()
	subscribe(presence1, "presence.present", presence)
}

def presence(evt)
{
	def anyLocked = lock1.count{it.currentLock == "unlocked"} != lock1.size()
	if (anyLocked) {
		sendPush "Destrancada porta devido a chegada de $evt.displayName"
		lock1.unlock()
	}
}