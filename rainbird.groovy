/***********************************************************************************************************************
*  Copyright 2020 craigde
*
*  Contributors:
*  jbarrancos - Thanks for documenting the Rainbird API calls and encryption routine - https://github.com/jbarrancos/pyrainbird project. this was a great reference to speed development.
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
*  Weatherstack Weather Driver
*
*  Author: craigde
*
*  Date: 2020-07-11
*
*  for use with HUBITAT
*
*  features: Supports the following so far
*    Advance Station - Run the passed station number
*    Current Date - Return the wifi current date
*    Current Time - Return the wifi current time
*    Current Irrigation State - Returns current watering state
*    Rain Delay Get - Returns current rain delay
*    Rain Delay Set - Sets rain delay
*    Model and Version - Returns model and version of wifi controller
*    Serial Number - Return the wifi controller serial number
*
***********************************************************************************************************************/

public static String version()      {  return "v0.90"  }

/***********************************************************************************************************************
*
* Version 0.9
*   7/11/2020: 0.9 - intial version of driver. Underlying encryption and comms complete. First set of commands complete. Still need to complete command set. 
*/

//import groovy.transform.Field
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import groovy.json.JsonSlurper

metadata    {
    definition (name: "Rainbird Sprinkler Controller Driver", namespace: "craigde", author: "craigde")  {
     
        capability "Refresh"
        capability "Switch"
        capability "Actuator"
        capability "Valve"
        capability "Sensor"
        capability "Polling"
        capability "Health Check"

        attribute "modelID", "string"
        attribute "hardwareDesc", "string"
        attribute "protocolRevisionMajor", "string"
        attribute "protocolRevisionMinor", "string"
        attribute "serialNo", "string"
        attribute "currentTime", "string"
        attribute "currentDate", "string"
        attribute "rainDelay", "string"
        attribute "watering", "boolean"
        //attribute "controllerOn", "string"

        //currently implemented commands
        command "ModelAndVersionRequest"
        command "SerialNumberRequest"
        command "CurrentTimeRequest"
        command "CurrentDateRequest"
        command "StopIrrigationRequest"
        command "RainDelayGetRequest"
        command "RainDelaySetRequest", ["number"]
        command "AdvanceStationRequest", ["number"]
        command "CurrentIrrigationStateRequest"

 
        //To be implemented
        //command "WaterBudgetRequest", ["number"]
        //command "ZonesSeasonalAdjustFactorRequest"
        //command "CurrentRunTimeRequest", ["number"]
        //command "CurrentRainSensorStateRequest"
        //command "CurrentStationsActiveRequest", ["number"]
        //command "ManuallyRunProgramRequest", ["number"]
        //command "ManuallyRunStationRequest", ["number", "number"]
        //command "TestStationsRequest", ["number"]
        
        //command "AvailableStationsRequest", ["number"]
        //command "CommandSupportRequest", ["sring"]
        //command "CurrentControllerStateSet", ["number"]
        //command "ControllerEventTimestampRequest", ["number"]
        //command "StackManuallyRunStationRequest", ["number"]
        //command "CombinedControllerStateRequest"
  }
    
}
 
preferences {
    section("Sprinker IP Address (x.x.x.x):") {
        input "SprinklerIP", "string", required: true, title: "Where?"
    }
    section("Sprinkler Password (string):") {
        input "SprinklerPassword", "string", required: true, title: "Password?"
    }
    section("Polling interval (minutes):") {
        input "minutes", "number", required: true, title: "How often?"
    }
    section("Collect Additional Debug information") {
        input "isDebug", "bool", title:"Debug mode", required:true, defaultValue:false
    }    
}

def initialize() {
    ModelAndVersionRequest()
    CurrentTimeRequest ()
    CurrentDateRequest ()
}

void installed() {
    initialize()
    state.isInstalled = true
}

void updated() {
    initialize()
}
    
