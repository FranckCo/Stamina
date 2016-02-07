import StaminaDispatcher from '../dispatchers/stamina-dispatcher';
import { ActionTypes, StoreEvents } from '../constants/stamina-constants';
import EventEmitter from 'events';
import Logger from '../utils/logger';

var logger = new Logger('DictionaryStore', 'Stores');

var _language = 'en';
var _localDictionary;

var _dictionary = {
  welcome: {'en': 'Welcome to Stamina', 'fr': 'Bienvenue dans Stamina'},
  models: {'en': 'Models', 'fr': 'Modèles'},
  services: {'en': 'Services', 'fr': 'Services'},
  classifications: {'en': 'Classifications', 'fr': 'Nomenclatures'},
  glossaries: {'en': 'Glossaries', 'fr': 'Glossaires'}
};

//initialization
setDictionary(_language);

function setDictionary(language) {
  var locale = {};
  for (var key in _dictionary) {
    locale[key] = _dictionary[key][language]
  }
  _localDictionary = locale;
}

function setLanguage(language) {
  _language = language;
  setDictionary(language);
}

var DictionaryStore = Object.assign({}, EventEmitter.prototype, {

  emitChange: function() {
    logger.debug('Store emitting change event');
    this.emit(StoreEvents.CHANGE_EVENT);
  },

  getDictionary: function () {
    return _localDictionary;
  },

  getEntry: function (key) {
    return _localDictionary[key];
  },

  setLanguage: setLanguage,

  dispatcherIndex: StaminaDispatcher.register(function(payload) {
    logger.debug('Received dispatched payload: ', payload);
    switch(payload.action.actionType) {
      case ActionTypes.LANGUAGE_CHANGED:
        setLanguage(payload.action.language);
        break;
      default:
        return true;
    }
    logger.debug('Store will emit change, language is: ', _language);
    DictionaryStore.emitChange();
    return true;
  })
});

export default DictionaryStore;