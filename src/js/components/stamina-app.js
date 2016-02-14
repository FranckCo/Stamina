import React from 'react';
import { render } from 'react-dom';
import { Router, Route, Link, browserHistory } from 'react-router';

import StaminaActions from '../actions/stamina-actions';
import Logger from '../utils/logger';
import GlobalMenu from './global-menu';
import GSBPMView from './gsbpm-view';
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
          <li><Link to="/gsbpm">GSBPM</Link></li>
          <li><Link to="/gsim">GSIM</Link></li>
        </ul>
        {this.props.children}
      </div>
    )
  }
}

class GSIMView extends React.Component {
  render() {
    logger.debug('Rendering GSIMView');
    return (
      <div>
        <h1>This is the GSIM home page</h1>
        {this.props.children}
      </div>
    )
  }
}

var routes = (
  <Route path='/' component={StaminaApp}>
    <Route name='gsbpm' path='gsbpm' component={GSBPMView}/>
    <Route name='gsim' path='gsim' component={GSIMView}/>
  </Route>
);

render(<Router routes={routes} history={browserHistory}/>, document.getElementById('base'))
