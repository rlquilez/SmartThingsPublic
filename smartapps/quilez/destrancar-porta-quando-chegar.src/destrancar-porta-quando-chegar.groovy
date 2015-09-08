/**
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
 *  Destrancar porta quando chegar
 *
 *  Author: Rodrigo Quilez
 *
 *  Date: 2015-09-07
 */

definition(
    name: "Destrancar porta quando chegar",
    namespace: "quilez",
    author: "Rodrigo Quilez",
    description: "Destranca as portas quando voce estiver dentro da sua região.",
    category: "Safety & Security",
    iconUrl: "http://appcrawlr.com/thumbs/app/icon/51/1307151.png",
    iconX2Url: "http://appcrawlr.com/thumbs/app/icon/51/1307151.png"
)

preferences {
	section("Quando eu chegar..."){
		input "presence1", "capability.presenceSensor", title: "Quem?", multiple: true
	}
	section("Destancar as fechaduras..."){
		input "lock1", "capability.lock", title: "Fechaduras?", multiple: true
	}
    section( "Notificações" ) {
    input("recipients", "contact", title: "Enviar notificações para") {
        input "sendPushMessage", "enum", title: "Enviar notificações de push?", options: ["Sim", "Não"], required: false
        input "phoneNumber", "phone", title: "Insira o número de celular que receberá mensagem texto", required: false
    	}
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
		send "Destrancada porta devido a chegada de $evt.displayName"
		lock1.unlock()
	}
}

private send(msg) {
    if (location.contactBookEnabled) {
        log.debug("Enviando notificação para: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "Não") {
            log.debug("Enviando mensagem de push")
            sendPush(msg)
        }

        if (phoneNumber) {
            log.debug("Enviando mensagem de texto")
            sendSms(phoneNumber, msg)
        }
    }

	log.debug msg
}