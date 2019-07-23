import { NativeModules } from "react-native";

const { RNSslPinning } = NativeModules;

export const fetch = async (url, obj) => {
  return await RNSslPinning.fetch(url, obj);
};

export const SSLPinning = (url, options) => {
  RNSslPinning.SSLPinning(url, options);
};

export const getCookies = domain => {
  if (domain) {
    return RNSslPinning.getCookies(domain);
  }

  return null;
};

export const removeCookieByName = name => {
  if (name) {
    return RNSslPinning.removeCookieByName(name);
  }

  return false;
};
