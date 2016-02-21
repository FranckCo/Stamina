import React from 'react';
import { Link } from 'react-router';

import ModelView from './model-view';
import Logger from '../utils/logger';
import locale from '../stores/dictionary-store';

var logger = new Logger('ModelsView', 'Components');

export default class ModelsView extends React.Component {
  constructor(props) {
    super(props);
  }
  render() {
  logger.debug('Rendering ModelsView');
    return (
      <div>
        <ModelView/>
        <ul>
          <li><Link to="/model/gsbpm">GSBPM</Link></li>
          <li><Link to="/model/gsim">GSIM</Link></li>
        </ul>
        {this.props.children}
      </div>
    )
  }
}
