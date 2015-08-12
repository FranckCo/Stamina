import { PayloadSources } from '../constants/stamina-constants';
import { Dispatcher } from 'flux';

// TODO Add logging

class StaminaDispatcher extends Dispatcher {

  handleServerAction(action) {
    var payload = {
      source: PayloadSources.SERVER_SOURCE,
      action: action
    };
    logger.info('StaminaDispatcher dispatching SERVER_SOURCE payload', payload);
    this.dispatch(payload);
  }

  handleViewAction(action) {
    var payload = {
      source: PayloadSources.VIEW_SOURCE,
      action: action
    };
    logger.info('StaminaDispatcher dispatching VIEW_SOURCE payload', payload);
    this.dispatch(payload);
  }
}

let _staminaDispatcher = new StaminaDispatcher();

export default _staminaDispatcher;