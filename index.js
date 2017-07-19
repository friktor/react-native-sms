import {
  DeviceEventEmitter,
  NativeModules
} from "react-native"

const SmsModule = NativeModules.SmsAndroid
export default SmsModule

const DELIVER_EVENT = "com.friktor.reactnativesms:sms_deliver_result"
const SENT_EVENT = "com.friktor.reactnativesms:sms_sent_result"

export const addSentListener = (listener) => {
  return DeviceEventEmitter.addListener(
    SENT_EVENT,
    listener
  )
}

export const addDeliverListener = (listener) => {
  return DeviceEventEmitter.addListener(
    DELIVER_EVENT,
    listener
  )
}