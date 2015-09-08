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
    description: "Tranca automaticamente a fechadura quando a porta estiver fechada por X minutos e destranca quando estiver aberta por mais de X segundos.",
    category: "Safety & Security",
    iconUrl: "http://www.vaishnavisoftech.com/images/unnamed-2.png",
    iconX2Url: "http://www.vaishnavisoftech.com/images/unnamed-2.png"
)

preferences{
    section("Selecione a fechadura:") {
        input "lock1", "capability.lock", required: true
    }
    section("Selecione o sensor da porta:") {
    	input "contact", "capability.contactSensor", required: true
    }   
    section("Trancar fechadura quando a porta estiver fechada por...") {
        input "minutesLater", "number", title: "Delay (in minutes):", required: true
    }
    section("Destrancar fechadura quando a porta estiver aberta por...") {
        input "secondsLater", "number", title: "Delay (in seconds):", required: true
    }
    section( "Notificações" ) {
		input "sendPushMessage", "enum", title: "Enviar notificações de push?", metadata:[values:["Yes", "No"]], required: false
		input "phoneNumber", "phone", title: "Insira o número de celular que receberá mensagem texto", required: false
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

def lockDoor(){
    log.debug "Locking the door."
    lock1.lock()
    log.debug ( "Sending Push Notification..." ) 
    if ( sendPushMessage != "No" ) sendPush( "${lock1} locked after ${contact} was closed for ${minutesLater} minutes!" )
    log.debug("Sending text message...")
    if ( phoneNumber != "0" ) sendSms( phoneNumber, "${lock1} locked after ${contact} was closed for ${minutesLater} minutes!" )
}

def unlockDoor(){
    log.debug "Unlocking the door."
    lock1.unlock()
    log.debug ( "Sending Push Notification..." ) 
    if ( sendPushMessage != "No" ) sendPush( "${lock1} unlocked after ${contact} was opened for ${secondsLater} seconds!" )
    log.debug("Sending text message...")
    if ( phoneNumber != "0" ) sendSms( phoneNumber, "${lock1} unlocked after ${contact} was opened for ${secondsLater} seconds!" )
}

def doorHandler(evt){
    if ((contact.latestValue("contact") == "open") && (evt.value == "locked")) { // If the door is open and a person locks the door then...  
        def delay = (secondsLater) // runIn uses seconds
        runIn( delay, unlockDoor )   // ...schedule (in minutes) to unlock...  We don't want the door to be closed while the lock is engaged. 
    }
    else if ((contact.latestValue("contact") == "open") && (evt.value == "unlocked")) { // If the door is open and a person unlocks it then...
        unschedule( unlockDoor ) // ...we don't need to unlock it later.
	}
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "locked")) { // If the door is closed and a person manually locks it then...
        unschedule( lockDoor ) // ...we don't need to lock it later.
    }   
    else if ((contact.latestValue("contact") == "closed") && (evt.value == "unlocked")) { // If the door is closed and a person unlocks it then...
        def delay = (minutesLater * 60) // runIn uses seconds
        runIn( delay, lockDoor ) // ...schedule (in minutes) to lock.
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "open")) { // If a person opens an unlocked door...
        unschedule( lockDoor ) // ...we don't need to lock it later.
    }
    else if ((lock1.latestValue("lock") == "unlocked") && (evt.value == "closed")) { // If a person closes an unlocked door...
        def delay = (minutesLater * 60) // runIn uses seconds
        runIn( delay, lockDoor ) // ...schedule (in minutes) to lock.
	}
    else { //Opening or Closing door when locked (in case you have a handle lock)
    	log.debug "Unlocking the door."
		lock1.unlock()
        log.debug ( "Sending Push Notification..." ) 
    	if ( sendPushMessage != "No" ) sendPush( "${lock1} unlocked after ${contact} was opened or closed when ${lock1} was locked!" )
        log.debug("Sending text message...")
    	if ( phoneNumber != "0" ) sendSms( phoneNumber, "${lock1} unlocked after ${contact} was opened or closed when ${lock1} was locked!" )
		}
}