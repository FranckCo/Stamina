import React from 'react'

import Logger from '../utils/logger';

import locale from '../stores/dictionary-store';

var logger = new Logger('GlobalMenu', 'Components');

class GlobalMenu extends React.Component {
  constructor(props) {
    super(props);
  }
  render() {
    logger.debug('Rendering GlobalMenu');
    return (
      <div>
        <p>Stamina</p>
        <ul>
          <li>Models</li>
          <li>Services</li>
          <li>Classifications</li>
          <li>Glossaries</li>
        </ul>
      </div>
    );
  }
}

export default GlobalMenu;