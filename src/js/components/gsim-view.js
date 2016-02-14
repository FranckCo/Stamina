import React from 'react';

import Logger from '../utils/logger';
import locale from '../stores/dictionary-store';

var logger = new Logger('GSIMView', 'Components');

export default class GSIMView extends React.Component {
  constructor(props) {
    super(props);
  }
  render() {
  logger.debug('Rendering GSIMView');
    return (
      <div>
        <h1>{locale.getEntry('welcome_gsim')}</h1>
        {this.props.children}
      </div>
    )
  }
}