def ModelAndVersionRequest () {
    response = SendData ("02", 1)
    //"82": {"length": 5, "type": "ModelAndVersionResponse", "modelID": {"position": 2, "length": 4},"protocolRevisionMajor": {"position": 6, "length": 2},"protocolRevisionMinor": {"position": 8, "length": 2}},
    if (isDebug) { log.debug "Json Data: ${response.result.data}" }
    
    if (response.result.data.reverse().endsWith("28")) {
       state.modelID = response.result.data.substring(2,6)                                   
       state.protocolRevisionMajor = response.result.data.substring(6,8)                                   
       state.protocolRevisionMinor = response.result.data.substring(8,10)                                   
               
       sendEvent(name: "modelID", value: state.modelID, displayed: true);
       sendEvent(name: "protocolRevisionMajor", value: state.protocolRevisionMajor, displayed: true);
       sendEvent(name: "protocolRevisionMinor", value: state.protocolRevisionMinor, displayed: true);
    }
    else {
       log.debug "ModelAndVersionRequest Fail: ${response.result.data}"
    }
}

def SerialNumberRequest () {
    response = SendData ("05", 1)
    //"85": {"length": 9, "type": "SerialNumberResponse", "serialNumber": {"position": 2, "length": 16}},
    if (isDebug) { log.debug "Json Data: ${response.result.data}" }
    
    if (response.result.data.reverse().endsWith("58")) {
       state.serialNo = response.result.data.substring(2)                                   
       sendEvent(name: "serialNo", value: state.serialNo, displayed: true);
    }
    else {
       log.debug "SerialNumberRequest Fail: ${response.result.data}"
    }
}


def CurrentTimeRequest () {
    response = SendData ("10", 1)
    //"90": {"length": 4, "type": "CurrentTimeResponse", "hour": {"position": 2, "length": 2}, "minute": {"position": 4, "length": 2}, "second": {"position": 6, "length": 2}},
	if (isDebug) { log.debug "Json Data: ${response.result.data}" }
    
    if (response.result.data.reverse().endsWith("09")) {
      
      def currentTimeHour = Integer.parseInt(response.result.data.substring(2,4),16) 
      def currentTimeMinute = Integer.parseInt(response.result.data.substring(4,6),16)
      def currentTimeSecond = Integer.parseInt(response.result.data.substring(6,8),16)
              
      state.currentTime = "${currentTimeHour}${currentTimeMinute}${currentTimeSecond}"
      sendEvent(name: "currentTime", value: state.currentTime, displayed: true);
    }
    else {
       log.debug "CurrentTimeRequest Fail: ${response.result.data}"
    }
}

def CurrentDateRequest () {
    response = SendData ("12", 1)
    //"92": {"length": 4, "type": "CurrentDateResponse", "day": {"position": 2, "length": 2}, "month": {"position": 4, "length": 1}, "year": {"position": 5, "length": 3}},
	if (isDebug) { log.debug "Json Data: ${response.result.data}" }
    
    if (response.result.data.reverse().endsWith("29")) {
      
      def currentDateDay = Integer.parseInt(response.result.data.substring(2,4),16) 
      def currentDateMonth = Integer.parseInt(response.result.data.substring(4,5),16)
      def currentDateYear = Integer.parseInt(response.result.data.substring(5,8),16)
              
      state.currentDate = "${currentDateDay}${currentDateMonth}${currentDateYear}"
      sendEvent(name: "currentDate", value: state.currentDate, displayed: true);
    }
    else {
       log.debug "CurrentDateRequest Fail: ${response.result.data}"
    }
}

def StopIrrigationRequest () {
    //"StopIrrigationRequest": {"command": "40", "response": "01", "length": 1},
    response = SendData ("40", 1)
	//	"01": {"length": 2, "type": "AcknowledgeResponse", "commandEcho": {"position": 2, "length": 2}},
	if (isDebug) { log.debug "Json Data: ${response.result.data}" }
    
    if (response.result.data.reverse().endsWith("10")) {
      state.watering = false
      sendEvent(name: "watering", value: state.watering, displayed: true);
    }
    else {
       log.debug "StopIrrigationRequest Fail: ${response.result.data}"
    }
}

def RainDelayGetRequest () {
    response = SendData ("36", 1)
    //"B6": {"length": 3, "type": "RainDelaySettingResponse", "delaySetting": {"position": 2, "length": 4}},
	if (isDebug) { log.debug "Json Data: ${response.result.data}" }
    
    if (response.result.data.reverse().endsWith("6B")) {
       def currentRainDelay = Integer.parseInt(response.result.data.substring(2,6),16)
        
       state.rainDelay = currentRainDelay                                   
       sendEvent(name: "rainDelay", value: state.rainDelay, displayed: true)
    }
    else {
       log.debug "RainDelayGetRequest Fail: ${response.result.data}"
    }
        
}

