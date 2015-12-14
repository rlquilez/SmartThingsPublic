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
 *  Trancar fechaduras automaticamente
 *
 *  Author: Rodrigo Quilez
 *
 *  Date: 2015-09-07
 */

definition(
    name: "Trancar fechaduras automaticamente",
    namespace: "quilez",
    author: "Rodrigo Quilez",
    description: "Tranca automaticamente a fechadura após determinado tempo.",
    category: "Safety & Security",
    iconUrl: "http://www.vaishnavisoftech.com/images/unnamed-2.png",
    iconX2Url: "http://www.vaishnavisoftech.com/images/unnamed-2.png"
)

preferences{
    section("Selecione a fechadura:") {
        input (name: "lock1", type: "capability.lock", required: true)
    }
    section("Selecione o sensor da porta:") {
    	input (name: "contact", type: "capability.contactSensor", required: true)
    }   
    section("Quando a porta for fechada?") {
		input (name: "closedDoor", title: "Trancar automaticamente?", type: "bool", required: true, defaultValue: true)
		input (name: "closedDelay", title: "Tempo (em segundos):", type: "number", required: true, defaultValue: 600)      
    }
    section("Quando a porta estiver aberta?") {
    	input (name: "openedDoor", title: "Destrancar automaticamente?", type: "bool", required: true, defaultValue: true)
        input (name: "openedDelay", title: "Tempo (em segundos):", type: "number", required: true, defaultValue: 2) 
	}
    section("Quando a porta estiver destrancada?") {
    	input (name: "unlockedDoor", title: "Trancar automaticamente?", type: "bool", required: true, defaultValue: true)
        input (name: "unlockedDelay", title: "Tempo (em segundos):", type: "number", required: true, defaultValue: 1800) 
    }
    section( "Notificações" ) {
		input (name: "sendEventMessage", title: "Registrar mensagem do evento?", type: "bool", required: true, defaultValue: true)
		input (name: "sendPushMessage", title: "Enviar notificações de push?", type: "bool", required: true, defaultValue: true)
		input (name: "phoneNumber", title: "Insira o número de celular que receberá mensagem texto", type: "phone", required: false)
	}
}

def installed(){
    initialize()
}

def updated(){
    unsubscribe()
    unschedule()
    initialize()
}

def initialize(){
    log.debug "Settings: ${settings}"
    subscribe(lock1, "lock", doorHandler, [filterEvents: false])
    subscribe(lock1, "unlock", doorHandler, [filterEvents: false])  
    subscribe(contact, "contact.open", doorHandler)
	subscribe(contact, "contact.closed", doorHandler)
}

def lockDoorClosed(){
    if ((contact.latestValue("contact") == "closed") && (lock1.latestValue("lock") == "unlocked")) { // Verifica que a porta esta fechada e destrancada.
    	log.debug "Trancando fechadura..."
    	lock1.lock()
    	log.debug ( "Enviando notificação..." )
		log.debug ( "CLOSEDDELAY: ${closedDelay}")
		if (closedDelay >= 60) {
			def delayTime = (closedDelay / 60)
			log.debug ( "DELAYTIME: ${delayTime}")
			send ("${lock1} trancada depois de ${contact} fechada por ${delayTime} minutos!")
		}
		else {
			send ("${lock1} trancada depois de ${contact} fechada por ${closedDelay} segundos!")
		}
	}
	else if (contact.latestValue("contact") == "open") {
		log.debug "${contact} ESTA ABERTA!"
	}
	else (lock1.latestValue("lock")) {
		log.debug "${lock1} JA ESTA TRANCADA"
	}
}

def lockDoorNoEvent(){
    if ((contact.latestValue("contact") == "closed") && (lock1.latestValue("lock") == "unlocked")) { // Verifica que a porta esta fechada e destrancada.
    	log.debug "Trancando fechadura..."
    	lock1.lock()
    	log.debug ( "Enviando notificação..." )
		log.debug ( "UNLOCKEDDELAY: ${unlockedDelay}")
		if (unlockedDelay >= 60) {
			def delayTime = (unlockedDelay / 60)
			log.debug ( "DELAYTIME: ${delayTime}")
			send ("${lock1} trancada depois de ${contact} destrancada por ${delayTime} minutos!")
		}
		else {
			send ("${lock1} trancada depois de ${contact} destrancada por ${unlockedDelay} segundos!")
		}
	}
	else if (contact.latestValue("contact") == "open") {
		log.debug "${contact} ESTA ABERTA!"
	}
	else (lock1.latestValue("lock")) {
		log.debug "${lock1} JA ESTA TRANCADA"
	}
}

def unlockDoorOpened(){
	if ((contact.latestValue("contact") == "open") && (lock1.latestValue("lock") == "locked")) { // Verifica que a porta esta fechada e destrancada.
    	log.debug "Destrancando fechadura..."
    	lock1.unlock()
    	log.debug ( "Enviando notificação..." )
		log.debug ( "OPENEDDELAY: ${openedDelay}")
		if (openedDelay >= 60) {
			def delayTime = (openedDelay / 60)
			log.debug ( "DELAYTIME: ${delayTime}")
			send ("${lock1} destrancada despois de ${contact} aberta por ${delayTime} minutos!")
		}
		else {
			send ("${lock1} destrancada despois de ${contact} aberta por ${openedDelay} segundos!")
		}
	}
	else if (contact.latestValue("contact") == "closed") {
		log.debug "${contact} ESTA FECHADA!"
	}
	else (!lock1.latestValue("lock")) {
		log.debug "${lock1} JA ESTA DESTRANCADA"
	}
}


def doorHandler(evt){
	// Caso a porta esteja ABERTA e a fechadura seja TRANCADA por alguma pessoa...
    if ((contact.latestValue("contact") == "open") && (evt.value == "locked")) {  
        runIn( openedDelay, unlockDoorOpened )   // ...o sistema irá destrancar a fechadura após o tempo determinado para evitar problemas.
    }
    // Caso a porta esteja FECHADA e a fechadura seja DESTRANCADA por alguma pessoa...
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "unlocked")) { 
        runIn( unlockedDelay, lockDoorNoEvent ) // ...o sistema irá trancar a fechadura após o tempo determinado para garantir a segurança.
    }
    // Caso a fechadura esteja DESTRANCADA e a porta seja FECHADA por alguma pessoa...
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "closed")) { 
        runIn( closedDelay, lockDoorClosed ) // ...o sistema irá trancar a fechadura após o tempo determinado para garantir a segurança.
	}
}

private send(msg) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
		if ((sendEventMessage) && (!sendPushMessage)) {
	    	log.debug("sending event message")
			sendNotificationEvent(msg)
			wait 
	    }
        if (sendPushMessage) {
            log.debug("sending push message")
            sendPush(msg)
        }
        if (phoneNumber) {
            log.debug("sending text message")
            sendSms(phoneNumber, msg)
        }
    }

	log.debug msg
}