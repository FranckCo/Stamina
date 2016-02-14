import React from 'react';

import Logger from '../utils/logger';
import locale from '../stores/dictionary-store';

var logger = new Logger('GSBPMView', 'Components');

export default class GSBPMView extends React.Component {
  constructor(props) {
    super(props);
  }
  render() {
  logger.debug('Rendering new GSBPMView');
    return (
      <div>
        <h1>{locale.getEntry('welcome_gsbpm')}</h1>
        {this.props.children}
      </div>
    )
  }
}