def RainDelaySetRequest (_rainDelay) {
    _rainDelay=_rainDelay.toString()
    if (_rainDelay.isNumber()) {
        _rainDelay = Integer.parseInt(_rainDelay)
       
       if (_rainDelay < 9) {
           _rainDelayHex="000"+Integer.toHexString(_rainDelay)
       }
       else {
       _rainDelayHex="00${_rainDelay}"
        
       }
       log.debug "_rainDelayHex ${_rainDelayHex}"
       log.debug "sending: 37${_rainDelayHex}"
       response = SendData ("37${_rainDelayHex}", 3)
	   // "01": {"length": 2, "type": "AcknowledgeResponse", "commandEcho": {"position": 2, "length": 2}},

       if (response.result.data.reverse().endsWith("10")) {
          state.rainDelay = _rainDelay                                  
          sendEvent(name: "rainDelay", value: state.rainDelay, displayed: true)
       }
       else {
          log.debug "RainDelaySetRequest Fail: ${response.result.data}"
       }
    }
    else {
       log.debug "Invalid RainDelay: ${_rainDelay}"
       return false 
    }
}

def CurrentIrrigationStateRequest () {
  
    //"CurrentIrrigationStateRequest": {"command": "48", "response": "C8", "length": 1},
       
    response = SendData ("48", 1)
    //	"C8": {"length": 2, "type": "CurrentIrrigationStateResponse", "irrigationState": {"position": 2, "length": 2}},
	
    if (response.result.data.reverse().endsWith("8C")) {
       def currentIrrigationState = Integer.parseInt(response.result.data.substring(2,4),16)
       log.debug "currentIrrigationState: ${currentIrrigationState}"  
    }
    else {
       log.debug "CurrentIrrigationStateRequest Fail: ${response.result.data}"
    }
}
 


def AdvanceStationRequest (_station) {
    //	"AdvanceStationRequest": {"command": "42", "parameter": 0, "response": "01", "length": 2},
    _station=_station.toString()
    if (_station.isNumber()) {
        _station = Integer.parseInt(_station)
       
       if (_station < 9) {
           _station="000"+Integer.toHexString(_station)
       }
       else {
       _rainDelayHex="00${_rainDelay}"
        
       }
       log.debug "_stationHex ${_stationHex}"
       log.debug "sending: 42${_stationHex}"
       response = SendData ("42${_stationHex}", 3)
	   // "01": {"length": 2, "type": "AcknowledgeResponse", "commandEcho": {"position": 2, "length": 2}},

       if (response.result.data.reverse().endsWith("10")) {
       }
       else {
          log.debug "RainDelaySetRequest Fail: ${response.result.data}"
       }
    }
    else {
       log.debug "Invalid RainDelay: ${_rainDelay}"
       return false 
    }
}


def SendData(strSendCommand, intLength) {
    long request_id = Math.floor((new Date()).getTime()/1000);
    if (isDebug) { log.debug "request_id = " + request_id }  
    
    byte[] responseBytes = [:]

    strSendData = /{"id":$request_id,"jsonrpc":"2.0","method":"tunnelSip","params":{"data":$strSendCommand,"length":$intLength}}/
 
    byte [] baEncryptedSendData = encrypt (strSendData, SprinklerPassword)
    if (isDebug) { log.debug "encrypt return = " + baEncryptedSendData.encodeHex().toString() }  
    
     def postParams = [
		uri: "http://$SprinklerIP/stick",
        contentType: "application/octet-stream",
        requestContentType: "application/octet-stream",
        headers: [
        "Accept-Language": "en",
        "Accept-Encoding": "gzip, deflate",
        "User-Agent": "RainBird/2.0 CFNetwork/811.5.4 Darwin/16.7.0",
        "Accept": "*/*",
        "Connection": "keep-alive"
        ],
		body : baEncryptedSendData
	]
    
        httpPost(postParams) { resp ->
        responseBytes = resp.data.bytes
            
         if (isDebug) {
             log.debug "Response Status: ${resp.status}"
             log.debug "Received Headers: ${resp.getAllHeaders()}"
             log.debug "Response Result: ${responseBytes.encodeHex().toString()}"
         }
          
       }
        
       temp2 = decrypt (responseBytes, SprinklerPassword)
       if (isDebug) { log.debug "decrypt return = " + temp2 }  
    
       def slurper = new groovy.json.JsonSlurper()
       def json = slurper.parseText(temp2)
       
       return json
}

