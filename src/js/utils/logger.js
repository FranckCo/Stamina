import StaminaConfig from '../config/config';

const DEFAULT_LEVEL = 'DEBUG';

const LEVELS = {
  'DEBUG' : 0,
  'INFO' : 1,
  'WARN' : 2,
  'ERROR' : 3
};

class Logger {
  constructor(moduleName, namespace='default') {
    this.moduleName = moduleName;
    this.namespace = namespace;
  }

  getPrefix() {
    return '[' + this.namespace + '][' + this.moduleName +']';
  }

  getCurrentLevel() {
    return StaminaConfig.log.level;
  }

  logWrapper(testLevel, messageArray) {
    messageArray.unshift(this.getPrefix());
    if(LEVELS[testLevel] >= LEVELS[this.getCurrentLevel()]
      && StaminaConfig.log.activeNamespaces.indexOf(this.namespace) >= 0) {
      switch(testLevel) {
        case 'DEBUG':
          console.log.apply(console, messageArray);
          break;
        case 'INFO':
          console.info.apply(console, messageArray);
          break;
        case 'WARN':
          console.warn.apply(console, messageArray);
          break;
        case 'ERROR':
          console.error.apply(console, messageArray);
          break
        default:
          //no-op
      }

    }
  }

  debug(message) {
    var messageArray = Array.prototype.slice.call(arguments);
    this.logWrapper('DEBUG', messageArray);
  }

  info(message) {
    var messageArray = Array.prototype.slice.call(arguments);
    this.logWrapper('INFO', messageArray);
  }

  warn(message) {
    var messageArray = Array.prototype.slice.call(arguments);
    this.logWrapper('WARN', messageArray);
  }

  error(message) {
    var messageArray = Array.prototype.slice.call(arguments);
    this.logWrapper('ERROR', messageArray);
  }
};

export default Logger;