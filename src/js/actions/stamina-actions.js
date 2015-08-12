import StaminaDispatcher from '../dispatchers/stamina-dispatcher';
import { ActionTypes } from '../constants/stamina-constants';
import Logger from '../utils/logger';

var logger = new Logger('StaminaActions', 'Actions');

// TODO maybe make several variables for different types of actions

export default StaminaActions = {
  setLanguage: function (language) {
    logger.info('Set language to: ', language);
    StaminaDispatcher.handleViewAction({
      actionType: ActionTypes.LANGUAGE_CHANGED,
      language: language
    });
  }
};
