import React from 'react'
import { render } from 'react-dom'
import { Router, Route, Link } from 'react-router'
import StaminaActions from '../actions/stamina-actions';
import Logger from '../utils/logger';
import GlobalMenu from './global-menu';

import locale from '../stores/dictionary-store';

// Set language has to be called before requiring components
var language = (navigator.language || navigator.browserLanguage).split('-')[0];
if (language !== 'fr') language = 'en';

StaminaActions.setLanguage(language);

var logger = new Logger('StaminaApp', 'Components');

const StaminaApp = React.createClass({
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
})

const GSBPMView = React.createClass({
  render() {
    logger.debug('Rendering GSBPMView');
    return (
      <div>
        <h1>This is the GSBPM home page</h1>
        {this.props.children}
      </div>
    )
  }
})

const GSIMView = React.createClass({
  render() {
    logger.debug('Rendering GSIMView');
    return (
      <div>
        <h1>This is the GSIM home page</h1>
        {this.props.children}
      </div>
    )
  }
})

render((
  <Router>
    <Route path="/" component={StaminaApp}>
      <Route path="gsbpm" component={GSBPMView}/>
      <Route path="gsim" component={GSIMView}/>
    </Route>
  </Router>
), document.getElementById('base'))

