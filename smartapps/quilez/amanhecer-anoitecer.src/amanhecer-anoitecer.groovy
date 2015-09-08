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
 *  Amanhecer/Anoitecer
 *
 *  Author: Rodrigo Quilez
 *
 *  Date: 2015-09-07
 */
 
definition(
    name: "Amanhecer/Anoitecer",
    namespace: "quilez",
    author: "Rodrigo Quilez",
    description: "Altera o modo e controla as luzes baseado no amanhacer e anoitecer.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/rise-and-shine@2x.png"
)

preferences {
	section ("Ao amanhecer...") {
		input "sunriseMode", "mode", title: "Alterar modo para?", required: false
		input "sunriseOn", "capability.switch", title: "Ligar?", required: false, multiple: true
		input "sunriseOff", "capability.switch", title: "Desligar?", required: false, multiple: true
	}
	section ("Ao anoitecer...") {
		input "sunsetMode", "mode", title: "Alterar modo para?", required: false
		input "sunsetOn", "capability.switch", title: "Ligar?", required: false, multiple: true
		input "sunsetOff", "capability.switch", title: "Desligar?", required: false, multiple: true
	}
	section ("Ajuste de diferença do amanhacer (opcional)...") {
		input "sunriseOffsetValue", "text", title: "HH:MM", required: false
		input "sunriseOffsetDir", "enum", title: "Antes ou Depois", required: false, options: ["Before","After"]
	}
	section ("Ajuste de diferença do anoitecer (opcional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Antes ou Depois", required: false, options: ["Before","After"]
	}
	section ("CEP (opcional, por padrão serão utilizadas as coordenadas da localização)...") {
		input "zipCode", "text", required: false
	}
	section( "Notificações" ) {
        input("recipients", "contact", title: "Enviar notificações para") {
            input "sendPushMessage", "enum", title: "Enviar notificações de push?", options: ["Yes", "No"], required: false
            input "phoneNumber", "phone", title: "Insira o número de celular para enviar mensagem de SMS.", required: false
        }
	}

}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	//unschedule handled in astroCheck method
	initialize()
}

def initialize() {
	subscribe(location, "position", locationPositionChange)
	subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)

	astroCheck()
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	log.trace "sunriseSunsetTimeHandler()"
	astroCheck()
}

def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)

	def now = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
	log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"

	if (state.riseTime != riseTime.time) {
		unschedule("sunriseHandler")

		if(riseTime.before(now)) {
			riseTime = riseTime.next()
		}

		state.riseTime = riseTime.time

		log.info "scheduling sunrise handler for $riseTime"
		schedule(riseTime, sunriseHandler)
	}

	if (state.setTime != setTime.time) {
		unschedule("sunsetHandler")

	    if(setTime.before(now)) {
		    setTime = setTime.next()
	    }

		state.setTime = setTime.time

		log.info "scheduling sunset handler for $setTime"
	    schedule(setTime, sunsetHandler)
	}
}

def sunriseHandler() {
	log.info "Executing sunrise handler"
	if (sunriseOn) {
		sunriseOn.on()
	}
	if (sunriseOff) {
		sunriseOff.off()
	}
	changeMode(sunriseMode)
}

def sunsetHandler() {
	log.info "Executing sunset handler"
	if (sunsetOn) {
		sunsetOn.on()
	}
	if (sunsetOff) {
		sunsetOff.off()
	}
	changeMode(sunsetMode)
}

def changeMode(newMode) {
	if (newMode && location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
			send "${label} has changed the mode to '${newMode}'"
		}
		else {
			send "${label} tried to change to undefined mode '${newMode}'"
		}
	}
}

private send(msg) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
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

private getLabel() {
	app.label ?: "SmartThings"
}

private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}