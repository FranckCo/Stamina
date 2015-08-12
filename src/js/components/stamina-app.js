import React from 'react';
import Router, { Route, RouteHandler, DefaultRoute, Link } from 'react-router';
import StaminaActions from '../actions/stamina-actions';
import Logger from '../utils/logger';

import locale from '../stores/dictionary-store';

// Set language has to be called before requiring components
var language = (navigator.language || navigator.browserLanguage).split('-')[0];
if (language !== 'fr') language = 'en';

StaminaActions.setLanguage(language);

var logger = new Logger('StaminaApp', 'Components');

class GSBPMView extends React.Component {
  render() {
    logger.debug('Rendering GSBPMView');
    return (
      <div>
        <h1 text="This is the GSPBM home page"/>
      </div>
    )
  }
}

class GSIMView extends React.Component {
  render() {
    logger.debug('Rendering GSIMView');
    return (
      <div>
        <h1 text="This is the GSIM home page"/>
      </div>
    )
  }
}

class StaminaHome extends React.Component {
  render() {
    return (
      <div>
        <h1 text={locale.welcome}/>
        <p text="Select one option below"/>
        <ul>
          <Link to='gsbpm' text='GSBPM'/>
          <Link to='gsim' text='GSIM'/>
        </ul>
      </div>
    )
  }
}

class StaminaApp extends React.Component {
  render() {
    logger.debug('Rendering StaminaApp');
    return (<RouteHandler/>)
  }
}

// Handler variables should be defined before
// transitioTo uses the route names, so this attribute must be present
var routes = (
  <Route handler={StaminaApp}>
    <DefaultRoute handler={StaminaHome}/>
    <Route name='gsbpm' path='gsbpm' handler={GSBPMView}/>
    <Route name='gsim' path='gsim' handler={GSIMView}/>
  </Route>
);

var router = Router.create({
  routes: routes,
  location: Router.HashLocation
});

router.run( (Handler) => {
  React.render(<Handler/>, document.getElementById('base'));
});
