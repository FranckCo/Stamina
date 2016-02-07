import React from 'react';
import { Link } from 'react-router';

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
      <div role="navigation">
        <ul className="nav nav-pills">
          <li role="presentation" className="active"><Link to="/">Stamina</Link></li>
          <li role="presentation"><Link to="/models">{locale.getEntry('models')}</Link></li>
          <li role="presentation"><Link to="/services">{locale.getEntry('services')}</Link></li>
          <li role="presentation"><Link to="/classifications">{locale.getEntry('classifications')}</Link></li>
          <li role="presentation"><Link to="/glossaries">{locale.getEntry('glossaries')}</Link></li>
        </ul>
      </div>
    );
  }
}

export default GlobalMenu;