def encrypt(def message,  def password) {
    //Deal with passed password value
    //First lets derive a shared SHA256 key from the password and make it a byte array - bKeyHash
    MessageDigest keydigest = MessageDigest.getInstance("SHA-256");
    byte[] bKeyHash = keydigest.digest(
       password.getBytes("UTF-8"));
    
    //cast it into a key object
    SecretKeySpec key = new SecretKeySpec(bKeyHash, "AES")
     
    _message = message
    //deal with passed message value
    //add end of data chars to message
    _message = _message + "\u0000\u0016"
    //pad message to 16 chars long
    _message = add_padding(_message)
    
    //hash original message and convert to bytes - bMessageHash
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] bMessageHash = digest.digest(message.getBytes("UTF-8"));
       
    // generate an Initial Vector using Random Number
    def IVKey = giveMeKey(16)
    IvParameterSpec iv = new IvParameterSpec(IVKey.getBytes("UTF-8"))
    
    // combine all that for encryption
    def cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE")
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    
    //encrypt updated message
    def bEncryptedMessage = cipher.doFinal(_message.getBytes("UTF-8"))
       
    // extract the Initial Vector and make it a string 
    def ivString = cipher.getIV()
    ivString = new String(ivString, "UTF-8")
       
    //return message to send as bytes array
    def byte[] bResult = [bMessageHash, IVKey.getBytes("UTF-8"),bEncryptedMessage].flatten()
    
    return bResult
}   

def decrypt (def baCypher, def strPassword) {
    //Read the iv
    byte[] baIV = baCypher[32..47]
 
    // trim header to encrypted message only.
    byte[] baDecoded = baCypher[48..baCypher.size()-1]
        
    //create cipher object 
    def cipher = Cipher.getInstance("AES/CBC/NoPadding", "SunJCE")
   
    //Derive a shared SHA256 key from the password and make it a byte array - bKeyHash
    MessageDigest keydigest = MessageDigest.getInstance("SHA-256");
    byte[] bKeyHash = keydigest.digest(
    strPassword.getBytes("UTF-8"));
   
    //cast it into a key object
    SecretKeySpec key = new SecretKeySpec(bKeyHash, "AES")
    
    //Put the IV into the right format
    IvParameterSpec iv = new IvParameterSpec(baIV)
   
    //decrypt
    cipher.init(Cipher.DECRYPT_MODE, key, iv )
   
    //convert to string and strip padding chars
    strDecoded = new String(cipher.doFinal(baDecoded), "UTF-8")
    strDecoded = strDecoded.replaceAll("\u0000","")
    strDecoded = strDecoded.replaceAll("\u0016","")
     
    return strDecoded
}   

def add_padding(data) {

   BLOCK_SIZE = 16
   INTERRUPT = "\u0000"
   PAD = "\u0016"
 
   _data = data
   _data_len = _data.length()
   remaining_len = BLOCK_SIZE - _data_len
   to_pad_len = BLOCK_SIZE - (_data_len % BLOCK_SIZE)
   pad_string = PAD * to_pad_len
 
   return _data + pad_string
}   


def giveMeKey(length){
    String alphabet = (('A'..'N')+('P'..'Z')+('a'..'k')+('m'..'z')+('2'..'9')).join() 
    key = new Random().with {
          (1..length).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
    }
    return key
}

private sendEventPublish(evt)	{
	def var = "${evt.name + 'Publish'}"
    if (isDebug) { log.debug var }
	def pub = this[var]
	if (pub)		sendEvent(name: evt.name, value: evt.value, descriptionText: evt.descriptionText, unit: evt.unit, displayed: evt.displayed);
    if (isDebug) { log.debug pub }
}
