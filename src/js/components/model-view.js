import React from 'react';

import Logger from '../utils/logger';

var logger = new Logger('ModelView', 'Components');

export default class ModelView extends React.Component {
  constructor(props) {
    super(props);
  }
  render() {
  logger.debug('Rendering ModelView');
    return (
      <hr/>
    )
  }
}
