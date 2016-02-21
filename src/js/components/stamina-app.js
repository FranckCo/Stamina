import React from 'react';
import { render } from 'react-dom';
import { Router, Route, Link, browserHistory } from 'react-router';

import StaminaActions from '../actions/stamina-actions';
import Logger from '../utils/logger';
import GlobalMenu from './global-menu';
import GSBPMView from './gsbpm-view';
import GSIMView from './gsim-view';
import ModelView from './model-view';
import ModelsView from './models-view';
import locale from '../stores/dictionary-store';

// Set language has to be called before requiring components
var language = (navigator.language || navigator.browserLanguage).split('-')[0];
if (language !== 'fr') language = 'en';

StaminaActions.setLanguage(language);

var logger = new Logger('StaminaApp', 'Components');

class StaminaApp extends React.Component {
  render() {
    logger.debug('Rendering StaminaApp');
    return (
      <div>
        <GlobalMenu/>
        <h2>{locale.getEntry('welcome')}</h2>
        <ul>
          <li><Link to="/models">{locale.getEntry('models')}</Link></li>
        </ul>
        {this.props.children}
      </div>
    )
  }
}

var routes = (
  <Route path='/' component={StaminaApp}>
    <Route path='model' component={ModelView}>
      <Route path='gsbpm' component={GSBPMView}/>
      <Route path='gsim' component={GSIMView}/>
    </Route>
    <Route path='models' component={ModelsView}/>
  </Route>
);

render(<Router routes={routes} history={browserHistory}/>, document.getElementById('base'))
