/**
 *  Door Lock Code Distress Message
 *
 *  Copyright 2014 skp19
 *
 */
definition(
    name: "Notificação de codigos inseridos na fechadura",
    namespace: "quilez",
    author: "Rodrigo Quilez",
    description: "Envia uma mensagem de texto com o código inserido na fechadura.",
    category: "Safety & Security",
    iconUrl: "https://www.yale2you.com/Yale_CDN/TSDBLT_YRD220_US15_RT-selector.png",
    iconX2Url: "https://www.yale2you.com/Yale_CDN/TSDBLT_YRD220_US15_RT-selector.png")

import groovy.json.JsonSlurper

preferences {
	section("Selecione as fechaduras") {
		input "lock1", "capability.lock", multiple: true
	}
    section("Código que será monitorado") {
    	input "distressCode", "number", defaultValue: "0", title: "Código"
        input "distressCodeName", "text", defaultValue: "Maria", title: "Descrição"
    }
    section("Envio de mensagem") {
    	input "phone1", "phone", title: "Número que será notificado"
    	input "distressMsg", "text", title: "Mensagem que será enviada"
    }
    section("Envio de códigos utilizados para abrir a porta") {
    	input "discoveryMode", "bool", defaultValue: "True", title: "Habilitado"
    }
}

def installed() {
    subscribe(lock1, "lock", checkCode)
}

def updated() {
	unsubscribe()
    subscribe(lock1, "lock", checkCode)
}

def checkCode(evt) {
    log.debug "$evt.value: $evt, $settings"

    if(evt.value == "unlocked" && evt.data) {
    	def lockData = new JsonSlurper().parseText(evt.data)
        
        if(discoveryMode) {
        	sendPush "Door unlocked with user code $lockData.usedCode"
        }
        
        if(lockData.usedCode == distressCode && discoveryMode == false) {
        	log.info "Distress Message Sent"
        	sendSms(phone1, distressMsg)
        }
    }
}